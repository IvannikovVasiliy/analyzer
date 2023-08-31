package ru.neoflex.scammertracking.analyzer.geo;

import org.junit.jupiter.api.Test;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoCoordinatesTest {

    @Test
    public void testCalculateDistance() {
        final double ACCURACY = 50;

        double distance1 = GeoCoordinates.calculateDistance(new GeoPoint(21, 37), new GeoPoint(82, 54));
        double distance2 = GeoCoordinates.calculateDistance(new GeoPoint(-21, 37), new GeoPoint(82, 54));
        double distance3 = GeoCoordinates.calculateDistance(new GeoPoint(21, -37), new GeoPoint(82, 54));
        double distance4 = GeoCoordinates.calculateDistance(new GeoPoint(-21, -37), new GeoPoint(82, 54));

        assertTrue(abs(6822 - distance1) < ACCURACY);
        assertTrue(abs(11488 - distance2) < ACCURACY);
        assertTrue(abs(7725 - distance3) < ACCURACY);
        assertTrue(abs(12347 - distance4) < ACCURACY);
    }
}