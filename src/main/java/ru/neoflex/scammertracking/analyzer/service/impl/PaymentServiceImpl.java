package ru.neoflex.scammertracking.analyzer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.domain.model.Coordinates;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.service.PaymentService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    public PaymentServiceImpl(PaymentCacheDao paymentCacheDao, FeignService feignService) {
        this.paymentCacheDao = paymentCacheDao;
        this.feignService = feignService;
    }

    private PaymentCacheDao paymentCacheDao;
    private FeignService feignService;

    public LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest, AtomicBoolean isCachedDateDeprecated) throws RuntimeException {
        log.info("received cacheDeprecated={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                isCachedDateDeprecated, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        LastPaymentResponseDto lastPaymentResponse = null;

        try {
            PaymentEntity paymentCacheEntity = paymentCacheDao.findPaymentByCardNumber(paymentRequest.getPayerCardNumber());
            if (null != paymentCacheEntity) {
                boolean isDeprecated = LocalDateTime.now().isAfter(paymentCacheEntity.getDateUpdating());
                isCachedDateDeprecated.set(isDeprecated);
                if (isCachedDateDeprecated.get()) {
                    lastPaymentResponse = feignService.getLastPayment(paymentRequest);
                } else {
                    lastPaymentResponse = LastPaymentResponseDto.builder()
                            .id(paymentCacheEntity.getIdPayment())
                            .payerCardNumber(paymentCacheEntity.getPayerCardNumber())
                            .receiverCardNumber(paymentCacheEntity.getReceiverCardNumber())
                            .coordinates(new Coordinates(paymentCacheEntity.getLatitude(), paymentCacheEntity.getLongitude()))
                            .date(paymentCacheEntity.getDatePayment())
                            .build();
                }
            } else {
                lastPaymentResponse = feignService.getLastPayment(paymentRequest);
                PaymentEntity paymentEntitySave = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
                        paymentRequest.getId(),
                        paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
                        paymentRequest.getDate(), LocalDateTime.now());
                paymentCacheDao.save(paymentEntitySave);
            }
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

        return lastPaymentResponse;
    }
}
