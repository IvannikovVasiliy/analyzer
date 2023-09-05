package ru.neoflex.scammertracking.analyzer.feign;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.neoflex.scammertracking.analyzer.config.AnalyzerConfig;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.model.Coordinates;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.utils.Constants;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@SpringJUnitConfig({AnalyzerConfig.class})
class FeignServiceTest {

    @Mock
    private PaymentFeignClient paymentFeignClient;

    @InjectMocks
    private FeignService feignService;

    @Test
    public void getLastPaymentTest() {
        PaymentRequestDto paymentRequest = new PaymentRequestDto(1, Constants.PAYER_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, Constants.COORDINATES, LocalDateTime.now());
        LastPaymentResponseDto lastPaymentResponseDto = new LastPaymentResponseDto(1, Constants.PAYER_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, Constants.COORDINATES, LocalDateTime.now());

        when(paymentFeignClient.getLastPaymentByPayerCardNumber(Mockito.any())).thenReturn(lastPaymentResponseDto);

        LastPaymentResponseDto response = feignService.getLastPayment(paymentRequest);
        assertEquals(lastPaymentResponseDto.getId(), response.getId());
        assertEquals(lastPaymentResponseDto.getPayerCardNumber(), response.getPayerCardNumber());
        assertEquals(lastPaymentResponseDto.getReceiverCardNumber(), response.getReceiverCardNumber());
        assertEquals(lastPaymentResponseDto.getCoordinates(), response.getCoordinates());
        assertEquals(lastPaymentResponseDto.getDate(), response.getDate());
    }

    @Test
    public void getLastPaymentThrowTest() {
        LastPaymentRequestDto fakeLastPaymentRequest = new LastPaymentRequestDto(Constants.FAKE_CARD_NUMBER);
        Coordinates coordinates = new Coordinates(Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_1);
        PaymentRequestDto paymentRequest = new PaymentRequestDto(1, Constants.FAKE_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, coordinates, LocalDateTime.now());

        when(paymentFeignClient.getLastPaymentByPayerCardNumber(Mockito.any())).thenThrow(new NotFoundException("The payment with the cardNumber not found"));

        assertThrows(NotFoundException.class, () -> {
            feignService.getLastPayment(paymentRequest);
        });
    }
}