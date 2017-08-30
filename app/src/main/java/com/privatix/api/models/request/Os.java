package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 13.11.2015.
 */
public class Os {
    @Expose
    String name;
    @Expose
    String version;
    @Expose
    String family;


    public Os(String name, String version, String family) {
        this.name = name;
        this.version = version;
        this.family = family;
    }


    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getFamily() {
        return family;
    }
}
