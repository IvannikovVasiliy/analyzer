package ru.neoflex.scammertracking.analyzer.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PaymentProducer {

    @Autowired
    public PaymentProducer(KafkaTemplate<String, PaymentResponseDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private KafkaTemplate<String, PaymentResponseDto> kafkaTemplate;

    public void sendMessage(final String TOPIC, PaymentResponseDto payment) {
        CompletableFuture<SendResult<String, PaymentResponseDto>> future = kafkaTemplate.send(TOPIC, String.valueOf(payment.getId()), payment);

        future.whenCompleteAsync((result, exception) -> {
            if (null != exception) {
                log.error("error. Unable to send message with key={} message={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} } due to : {}",
                        payment.getId(), payment.getId(), payment.getPayerCardNumber(), payment.getReceiverCardNumber(), payment.getCoordinates().getLatitude(), payment.getCoordinates().getLongitude(), payment.getDate(), exception.getMessage());
            } else {
                log.info("Sent message={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} } with offset=={}",
                        payment.getId(), payment.getPayerCardNumber(), payment.getReceiverCardNumber(), payment.getCoordinates().getLatitude(), payment.getCoordinates().getLongitude(), payment.getDate(), result.getRecordMetadata().offset());
            }
        });
    }
}
