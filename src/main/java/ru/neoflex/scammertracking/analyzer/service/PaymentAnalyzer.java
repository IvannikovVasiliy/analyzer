package ru.neoflex.scammertracking.analyzer.service;

import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;

public interface PaymentAnalyzer {
    void analyzeConsumeMessage(String key, PaymentRequestDto paymentRequest) throws Exception;
}
