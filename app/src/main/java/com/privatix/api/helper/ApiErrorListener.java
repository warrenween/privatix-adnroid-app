package com.privatix.api.helper;

/**
 * Created by Lotar on 23.04.2016.
 */
public interface ApiErrorListener {

    public void onError(int errorStatus, int errorCode, String error);
}
