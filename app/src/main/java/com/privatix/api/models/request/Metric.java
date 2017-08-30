package com.privatix.api.models.request;

/**
 * Created by Lotar on 20.11.2015.
 */
public class Metric {
    String type;
    String at;

    public Metric(String type, String at) {
        this.type = type;
        this.at = at;
    }
}
