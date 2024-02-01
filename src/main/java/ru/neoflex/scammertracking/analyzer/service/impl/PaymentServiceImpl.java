package ru.neoflex.scammertracking.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.domain.model.Coordinates;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.service.PaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.service.PaymentService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentCacheDao paymentCacheDao;
    private final FeignService feignService;
    private PaymentAnalyzerImpl paymentAnalyzer;
    private final ModelMapper modelMapper;

    @Autowired
    public void setPaymentAnalyzer(@Lazy PaymentAnalyzerImpl paymentAnalyzer) {
        this.paymentAnalyzer = paymentAnalyzer;
    }

    public LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest, AtomicBoolean isCachedDateDeprecated) throws Exception {
        log.info("received cacheDeprecated={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                isCachedDateDeprecated, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        LastPaymentResponseDto lastPaymentResponse = null;

        try {
            PaymentEntity paymentCacheEntity = paymentCacheDao.findPaymentByCardNumber(paymentRequest.getPayerCardNumber());
            if (null != paymentCacheEntity) {
                boolean isDeprecated = LocalDateTime.now().minusDays(1).isAfter(paymentCacheEntity.getDateUpdating());
                isCachedDateDeprecated.set(isDeprecated);
                if (isCachedDateDeprecated.get()) {
                    lastPaymentResponse = feignService.getLastPayment(paymentRequest);
                    log.info("Response. cache is deprecated. Feign service return last payment response={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                            lastPaymentResponse.getId(), lastPaymentResponse.getPayerCardNumber(), lastPaymentResponse.getReceiverCardNumber(), lastPaymentResponse.getCoordinates().getLatitude(), lastPaymentResponse.getCoordinates().getLongitude(), lastPaymentResponse.getDate());
                } else {
                    lastPaymentResponse = LastPaymentResponseDto.builder()
                            .id(paymentCacheEntity.getIdPayment())
                            .payerCardNumber(paymentCacheEntity.getPayerCardNumber())
                            .receiverCardNumber(paymentCacheEntity.getReceiverCardNumber())
                            .coordinates(new Coordinates(paymentCacheEntity.getLatitude(), paymentCacheEntity.getLongitude()))
                            .date(paymentCacheEntity.getDatePayment())
                            .build();
                    log.info("Response cache. Last payment response={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                            lastPaymentResponse.getId(), lastPaymentResponse.getPayerCardNumber(), lastPaymentResponse.getReceiverCardNumber(), lastPaymentResponse.getCoordinates().getLatitude(), lastPaymentResponse.getCoordinates().getLongitude(), lastPaymentResponse.getDate());
                }
            } else {
                lastPaymentResponse = feignService.getLastPayment(paymentRequest);
//                PaymentEntity paymentEntitySave = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
//                        paymentRequest.getId(),
//                        paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
//                        paymentRequest.getDate(), LocalDateTime.now());
                isCachedDateDeprecated.set(true);
                log.info("Response. Cache does not exist. Feign service return last payment response={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                        lastPaymentResponse.getId(), lastPaymentResponse.getPayerCardNumber(), lastPaymentResponse.getReceiverCardNumber(), lastPaymentResponse.getCoordinates().getLatitude(), lastPaymentResponse.getCoordinates().getLongitude(), lastPaymentResponse.getDate());
                //paymentCacheDao.save(paymentEntitySave);
            }
        } catch (NotFoundException e) {
//            log.error("The payment with cardNumber={} not found", paymentRequest.getPayerCardNumber());
//            throw new NotFoundException(e.getMessage());
            PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
            paymentResult.setTrusted(false);
            paymentAnalyzer.routePayment(true, new AtomicBoolean(true), paymentRequest, paymentResult);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        return lastPaymentResponse;
    }
}
