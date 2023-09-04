package ru.neoflex.scammertracking.analyzer.service;

import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;

import java.util.concurrent.atomic.AtomicBoolean;

public interface PaymentService {
    LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest, AtomicBoolean isCachedDateDeprecated) throws RuntimeException, Exception;
}
