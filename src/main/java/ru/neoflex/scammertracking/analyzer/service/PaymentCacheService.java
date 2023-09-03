package ru.neoflex.scammertracking.analyzer.service;

import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.model.PaymentCacheResponseDto;

public interface PaymentCacheService {
    PaymentCacheResponseDto findPaymentByPayerCardNumber(String cardNumber);
    void save(PaymentRequestDto paymentRequest);
    void update(PaymentRequestDto paymentRequest);
}
