package ru.neoflex.scammertracking.analyzer.kafka.consumer;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.geo.GeoAnalyzer;
import ru.neoflex.scammertracking.analyzer.kafka.producer.PaymentProducer;

@Service
public class PaymentConsumer {

    private final Logger LOGGER = LoggerFactory.getLogger(PaymentConsumer.class);

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
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        LOGGER.info("received key={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                key, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        LastPaymentResponseDto lastPayment = feignService.getLastPayment(paymentRequest);

        PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
        boolean isTrusted = GeoAnalyzer.checkPayment(lastPayment, paymentRequest);

        if (isTrusted) {
            feignService.savePayment(paymentRequest);
            paymentProducer.sendMessage(checkedPaymentsTopic, paymentResult);

            LOGGER.info("response. Sent message in topic={}", checkedPaymentsTopic);
        } else {
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            LOGGER.info("response. Sent message in topic={}, because latitude={}, longitude={}",
                    suspiciousPaymentsTopic, paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude());
        }
    }
}
