package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserCheckMail {
    @Expose
    String login;

    public UserCheckMail(String login) {
        this.login = login;
    }
}
