package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 15.11.2015.
 */
public class UserPaymentWrapper {
    @Expose
    String status;

    @Expose
    String error;


    public boolean isStatusOk() {
        return status.equals("ok");
    }


    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
