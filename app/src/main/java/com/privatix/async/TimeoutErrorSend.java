package com.privatix.async;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.privatix.R;
import com.privatix.api.ApiClient;
import com.privatix.model.ProfileTable;
import com.privatix.utils.AnalyticsUtils;

import java.util.List;

import retrofit.RetrofitError;

/**
 * Created by Lotar on 08.10.2016.
 */

public class TimeoutErrorSend {
    public static final String TAG = TimeoutErrorSend.class.getSimpleName();
    private Context context;
    private Tracker tracker;
    private RetrofitError retrofitError;
    private String message;
    private int errorCode;
    private String connectedCountry;
    private String connectedNode;

    public TimeoutErrorSend(Context context, Tracker tracker, RetrofitError retrofitError, String message, int errorCode, String connectedCountry, String connectedNode) {
        this.context = context;
        this.tracker = tracker;
        this.retrofitError = retrofitError;
        this.message = message;
        this.errorCode = errorCode;
        this.connectedCountry = connectedCountry;
        this.connectedNode = connectedNode;
    }


    public void timeOutError() {
        String apiMethod;
        String label;
        if (retrofitError.getUrl().contains(ApiClient.ENDPOINT)) {
            apiMethod = retrofitError.getUrl().replace(ApiClient.ENDPOINT, "");
            apiMethod = apiMethod.substring(1);
            Log.d("Api method", apiMethod);
            List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
            if (profileTables.size() > 0 && !TextUtils.isEmpty(apiMethod)) {
                ProfileTable currentProfileTable = profileTables.get(0);
                String originalCountry = currentProfileTable.getOriginalCountry();
                if (!TextUtils.isEmpty(connectedCountry))
                    label = originalCountry + "_" + connectedCountry + "_" + apiMethod;
                else
                    label = originalCountry + "_" + apiMethod;
                if (tracker != null) {
                    Log.e(TAG, "Timeout error send");
                    AnalyticsUtils.sendEventTimedOut(context, tracker, label);
                    TraceErrorSend traceErrorSend = new TraceErrorSend(context, context.getString(R.string.trace_error_timeout), message, originalCountry, connectedNode);
                    traceErrorSend.sentError();
                }
            }
        }
    }
}
