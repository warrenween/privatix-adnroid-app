package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 20.11.2015.
 */
public class MetricWrapper {
    @Expose
    String status;
    @Expose
    String error;

    @Expose
    String details;


    public String getError() {
        return error;
    }

    public boolean isStatusOk() {
        return status.equals("ok");
    }
}
