package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 05.05.2016.
 */
public class UserPayment {
    @Expose
    String token;

    @Expose
    String provider = "gp";


    public UserPayment(String token) {
        this.token = token;
    }
}
