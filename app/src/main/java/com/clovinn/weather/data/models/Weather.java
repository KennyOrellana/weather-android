package com.clovinn.weather.data.models;

/**
 * Created by kennyorellana on 4/5/18.
 */

public class Weather {
    private String timezone;
    private Forecast currently;

    public String getTimezone() {
        return timezone;
    }

    public Forecast getCurrently() {
        return currently;
    }
}
