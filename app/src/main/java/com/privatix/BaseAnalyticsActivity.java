package com.privatix;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.privatix.utils.Utils;

public class BaseAnalyticsActivity extends AppCompatActivity {
    private static final String TAG = BaseAnalyticsActivity.class.getSimpleName();
    public Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!Utils.isTabletMoreThanSevenInches(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        initTracker();
        super.onCreate(savedInstanceState);
    }

    public void initTracker() {
        ApplicationClass application = (ApplicationClass) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mTracker != null) mTracker = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

    }
}
