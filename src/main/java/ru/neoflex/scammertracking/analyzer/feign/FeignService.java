package ru.neoflex.scammertracking.analyzer.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;

@Service
public class FeignService {

    @Autowired
    public FeignService(PaymentFeignClient paymentFeignClient) {
        this.paymentFeignClient = paymentFeignClient;
    }

    private PaymentFeignClient paymentFeignClient;

    public LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest) throws NotFoundException, Exception {
        LastPaymentRequestDto lastPaymentRequestDto = new LastPaymentRequestDto(paymentRequest.getPayerCardNumber());
        LastPaymentResponseDto lastPaymentResponseEntity = paymentFeignClient.getLastPaymentByReceiverCardNumber(lastPaymentRequestDto);

        return lastPaymentResponseEntity;
    }

    public boolean savePayment(PaymentRequestDto paymentRequest) {
        String paymentResponse = paymentFeignClient.savePayment(paymentRequest);

        return true;
    }
}
