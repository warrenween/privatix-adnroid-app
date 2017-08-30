package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 13.11.2015.
 */
public class Software {
    @Expose
    String type;
    @Expose
    String version;

    @Expose
    String source = "store";


    public Software(String type, String version) {
        this.type = type;
        this.version = version;
    }


    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getSource() {
        return source;
    }
}
