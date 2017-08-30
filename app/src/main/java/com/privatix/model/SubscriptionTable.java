package com.privatix.model;

import com.orm.SugarRecord;

import java.util.List;

/**
 * Created by Lotar on 12.11.2015.
 */
public class SubscriptionTable extends SugarRecord<SubscriptionTable> {
    private String country;

    private String countryCode;

    private Boolean isFree;

    public SubscriptionTable() {
    }


    public SubscriptionTable(String country, String countryCode, Boolean isFree) {
        this.country = country;
        this.countryCode = countryCode;
        this.isFree = isFree;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountry() {
        return country;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public List<Hosts> getHosts() {
        return Hosts.find(Hosts.class, "subscription = ?", String.valueOf(this.getId()));
    }
}
