package ru.neoflex.scammertracking.analyzer.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.error.exception.BadRequestException;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;

@Service
@Slf4j
public class FeignService {

    @Autowired
    public FeignService(PaymentFeignClient paymentFeignClient) {
        this.paymentFeignClient = paymentFeignClient;
    }

    private PaymentFeignClient paymentFeignClient;

    public LastPaymentResponseDto getLastPayment(PaymentRequestDto paymentRequest) throws RuntimeException {
        log.info("Received paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        LastPaymentRequestDto lastPaymentRequestDto = new LastPaymentRequestDto(paymentRequest.getPayerCardNumber());
        LastPaymentResponseDto lastPaymentResponse;
        try {
            lastPaymentResponse = paymentFeignClient.getLastPaymentByPayerCardNumber(lastPaymentRequestDto);
        } catch (NotFoundException e) {
            log.error("The payment with cardNumber={} not found", paymentRequest.getPayerCardNumber());
            throw new NotFoundException(e.getMessage());
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        log.info("Response payment entity={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                lastPaymentResponse.getId(), lastPaymentResponse.getPayerCardNumber(), lastPaymentResponse.getReceiverCardNumber(), lastPaymentResponse.getCoordinates().getLatitude(), lastPaymentResponse.getCoordinates().getLongitude(), lastPaymentResponse.getDate());

        return lastPaymentResponse;
    }

    public void savePayment(PaymentRequestDto paymentRequest) {
        try {
            paymentFeignClient.savePayment(paymentRequest);
        } catch (BadRequestException e) {
            throw new BadRequestException(e.getMessage());
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
