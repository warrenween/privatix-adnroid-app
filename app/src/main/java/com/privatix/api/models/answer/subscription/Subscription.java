package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Lotar on 12.11.2015.
 */
public class Subscription {


    @SerializedName("subscription_id")
    @Expose
    String subscriptionId;

    @Expose
    String active;

    @SerializedName("subscription")
    @Expose
    SubscriptionInner subscriptionInner;


    public Quotes getQuotes() {
        return subscriptionInner.getQuotes();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Integer getCreatedAt() {
        return subscriptionInner.getCreatedAt();
    }

    public String getLogin() {
        return subscriptionInner.getLogin();
    }

    public String getPassword() {
        return subscriptionInner.getPassword();
    }

    public String getPlan() {
        return subscriptionInner.getPlan();
    }

    public String getActive() {
        return active;
    }

    public List<Nodes> getNodes() {
        return subscriptionInner.getNodes();
    }


}
