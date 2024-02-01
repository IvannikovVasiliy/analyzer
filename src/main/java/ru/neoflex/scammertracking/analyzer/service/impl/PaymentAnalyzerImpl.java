package ru.neoflex.scammertracking.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.neoflex.scammertracking.analyzer.dao.PaymentCacheDao;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.entity.PaymentEntity;
import ru.neoflex.scammertracking.analyzer.error.exception.BadRequestException;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.feign.FeignService;
import ru.neoflex.scammertracking.analyzer.geo.SimplePaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.kafka.producer.PaymentProducer;
import ru.neoflex.scammertracking.analyzer.service.PaymentAnalyzer;
import ru.neoflex.scammertracking.analyzer.service.PaymentService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentAnalyzerImpl implements PaymentAnalyzer {

    private final FeignService feignService;
    private final PaymentService paymentService;
    private final PaymentCacheDao paymentCacheDao;
    private final PaymentProducer paymentProducer;
    private final ModelMapper modelMapper;

    @Value("${spring.kafka.topic.suspicious-payments}")
    private String suspiciousPaymentsTopic;
    @Value("${spring.kafka.topic.checked-payments}")
    private String checkedPaymentsTopic;

    @Override
    public void analyzeConsumeMessage(String key, PaymentRequestDto paymentRequest) throws Exception {
        log.info("received key={} paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                key, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        PaymentResponseDto paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
        AtomicBoolean isCacheDeprecated = new AtomicBoolean();

        boolean isTrusted;
        if (checkSuspicious(paymentRequest)) {
            log.info("response. Sent message with key={} in topic={}", key, suspiciousPaymentsTopic);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            return;
        }

        LastPaymentResponseDto lastPayment = null;
        try {
            lastPayment = paymentService.getLastPayment(paymentRequest, isCacheDeprecated);
        } catch (BadRequestException | NotFoundException e) {
            paymentResult = modelMapper.map(paymentRequest, PaymentResponseDto.class);
            paymentResult.setTrusted(false);
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.warn("sent message with key={} in topic {}", key, suspiciousPaymentsTopic);
            return;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new Exception(e.getMessage());
        }

        isTrusted = SimplePaymentAnalyzer.checkPayment(lastPayment, paymentRequest);
        paymentResult.setTrusted(isTrusted);

        routePayment(isTrusted, isCacheDeprecated, paymentRequest, paymentResult);
    }

    private boolean checkSuspicious(PaymentRequestDto paymentRequest) {
        log.info("Check suspicious. paymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }",
                paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate());

        if (paymentRequest.getPayerCardNumber().length() < 6) {
            log.warn("Result validating. The message is suspicious, because the length of payerCardNumber is too short");
            return true;
        }
        if (paymentRequest.getReceiverCardNumber().length() < 6) {
            log.warn("Result validating. The message is suspicious, because the length of receiverCardNumber is too short");
            return true;
        }
        if (LocalDateTime.now().isBefore(paymentRequest.getDate())) {
            log.warn("Result validating. The message is suspicious, because date of paymentRequest more than current datetime");
            return true;
        }

        log.info("Result validating. The message is valid");
        return false;
    }

    private void routePayment(boolean isTrusted, AtomicBoolean isCacheDeprecated, PaymentRequestDto paymentRequest, PaymentResponseDto paymentResult) throws Exception {
        log.info("Received. isTrusted={}, isCacheDeprecated={}.\n PaymentRequest={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }.\n Payment result={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={}, trusted = {}}",
                isTrusted, isCacheDeprecated, paymentRequest.getId(), paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(), paymentRequest.getDate(), paymentResult.getId(), paymentResult.getPayerCardNumber(), paymentResult.getReceiverCardNumber(), paymentRequest.getCoordinates().getLatitude(), paymentResult.getCoordinates().getLongitude(), paymentResult.getDate(), paymentResult.getTrusted());

        if (isTrusted) {
            try {
                feignService.savePayment(paymentRequest);
            } catch (BadRequestException e) {
                paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
                log.info("Response. Sent message in topic={}, BadRequest because of {}",
                        suspiciousPaymentsTopic, e.getMessage());
                return;
            } catch (Exception e) {
                log.info("Internal error");
                throw new Exception(e.getMessage());
            }

            if (isCacheDeprecated.get()) {
                PaymentEntity paymentEntity = new PaymentEntity(paymentRequest.getPayerCardNumber(), paymentRequest.getReceiverCardNumber(),
                        paymentRequest.getId(),
                        paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude(),
                        paymentRequest.getDate(), LocalDateTime.now());
                if (paymentCacheDao.findPaymentByCardNumber(paymentRequest.getPayerCardNumber()) != null) {
                    paymentCacheDao.update(paymentEntity);
                } else {
                    paymentCacheDao.save(paymentEntity);
                }
            }
            paymentProducer.sendMessage(checkedPaymentsTopic, paymentResult);
            log.info("response. Sent message in topic={}", checkedPaymentsTopic);
        } else {
            paymentProducer.sendMessage(suspiciousPaymentsTopic, paymentResult);
            log.info("Response. Sent message in topic={}, because latitude={}, longitude={}",
                    suspiciousPaymentsTopic, paymentRequest.getCoordinates().getLatitude(), paymentRequest.getCoordinates().getLongitude());
        }
    }
}
