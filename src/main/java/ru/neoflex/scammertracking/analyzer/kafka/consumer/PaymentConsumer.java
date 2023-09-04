package ru.neoflex.scammertracking.analyzer.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.service.PaymentAnalyzer;

@Service
@Import(FeignClientsConfiguration.class)
@Slf4j
public class PaymentConsumer {

    @Autowired
    public PaymentConsumer(PaymentAnalyzer paymentAnalyzer) {
        this.paymentAnalyzer = paymentAnalyzer;
    }

    private PaymentAnalyzer paymentAnalyzer;

    @KafkaListener(topics = "${kafka.topic.payments}", containerFactory = "paymentsKafkaListenerContainerFactory")
    public void consumePayment(@Payload PaymentRequestDto paymentRequest,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) throws Exception {
        log.info("received key={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                key, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        paymentAnalyzer.analyzeConsumeMessage(key, paymentRequest);
    }
}
