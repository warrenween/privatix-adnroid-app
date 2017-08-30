package com.privatix.api.models.answer.subscription;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 14.11.2015.
 */
public class Time {
    @Expose
    Integer used;

    @Expose
    Integer limit;

    @Expose
    Integer avaliable;
}
