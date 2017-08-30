package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by Lotar on 18.04.2016.
 */
public class Hosts extends SugarRecord {
    String host;

    //defining a relationship
    SubscriptionTable subscription;

    public Hosts() {
    }

    public Hosts(String host, SubscriptionTable subscriptionTable) {
        this.host = host;
        this.subscription = subscriptionTable;
    }

    public String getHost() {
        return host;
    }
}
