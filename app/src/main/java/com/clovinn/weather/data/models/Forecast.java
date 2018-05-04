package com.clovinn.weather.data.models;

/**
 * Created by kennyorellana on 4/5/18.
 */

public class Forecast {
    private String summary;
    private String icon;
    private double precipProbability;
    private double temperature;
    private double humidity;

    public String getSummary() {
        return summary;
    }

    public String getIcon() {
        return icon;
    }

    public String getPrecipProbability() {
        return (int)(precipProbability*100) + "%";
    }

    public String getTemperature() {
        return (int)temperature + "º";
    }

    public String getHumidity() {
        return (int)(humidity * 100) + "%";
    }
}
