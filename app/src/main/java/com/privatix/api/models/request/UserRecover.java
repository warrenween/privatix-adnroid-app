package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserRecover {
    @Expose
    String login;

    public UserRecover(String login) {
        this.login = login;
    }
}
