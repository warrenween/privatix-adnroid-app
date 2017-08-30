package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserActivation {

    @SerializedName("device")
    @Expose
    Device device;
    @SerializedName("software")
    @Expose
    Software software;

    public UserActivation(Device device, Software software) {
        this.device = device;
        this.software = software;
    }


    public Device getDevice() {
        return device;
    }

    public Software getSoftware() {
        return software;
    }
}



