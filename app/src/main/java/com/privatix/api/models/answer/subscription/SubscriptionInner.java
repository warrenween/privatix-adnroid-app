package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Lotar on 05.04.2016.
 */
public class SubscriptionInner {
    @Expose
    Quotes quotes;

    @Expose
    String login;

    @Expose
    String password;

    @Expose
    String plan;

    @Expose
    List<Nodes> nodes;

    @SerializedName("created_at")
    @Expose
    Integer createdAt;


    public Quotes getQuotes() {
        return quotes;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getPlan() {
        return plan;
    }

    public List<Nodes> getNodes() {
        return nodes;
    }

    public Integer getCreatedAt() {
        return createdAt;
    }
}
