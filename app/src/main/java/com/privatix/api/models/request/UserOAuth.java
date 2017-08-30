package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 07.12.2015.
 */
public class UserOAuth {
    @Expose
    String token;
    @Expose
    String provider;


    public UserOAuth(String token, String provider) {
        this.token = token;
        this.provider = provider;
    }
}
