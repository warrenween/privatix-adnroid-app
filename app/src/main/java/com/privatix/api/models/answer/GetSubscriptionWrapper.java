package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.privatix.api.models.answer.subscription.Subscription;

/**
 * Created by Lotar on 15.11.2015.
 */
public class GetSubscriptionWrapper {
    @Expose
    String status;

    @Expose
    String error;
    @Expose
    Subscription subscription;
    @SerializedName("original_country")
    @Expose
    private
    String originalCountry;

    public String getOriginalCountry() {
        return originalCountry;
    }

    public boolean isStatusOk() {
        return status.equals("ok");
    }


    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
