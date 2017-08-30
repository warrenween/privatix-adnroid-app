package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Lotar on 14.11.2015.
 */
public class Quotes {
    @SerializedName("expires_at")
    @Expose
    String expiresAt;


    @SerializedName("bandwidth")
    @Expose
    Bandwidth bandwidth;

    @SerializedName("time")
    @Expose
    Time time;

    @SerializedName("sessions")
    @Expose
    Sessions sessions;


    public String getExpiresAt() {
        return expiresAt;
    }

    public Bandwidth getBandwidth() {
        return bandwidth;
    }

    public Time getTime() {
        return time;
    }

    public Sessions getSessions() {
        return sessions;
    }
}
