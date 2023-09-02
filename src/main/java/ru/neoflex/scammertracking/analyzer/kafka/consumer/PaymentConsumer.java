package ru.neoflex.scammertracking.analyzer.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.geo.SimplePaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.kafka.producer.PaymentProducer;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;

@Service
@Import(FeignClientsConfiguration.class)
@Slf4j
public class PaymentConsumer {

//    private final Logger LOGGER = LoggerFactory.getLogger(PaymentConsumer.class);

    @Autowired
    public PaymentConsumer(FeignService feignService, PaymentProducer paymentProducer, ModelMapper modelMapper) {
        this.feignService = feignService;
        this.paymentProducer = paymentProducer;
        this.modelMapper = modelMapper;
    }

    private FeignService feignService;
    private final PaymentProducer paymentProducer;
    private final ModelMapper modelMapper;

    @Value("${kafka.topic.suspicious-payments}")
    private String suspiciousPaymentsTopic;
    @Value("${kafka.topic.checked-payments}")
    private String checkedPaymentsTopic;

    @KafkaListener(topics = "${kafka.topic.payments}", containerFactory = "paymentsKafkaListenerContainerFactory")
    public void consumePayment(@Payload PaymentRequestDto paymentRequest,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) throws Exception {
        log.info("received key={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                key, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        LastPaymentResponseDto lastPayment = null;
        try {
            lastPayment = feignService.getLastPayment(paymentRequest);
        } catch (NotFoundException e) {
            PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);

            log.warn(e.getMessage());
            return;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new Exception(e.getMessage());
        }

        PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
        boolean isTrusted = SimplePaymentAnalyzer.checkPayment(lastPayment, paymentRequest);
        paymentResult.setTrusted(isTrusted);

        if (isTrusted) {
            //feignService.savePayment(paymentRequest);
            paymentProducer.sendMessage(checkedPaymentsTopic, paymentResult);

            log.info("response. Sent message in topic={}", checkedPaymentsTopic);
        } else {
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.info("response. Sent message in topic={}, because latitude={}, longitude={}",
                    suspiciousPaymentsTopic, paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude());
        }
    }
}
