package ru.neoflex.scammertracking.analyzer.geo;

import org.junit.jupiter.api.Test;
import ru.neoflex.scammertracking.analyzer.Constants;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoCoordinatesTest {

    @Test
    public void testCalculateDistance() {
        final double ACCURACY = 50;
        final int TEST_DISTANCE_1 = 7300;
        final int TEST_DISTANCE_2 = 11650;
        final int TEST_DISTANCE_3 = 8000;
        final int TEST_DISTANCE_4 = 7300;

        GeoPoint geoPoint1 = new GeoPoint(Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_1);
        GeoPoint geoPoint2 = new GeoPoint(Constants.TEST_COORDINATE_3, Constants.TEST_COORDINATE_3);
        GeoPoint geoPoint3 = new GeoPoint(Constants.TEST_COORDINATE_2, Constants.TEST_COORDINATE_1);
        GeoPoint geoPoint4 = new GeoPoint(Constants.TEST_COORDINATE_1, Constants.TEST_COORDINATE_2);
        GeoPoint geoPoint5 = new GeoPoint(Constants.TEST_COORDINATE_2, Constants.TEST_COORDINATE_2);
        GeoPoint geoPoint6 = new GeoPoint(Constants.TEST_COORDINATE_4, Constants.TEST_COORDINATE_4);

        double distance1 = GeoCoordinates.calculateDistance(geoPoint1, geoPoint2);
        double distance2 = GeoCoordinates.calculateDistance(geoPoint3, geoPoint2);
        double distance3 = GeoCoordinates.calculateDistance(geoPoint4, geoPoint2);
        double distance4 = GeoCoordinates.calculateDistance(geoPoint5, geoPoint6);

        assertTrue(abs(TEST_DISTANCE_1 - distance1) < ACCURACY);
        assertTrue(abs(TEST_DISTANCE_2 - distance2) < ACCURACY);
        assertTrue(abs(TEST_DISTANCE_3 - distance3) < ACCURACY);
        assertTrue(abs(TEST_DISTANCE_4 - distance4) < ACCURACY);
    }
}