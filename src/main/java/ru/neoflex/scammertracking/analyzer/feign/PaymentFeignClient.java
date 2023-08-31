package ru.neoflex.scammertracking.analyzer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;

@FeignClient(name = "feignService", url = "http://localhost:8082/payment")
public interface PaymentFeignClient {

    @PostMapping("/last-payment")
    LastPaymentResponseDto getLastPaymentByReceiverCardNumber(@RequestBody LastPaymentRequestDto payment);

    @PostMapping("/save")
    @ResponseStatus(value = HttpStatus.CREATED)
    String savePayment(@RequestBody PaymentRequestDto payment);
}
