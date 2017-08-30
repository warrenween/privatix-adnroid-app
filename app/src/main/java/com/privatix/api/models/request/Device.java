package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jaredrummler.android.device.DeviceName;

/**
 * Created by Lotar on 13.11.2015.
 */
public class Device {
    @Expose
    String name;
    @Expose
    String type;
    @Expose
    String model;

    @SerializedName("device_id")
    @Expose
    String deviceId;

    @Expose
    Os os;


    public Device(String model, Os os) {
        this.name = DeviceName.getDeviceName();
        this.type = "Mobile";
        this.model = model;
        this.os = os;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Os getOs() {
        return os;
    }
}
