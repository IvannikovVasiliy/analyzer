package ru.neoflex.scammertracking.analyzer.utils;

import ru.neoflex.scammertracking.analyzer.domain.model.Coordinates;

import java.time.LocalDateTime;

public class Constants {
    public static final int TEST_COORDINATE_1 = 20;
    public static final int TEST_COORDINATE_2 = -20;
    public static final int TEST_COORDINATE_3 = 80;
    public static final int TEST_COORDINATE_4 = -80;

    public static final long ID = 1;
    public static final String PAYER_CARD_NUMBER = "123456";
    public static final String RECEIVER_CARD_NUMBER = "654321";
    public static final String FAKE_CARD_NUMBER = "fake";
    public static final Coordinates COORDINATES = new Coordinates(Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_1);
    public static final LocalDateTime FUTURE_DATETIME = LocalDateTime.now().plusDays(1);
}
