package ru.neoflex.scammertracking.analyzer.domain.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@RedisHash("Payment")
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEntity implements Serializable {

    @Id
    private String payerCardNumber;
    private String receiverCardNumber;
    private Long idPayment;
    private float latitude;
    private float longitude;
    private LocalDateTime datePayment;
    private LocalDateTime dateUpdating;

    public Long getIdPayment() {
        return idPayment;
    }

    public void setIdPayment(Long idPayment) {
        this.idPayment = idPayment;
    }

    public String getPayerCardNumber() {
        return payerCardNumber;
    }

    public void setPayerCardNumber(String payerCardNumber) {
        this.payerCardNumber = payerCardNumber;
    }

    public String getReceiverCardNumber() {
        return receiverCardNumber;
    }

    public void setReceiverCardNumber(String receiverCardNumber) {
        this.receiverCardNumber = receiverCardNumber;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getDatePayment() {
        return datePayment;
    }

    public void setDatePayment(LocalDateTime datePayment) {
        this.datePayment = datePayment;
    }

    public LocalDateTime getDateUpdating() {
        return dateUpdating;
    }

    public void setDateUpdating(LocalDateTime dateUpdating) {
        this.dateUpdating = dateUpdating;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append("paymentEntity: {")
                .append("\"idPayment\": ").append(idPayment)
                .append("\"payerCardNumber\": ").append(payerCardNumber)
                .append("\"receiverCardNumber\": ").append(receiverCardNumber)
                .append("\"latitude\": ").append(latitude)
                .append("\"longitude\": ").append(longitude)
                .append("\"datePayment\": ").append(datePayment)
                .append("\"dateUpdating\": ").append(dateUpdating)
                .toString();
    }
}
