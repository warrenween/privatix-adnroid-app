package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.privatix.api.models.answer.subscription.SubscriptionSession;

import java.util.List;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserSessionWrapper {
    @Expose
    String status;

    @Expose
    Integer error;

    @SerializedName("server_time")
    @Expose
    Integer serverTime;

    @SerializedName("country")
    @Expose
    String country;


    @SerializedName("is_authorized")
    @Expose
    Boolean isAuthorized;

    @SerializedName("is_verified")
    @Expose
    Boolean isVerified;
    ;


    @SerializedName("connected")
    @Expose
    Boolean connected;
    ;


    @SerializedName("original_country")
    @Expose
    String originalCountry;

    @SerializedName("current_ip")
    @Expose
    String currentIp;

    @SerializedName("subscription")
    @Expose
    SubscriptionSession subscription;

    List<Notification> notifications;


    public boolean isStatusOk() {
        return status.equals("ok");
    }

    public Integer getError() {
        return error;
    }

    public SubscriptionSession getSubscription() {
        return subscription;
    }

    public String getStatus() {
        return status;
    }

    public Integer getServerTime() {
        return serverTime;
    }

    public String getCountry() {
        return country;
    }

    public Boolean getIsAuthorized() {
        return isAuthorized;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public Boolean getConnected() {
        return connected;
    }

    public String getOriginalCountry() {
        return originalCountry;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
}
