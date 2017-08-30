package com.privatix.utils;

/**
 * Created by Lotar on 09.04.2016.
 */
public class Constants {
    public static final Integer NOTIFY_ID_RISK = 100;


    public static final int NOTIFY_ID_DISCONNECTED = 1111;
    public static final int NOTIFY_ID_FREE_CONNECTED = 2222;
    public static final int NOTIFY_ID_CONNECTION_LOST = 3333;
    public static final int NOTIFY_ID_VPN_STATUS = 3424;


    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = ONE_SECOND * 60;
    public static final long ONE_HOUR = ONE_MINUTE * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;


    public static final long MAX_TIME_FOR_CONNECTING = ONE_SECOND * 3;
    public static final long MAX_TIME_FOR_SELECTING_ANOTHER_HOST = ONE_SECOND * 5;


    /*Server error code */
    public static final int JSON_SCHEMA_ERROR = 4003;
    public static final int DEVICE_LIMIT_REACHED = 4031;
    public static final int SESSION_REQUIRED = 4032;


    /*Small icon type*/
    public static final int NOTIFICATION_SMALL_ICON_START_RATE = 200;
    public static final int NOTIFICATION_SMALL_ICON_REPORT = 201;


    public static final String GOOGLE_ANALYTICS_DEBUG_TAG = "google_debug_tag";
    public static final String FLURRY_DEBUG_TAG = "flurry_debug_tag";


    public static final String SKU_PREMIUM_MONTHLY = "subscription_monthly";
    public static final String SKU_PREMIUM_YEARLY = "subscription_yearly";


    //public static final String SOFTWARE_NAME = "Android-App/" + Build.VERSION.RELEASE;

    public static final String CONNECTION_STATE = "connection_state";

    public static final String BOOT_START_EXTRA = "need_start_on_boot";
}


