package ru.neoflex.scammertracking.analyzer.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentProducer {

    private final Logger LOGGER = LoggerFactory.getLogger(PaymentProducer.class);

    @Autowired
    public PaymentProducer(KafkaTemplate<String, PaymentResponseDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private KafkaTemplate<String, PaymentResponseDto> kafkaTemplate;

    public void sendMessage(final String TOPIC, PaymentResponseDto payment) {
        CompletableFuture<SendResult<String, PaymentResponseDto>> future = kafkaTemplate.send(TOPIC, String.valueOf(payment.getId()), payment);

        future.whenCompleteAsync((result, exception) -> {
            if (null != exception) {
                LOGGER.error("error. Unable to send message={} due to : {}", payment, exception.getMessage());
            } else {
                LOGGER.info("Sent message={} with offset=={}", payment, result.getRecordMetadata().offset());
            }
        });
    }
}
