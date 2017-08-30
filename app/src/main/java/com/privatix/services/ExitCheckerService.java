package com.privatix.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.PrefKeys;

/**
 * Created by ross on 11.05.16.
 */
public class ExitCheckerService extends Service {
    Context mContext;
    SharedPreferences sp;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ApplicationClass application = (ApplicationClass) getApplication();
        mContext = getApplicationContext();

        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, MODE_PRIVATE);
        Tracker mTracker = application.getDefaultTracker();

        if (sp.getBoolean(PrefKeys.APP_OPEN, true)) {
            sp.edit().putBoolean(PrefKeys.APP_OPEN, false).apply();
        } else {
            AnalyticsUtils.sendEventAppExit(this, mTracker);
            stopSelf();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
