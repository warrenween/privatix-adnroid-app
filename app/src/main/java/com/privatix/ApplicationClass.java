package com.privatix;

import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.orm.SugarApp;
import com.privatix.services.AdNotificationService;
import com.zendesk.sdk.network.impl.ZendeskConfig;

import org.strongswan.android.security.LocalCertificateKeyStoreProvider;

import java.security.Security;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Lotar on 19.11.2015.
 */
public class ApplicationClass extends SugarApp {

    static {
        Security.addProvider(new LocalCertificateKeyStoreProvider());
    }

    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        FacebookSdk.sdkInitialize(this);

        initZendesk();
        initFlurry();
        startService(new Intent(this, AdNotificationService.class));
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    public void initZendesk() {
        ZendeskConfig.INSTANCE.init(this, "https://privatix.zendesk.com", "066132e8c639addad67bf202b57164105bc8eb85b6180938", "mobile_sdk_client_c690252b5974d75dac92");
    }

    // configure and init Flurry
    public void initFlurry() {
        Log.d("Application", "onCreate initFlurry");
    }


    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }


}
