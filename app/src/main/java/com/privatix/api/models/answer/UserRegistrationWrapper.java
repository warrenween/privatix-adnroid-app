package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 20.11.2015.
 */
public class UserRegistrationWrapper {
    @Expose
    String status;

    @Expose
    String error;

    @Expose
    String sid;

    public boolean isStatusOk() {
        return status.equals("ok");
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getSid() {
        return sid;
    }
}
