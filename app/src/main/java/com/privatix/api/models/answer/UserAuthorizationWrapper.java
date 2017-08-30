package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.privatix.api.models.answer.subscription.Subscription;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserAuthorizationWrapper {
    @Expose
    String status;

    @Expose
    String error;

    @Expose
    String sid;


    @SerializedName("subscription")
    @Expose
    Subscription subscription;

    public boolean isStatusOk() {
        return status.equals("ok");
    }


    public String getError() {
        return error;
    }

    public String getStatus() {
        return status;
    }

    public String getSid() {
        return sid;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
