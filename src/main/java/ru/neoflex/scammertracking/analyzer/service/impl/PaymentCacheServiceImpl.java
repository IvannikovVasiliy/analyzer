package ru.neoflex.scammertracking.analyzer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.domain.model.PaymentCacheResponseDto;
import ru.neoflex.scammertracking.analyzer.service.PaymentCacheService;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentCacheServiceImpl implements PaymentCacheService {

    public PaymentCacheServiceImpl(PaymentCacheDao paymentCacheDao, ModelMapper modelMapper) {
        this.paymentCacheDao = paymentCacheDao;
        this.modelMapper = modelMapper;
    }

    private PaymentCacheDao paymentCacheDao;
    private ModelMapper modelMapper;

    @Override
    public PaymentCacheResponseDto findPaymentByPayerCardNumber(String cardNumber) {
        log.info("received cardNumber: {}", cardNumber);

        PaymentEntity payment = paymentCacheDao.findPaymentByCardNumber(cardNumber);
        if (payment == null) {
            return null;
        }

        PaymentCacheResponseDto paymentResponse = modelMapper.map(payment, PaymentCacheResponseDto.class);
        paymentResponse.setId(payment.getIdPayment());

        log.info("result payment={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                payment.getIdPayment(), payment.getPayerCardNumber(), payment.getReceiverCardNumber(), payment.getLatitude(), payment.getLongitude(), payment.getDatePayment());

        return paymentResponse;
    }

    @Override
    public void save(PaymentRequestDto paymentRequest) {
        log.info("received for save. paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        PaymentEntity paymentEntity = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
                        paymentRequest.getId(),
                        paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
                        paymentRequest.getDate(), LocalDateTime.now());
        paymentCacheDao.save(paymentEntity);

        log.info("Payment entity was saved in Redis");
    }

    @Override
    public void update(PaymentRequestDto paymentRequest) {
        log.info("received for update. paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        PaymentEntity paymentEntity = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
                paymentRequest.getId(),
                paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
                paymentRequest.getDate(), LocalDateTime.now());
        paymentCacheDao.update(paymentEntity);

        log.info("Payment entity was updated in Redis");
    }


}
