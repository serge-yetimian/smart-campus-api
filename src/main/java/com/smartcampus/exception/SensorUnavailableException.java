package com.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {
    private String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor " + sensorId + " is currently under maintenance and cannot accept readings");
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }
}