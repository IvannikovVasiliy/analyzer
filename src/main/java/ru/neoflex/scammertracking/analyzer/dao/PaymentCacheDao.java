package ru.neoflex.scammertracking.analyzer.dao;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;

@Repository
public class PaymentCacheDao {

    private static final String HASH_KEY = "Payment";

    public PaymentCacheDao(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private final RedisTemplate redisTemplate;

    public PaymentEntity findPaymentByCardNumber(String payerCardNumber) {
        return (PaymentEntity) redisTemplate.opsForHash().get(HASH_KEY, payerCardNumber);
    }

    public PaymentEntity save(PaymentEntity payment) {
        redisTemplate.opsForHash().put(HASH_KEY, payment.getPayerCardNumber(), payment);
        return payment;
    }

    public PaymentEntity update(PaymentEntity paymentUpdate) {
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

        return paymentEntity;
    }
}
