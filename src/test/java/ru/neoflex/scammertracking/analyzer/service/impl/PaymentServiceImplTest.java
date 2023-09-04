package ru.neoflex.scammertracking.analyzer.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.utils.Constants;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class PaymentServiceImplTest {

    @Mock
    private PaymentCacheDao paymentCacheDao;
    @Mock
    private FeignService feignService;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    public void getLastPaymentTest() throws Exception {
        final String PAYER_CARD_NUMBER_DEPRECATED = "987654321";
        final String NULL_PAYER_CARD_NUMBER = "null";
        final LocalDateTime DEPRECATED_DATETIME = LocalDateTime.now().minusMonths(1);

        PaymentRequestDto paymentRequest = new PaymentRequestDto(Constants.ID, Constants.PAYER_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, Constants.COORDINATES, LocalDateTime.now());
        PaymentRequestDto paymentRequestDeprecated = new PaymentRequestDto(Constants.ID, PAYER_CARD_NUMBER_DEPRECATED, Constants.RECEIVER_CARD_NUMBER, Constants.COORDINATES, LocalDateTime.now());
        PaymentRequestDto paymentRequestNull = new PaymentRequestDto(Constants.ID, NULL_PAYER_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, Constants.COORDINATES, LocalDateTime.now());
        PaymentEntity paymentEntity = new PaymentEntity(Constants.PAYER_CARD_NUMBER, Constants.RECEIVER_CARD_NUMBER, Constants.ID, Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_1, LocalDateTime.now(), LocalDateTime.now());
        PaymentEntity paymentEntityDeprecated =  new PaymentEntity(PAYER_CARD_NUMBER_DEPRECATED, Constants.RECEIVER_CARD_NUMBER, Constants.ID, Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_1, LocalDateTime.now(), DEPRECATED_DATETIME);

        when(paymentCacheDao.findPaymentByCardNumber(Constants.PAYER_CARD_NUMBER)).thenReturn(paymentEntity);
        when(paymentCacheDao.findPaymentByCardNumber(PAYER_CARD_NUMBER_DEPRECATED)).thenReturn(paymentEntityDeprecated);
        when(paymentCacheDao.findPaymentByCardNumber(NULL_PAYER_CARD_NUMBER)).thenReturn(null);

        paymentService.getLastPayment(paymentRequest, new AtomicBoolean());
        paymentService.getLastPayment(paymentRequestDeprecated, new AtomicBoolean());
        paymentService.getLastPayment(paymentRequestNull, new AtomicBoolean());
    }
}