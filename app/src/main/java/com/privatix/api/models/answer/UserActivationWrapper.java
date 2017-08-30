package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.privatix.api.models.answer.subscription.SubscriptionSession;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserActivationWrapper {
    @Expose
    private
    String status;

    @Expose
    private
    String error;

    @SerializedName("server_time")
    @Expose
    private
    Long serverTime;

    @Expose
    private
    String sid;

    @Expose
    private
    String country;


    @SerializedName("original_country")
    @Expose
    private
    String originalCountry;

    @SerializedName("current_ip")
    @Expose
    private
    String currentIp;


    @Expose
    private
    SubscriptionSession subscription;


    public String getOriginalCountry() {
        return originalCountry;
    }

    public String getError() {
        return error;
    }

    public String getStatus() {
        return status;
    }


    public boolean isStatusOk() {
        return status.equals("ok");
    }


    public Long getServerTime() {
        return serverTime;
    }

    public String getSid() {
        return sid;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public SubscriptionSession getSubscription() {
        return subscription;
    }
}
