package ru.neoflex.scammertracking.analyzer.domain.dto;

public class LastPaymentRequestDto {

    public LastPaymentRequestDto(String payerCardNumber) {
        this.payerCardNumber = payerCardNumber;
    }

    public LastPaymentRequestDto() {
    }

    private String payerCardNumber;

    public String getPayerCardNumber() {
        return payerCardNumber;
    }

    public void setPayerCardNumber(String payerCardNumber) {
        this.payerCardNumber = payerCardNumber;
    }
}
