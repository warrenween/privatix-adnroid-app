package com.privatix.api.models.answer;

/**
 * Created by Lotar on 17.12.2015.
 */
public class UserCheckMailWrapper {
    String status;
    String details;

    public boolean isStatusOk() {
        return status.equals("ok");
    }


    public String getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }
}
