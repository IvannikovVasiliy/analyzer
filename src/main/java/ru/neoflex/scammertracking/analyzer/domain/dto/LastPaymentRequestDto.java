package ru.neoflex.scammertracking.analyzer.domain.dto;

public class LastPaymentRequestDto {

    public LastPaymentRequestDto(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public LastPaymentRequestDto() {
    }

    private String cardNumber;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}
