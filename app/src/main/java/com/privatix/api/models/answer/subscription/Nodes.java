package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Lotar on 14.11.2015.
 */
public class Nodes {

    @Expose
    private
    String city;

    @Expose
    private
    String country;

    @SerializedName("display_name")
    @Expose
    private
    String displayName;

    @SerializedName("country_code")
    @Expose
    private
    String countryCode;

    @Expose
    private
    Integer priority;

    @Expose
    private
    Boolean free;

    @Expose
    private
    List<String> mode;

    private List<Hosts> hosts;


    public String getDisplayName() {
        return displayName;
    }

    public Boolean getFree() {
        return free;
    }

    public String getCity() {
        return city;
    }


    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<String> getMode() {
        return mode;
    }

    public List<Hosts> getHosts() {
        return hosts;
    }
}
