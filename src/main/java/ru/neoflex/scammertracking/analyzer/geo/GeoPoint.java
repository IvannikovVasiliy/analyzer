package ru.neoflex.scammertracking.analyzer.geo;

public class GeoPoint {

    private final double lat;
    private final double lon;
    private final double accuracy;

    public GeoPoint(double lat, double lon){
        this(lat, lon, -1d);
    }

    public GeoPoint(double lat, double lon, double accuracy){
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy < 0 ? -1d : accuracy;
    }

    public double getLat(){
        return this.lat;
    }

    public double getLon(){
        return this.lon;
    }

    public double getAccuracy(){
        return this.accuracy;
    }

}