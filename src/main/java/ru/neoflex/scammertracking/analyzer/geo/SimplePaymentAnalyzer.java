package ru.neoflex.scammertracking.analyzer.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.neoflex.scammertracking.analyzer.domain.dto.LastPaymentResponseDto;
import ru.neoflex.scammertracking.analyzer.domain.dto.PaymentRequestDto;

import java.time.LocalDateTime;

public class SimplePaymentAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePaymentAnalyzer.class);

    public static boolean checkPayment(LastPaymentResponseDto lastPayment, PaymentRequestDto currentPayment) {
        LOGGER.info("Received for check payment. lastPayment={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date ={} }.\n currentPayment={ id={}, payerCardNumber={}, receiverCardNumber={}, latitude={}, longitude={}, date={} }",
                lastPayment.getId(), lastPayment.getPayerCardNumber(), lastPayment.getReceiverCardNumber(), lastPayment.getCoordinates().getLatitude(), lastPayment.getCoordinates().getLongitude(), lastPayment.getDate(), currentPayment.getId(), currentPayment.getPayerCardNumber(), currentPayment.getReceiverCardNumber(), currentPayment.getCoordinates().getLatitude(), currentPayment.getCoordinates().getLongitude(), currentPayment.getDate());

        LocalDateTime lastPaymentDate = lastPayment.getDate();
        LocalDateTime currentPaymentDate = currentPayment.getDate();
        GeoPoint lastGeoPoint = new GeoPoint(lastPayment.getCoordinates().getLatitude(), lastPayment.getCoordinates().getLongitude());
        GeoPoint currentGeoPoint = new GeoPoint(currentPayment.getCoordinates().getLatitude(), currentPayment.getCoordinates().getLongitude());

        double distance = GeoCoordinates.calculateDistance(lastGeoPoint, currentGeoPoint);

        if (lastPaymentDate.plusHours(1).compareTo(currentPaymentDate)>=0 && distance > 10000) {
            LOGGER.warn("The payment with id={} is suspicious", currentPayment.getId());
            return false;
        }
        if (lastPaymentDate.plusMinutes(1).compareTo(currentPaymentDate)>=0 && distance > 50) {
            LOGGER.warn("The payment with id={} is suspicious", currentPayment.getId());
            return false;
        }
        if (lastPaymentDate.plusSeconds(1).compareTo(currentPaymentDate)>=0 && distance > 1) {
            LOGGER.warn("The payment with id={} is suspicious", currentPayment.getId());
            return false;
        }

        LOGGER.info("The payment with id={} is trusted", currentPayment.getId());

        return true;
    }
}
