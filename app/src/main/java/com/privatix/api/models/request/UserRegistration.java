package com.privatix.api.models.request;

/**
 * Created by Lotar on 20.11.2015.
 */
public class UserRegistration {

    String login;
    String password;
    /*@SerializedName("device")
    @Expose
    Device device;
    @SerializedName("software")
    @Expose
    Software software;*/


    public UserRegistration(String login, String password) {
        this.login = login;
        this.password = password;
    }
}

