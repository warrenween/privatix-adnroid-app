package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 20.11.2015.
 */
public class TraceErrorWrapper {
    @Expose
    private
    String status;
    @Expose
    private
    String error;


    public String getError() {
        return error;
    }

    public boolean isStatusOk() {
        return status.equals("ok");
    }
}
