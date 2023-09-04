package ru.neoflex.scammertracking.analyzer.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.model.Coordinates;
import ru.neoflex.scammertracking.analyzer.domain.model.PaymentCacheResponseDto;
import ru.neoflex.scammertracking.analyzer.error.exception.BadRequestException;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.geo.SimplePaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.kafka.producer.PaymentProducer;
import ru.neoflex.scammertracking.analyzer.service.PaymentCacheService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Import(FeignClientsConfiguration.class)
@Slf4j
public class PaymentConsumer {

    @Autowired
    public PaymentConsumer(FeignService feignService, PaymentCacheService paymentCacheService, PaymentProducer paymentProducer, ModelMapper modelMapper, PaymentCacheDao paymentCacheDao) {
        this.feignService = feignService;
        this.paymentCacheService = paymentCacheService;
        this.paymentCacheDao = paymentCacheDao;
        this.paymentProducer = paymentProducer;
        this.modelMapper = modelMapper;
    }

    private FeignService feignService;
    private PaymentCacheService paymentCacheService;
    private PaymentCacheDao paymentCacheDao;
    private final PaymentProducer paymentProducer;
    private ModelMapper modelMapper;

    @Value("${kafka.topic.suspicious-payments}")
    private String suspiciousPaymentsTopic;
    @Value("${kafka.topic.checked-payments}")
    private String checkedPaymentsTopic;

    @KafkaListener(topics = "${kafka.topic.payments}", containerFactory = "paymentsKafkaListenerContainerFactory")
    public void consumePayment(@Payload PaymentRequestDto paymentRequest,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) throws Exception {
        log.info("received key={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                key, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
        AtomicBoolean isCacheDeprecated = new AtomicBoolean();

        boolean isTrusted;
        if (checkSuspicious(paymentRequest)) {
            log.info("response. Sent message in topic={}", suspiciousPaymentsTopic);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            return;
        }

        LastPaymentResponseDto lastPayment = null;
        try {
            lastPayment = getLastPayment(paymentRequest, isCacheDeprecated);
        } catch (BadRequestException e) {
            paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.warn("sent message with key={} in topic {}", key, suspiciousPaymentsTopic);
            return;
        } catch (NotFoundException e) {
            paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.warn("sent message with key={} in topic {}", key, suspiciousPaymentsTopic);
            return;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new Exception(e.getMessage());
        }

        isTrusted = SimplePaymentAnalyzer.checkPayment(lastPayment, paymentRequest);
        paymentResult.setTrusted(isTrusted);

        if (isTrusted) {
            try {
                feignService.savePayment(paymentRequest);
            } catch (BadRequestException e) {
                paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
                isTrusted = false;
                log.info("Response. Sent message in topic={}, because the payment with id={} already exists",
                        suspiciousPaymentsTopic, paymentRequest.getId());
            } catch (Exception e) {
                log.info("Internal error");
                throw new Exception(e.getMessage());
            }

            if (isTrusted) {
                if (isCacheDeprecated.get()) {
                    paymentCacheService.update(paymentRequest);
                }
                paymentProducer.sendMessage(checkedPaymentsTopic, paymentResult);
                log.info("response. Sent message in topic={}", checkedPaymentsTopic);
            }
        } else {
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.info("Response. Sent message in topic={}, because latitude={}, longitude={}",
                    suspiciousPaymentsTopic, paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude());
        }
    }

    private LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest, AtomicBoolean isCachedDateDeprecated) throws RuntimeException, Exception {
        LastPaymentResponseDto lastPaymentResponse = null;

        PaymentCacheResponseDto paymentCache = paymentCacheService.findPaymentByPayerCardNumber(paymentRequest.getPayerCardNumber());
        if (null != paymentCache) {
            boolean isDeprecated = LocalDateTime.now().isAfter(paymentCache.getDateUpdating().plusDays(1));
            isCachedDateDeprecated.set(isDeprecated);
            if (isCachedDateDeprecated.get()) {
                lastPaymentResponse = feignService.getLastPayment(paymentRequest);
            } else {
                lastPaymentResponse = LastPaymentResponseDto.builder()
                        .id(paymentCache.getId())
                        .payerCardNumber(paymentCache.getPayerCardNumber())
                        .receiverCardNumber(paymentCache.getReceiverCardNumber())
                        .coordinates(new Coordinates(paymentCache.getLatitude(), paymentCache.getLongitude()))
                        .date(paymentCache.getDatePayment())
                        .build();
            }
        } else {
            lastPaymentResponse = feignService.getLastPayment(paymentRequest);
            paymentCacheService.save(paymentRequest);
        }

        return lastPaymentResponse;
    }

    private boolean checkSuspicious(PaymentRequestDto paymentRequest) {
        log.info("Check suspicious. paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        if (paymentRequest.getPayerCardNumber().length() < 6) {
            log.info("Result validating. The message is suspicious, because the length of payerCardNumber is too short");
            return true;
        }
        if (paymentRequest.getReceiverCardNumber().length() < 6) {
            log.info("Result validating. The message is suspicious, because the length of receiverCardNumber is too short");
            return true;
        }
        if (LocalDateTime.now().isBefore(paymentRequest.getDate())) {
            log.info("Result validating. The message is suspicious, because date of paymentRequest more than current datetime");
            return true;
        }

        log.info("Result validating. The message is valid");
        return false;
    }
}
