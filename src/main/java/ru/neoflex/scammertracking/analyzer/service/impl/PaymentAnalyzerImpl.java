package ru.neoflex.scammertracking.analyzer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.error.exception.BadRequestException;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.geo.SimplePaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.kafka.producer.PaymentProducer;
import ru.neoflex.scammertracking.analyzer.service.PaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.service.PaymentService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class PaymentAnalyzerImpl implements PaymentAnalyzer {

    @Autowired
    public PaymentAnalyzerImpl(FeignService feignService, PaymentService paymentService, PaymentCacheDao paymentCacheDao, PaymentProducer paymentProducer, ModelMapper modelMapper) {
        this.feignService = feignService;
        this.paymentService = paymentService;
        this.paymentCacheDao = paymentCacheDao;
        this.paymentProducer = paymentProducer;
        this.modelMapper = modelMapper;
    }

    private FeignService feignService;
    private PaymentService paymentService;
    private PaymentCacheDao paymentCacheDao;
    private final PaymentProducer paymentProducer;
    private ModelMapper modelMapper;

    @Value("${kafka.topic.suspicious-payments}")
    private String suspiciousPaymentsTopic;
    @Value("${kafka.topic.checked-payments}")
    private String checkedPaymentsTopic;

    @Override
    public void analyzeConsumeMessage(String key, PaymentRequestDto paymentRequest) throws Exception {
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
            lastPayment = paymentService.getLastPayment(paymentRequest, isCacheDeprecated);
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

        routePayment(isTrusted, isCacheDeprecated, paymentRequest, paymentResult);
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

    private void routePayment(boolean isTrusted, AtomicBoolean isCacheDeprecated, PaymentRequestDto paymentRequest, PaymentResponseDto paymentResult) throws Exception {
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
                    PaymentEntity paymentEntity = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
                            paymentRequest.getId(),
                            paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
                            paymentRequest.getDate(), LocalDateTime.now());
                    paymentCacheDao.update(paymentEntity);
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
}
