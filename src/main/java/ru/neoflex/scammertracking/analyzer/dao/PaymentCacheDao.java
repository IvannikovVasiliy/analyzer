package ru.neoflex.scammertracking.analyzer.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PaymentCacheDao {

    private static final String HASH_KEY = "Payment";

    private final RedisTemplate redisTemplate;

    public PaymentEntity findPaymentByCardNumber(String payerCardNumber) {
        log.info("find payment by card number={} was saved", payerCardNumber);
        return (PaymentEntity) redisTemplate.opsForHash().get(HASH_KEY, payerCardNumber);
    }

    public PaymentEntity save(PaymentEntity payment) {
        log.info("receive for save. payment={}", payment);

        redisTemplate.opsForHash().put(HASH_KEY, payment.getPayerCardNumber(), payment);

        log.info("The payment with idPayment={} was saved", payment.getIdPayment());
        return payment;
    }

    public PaymentEntity update(PaymentEntity paymentUpdate) {
        log.info("Received for check payment. payment={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, datePayment ={}, dateUpdating={} }",
                paymentUpdate.getIdPayment(), paymentUpdate.getPayerCardNumber(), paymentUpdate.getReceiverCardNumber(), paymentUpdate.getLatitude(), paymentUpdate.getLongitude(), paymentUpdate.getDatePayment(), paymentUpdate.getDateUpdating());

        PaymentEntity paymentEntity = (PaymentEntity) redisTemplate.opsForHash().get(HASH_KEY, paymentUpdate.getPayerCardNumber());
        if (paymentEntity == null) {
            paymentEntity = new PaymentEntity();
            paymentEntity.setPayerCardNumber(paymentUpdate.getPayerCardNumber());
        }
        paymentEntity.setIdPayment(paymentEntity.getIdPayment());
        paymentEntity.setReceiverCardNumber(paymentUpdate.getReceiverCardNumber());
        paymentEntity.setLatitude(paymentUpdate.getLatitude());
        paymentEntity.setLongitude(paymentUpdate.getLongitude());
        paymentEntity.setDatePayment(paymentUpdate.getDatePayment());
        paymentEntity.setDateUpdating(paymentUpdate.getDateUpdating());

        redisTemplate.opsForHash().put(HASH_KEY, paymentEntity.getPayerCardNumber(), paymentEntity);

        log.info("The payment with idPayment={} was updateing", paymentUpdate.getIdPayment());
        return paymentEntity;
    }
}
