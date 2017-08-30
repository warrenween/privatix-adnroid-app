package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 12.11.2015.
 */
public class UserAuthorization {
    @Expose
    String login;
    @Expose
    String password;

    public UserAuthorization(String login, String password) {
        this.login = login;
        this.password = password;
    }
}
