package com.privatix.api.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.privatix.R;
import com.privatix.async.TimeoutErrorSend;
import com.privatix.services.MyVpnListenerService;

import org.strongswan.android.data.VpnProfile;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class NoActivityCustomErrorHandler implements ErrorHandler {
    public static final String TAG = CustomErrorHandler.class.getSimpleName();
    public Tracker mTracker;
    private Context mContext;
    private String connectedCountry = "", connectedNode = "";
    private MyVpnListenerService myVpnListenerService;
    private final ServiceConnection myVpnListenerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            myVpnListenerService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "myVpnListenerServiceConnection connected");
            myVpnListenerService = ((MyVpnListenerService.LocalBinder) service).getService();
            VpnProfile profile = myVpnListenerService.getProfile();
            if (profile != null) {
                connectedCountry = profile.getCountryCode();
                connectedNode = profile.getGateway();
            }
            if (mContext != null)
                mContext.unbindService(myVpnListenerServiceConnection);
        }
    };

    public NoActivityCustomErrorHandler(Context mContext, Tracker tracker) {
        mContext.bindService(new Intent(mContext, MyVpnListenerService.class),
                myVpnListenerServiceConnection, 0);
        mTracker = tracker;
        if (mTracker != null)
            mTracker.setScreenName("");
        this.mContext = mContext;
    }

    @Override
    public Throwable handleError(RetrofitError retrofitError) {
        Log.e(TAG, "handle error ");
        retrofitError.printStackTrace();
        Response r = retrofitError.getResponse();
        String messageRest = "";
        int errorCode = -1;
        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {
            Log.e(TAG, "network error: message: " + retrofitError.getMessage() + " cause message: " + retrofitError.getCause().toString() +
                    " loc message: " + retrofitError.getLocalizedMessage() +
                    " SuccessType" + retrofitError.getSuccessType());
            Log.e(TAG, retrofitError.getCause().toString());
//            Log.e(TAG , "Error body " +retrofitError.getBody().toString()+"");
            if (retrofitError.getResponse() != null) {
                Log.e(TAG, " Reason: " + retrofitError.getResponse().getReason());
            }
            if (retrofitError.getCause().toString().equals(mContext.getString(R.string.api_error_timeout))) {
                TimeoutErrorSend timeoutErrorSend = new TimeoutErrorSend(mContext, mTracker, retrofitError, retrofitError.getCause().toString(),
                        errorCode, connectedCountry, connectedNode);
                timeoutErrorSend.timeOutError();
            }
        } else if (r != null && mContext != null) {
            try {
                RestError restError = ((RestError) retrofitError.getBodyAs(RestError.class));
                messageRest = restError.getMessage();
                errorCode = restError.getError_code();
            } catch (Exception e) {
                Log.e(TAG, "catch error " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            Log.e(TAG, "API Error" + " Status " + r.getStatus() + " errorCode: " + errorCode + " error message: " + messageRest);
            switch (r.getStatus()) {
                case 408:
                    Log.e(TAG, "error 408");
                    TimeoutErrorSend timeoutErrorSend = new TimeoutErrorSend(mContext, mTracker, retrofitError, messageRest, errorCode, connectedCountry, connectedNode);
                    timeoutErrorSend.timeOutError();
//                    String apiMethod;
//                    String label;
//                    if (retrofitError.getUrl().contains(ApiClient.ENDPOINT)) {
//                        apiMethod = retrofitError.getUrl().replace(ApiClient.ENDPOINT, "");
//                        apiMethod = apiMethod.substring(1);
//                        Log.d("Api method", apiMethod);
//                        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
//                        if (profileTables.size() > 0 && !TextUtils.isEmpty(apiMethod)) {
//                            ProfileTable currentProfileTable = profileTables.get(0);
//                            String originalCountry= currentProfileTable.getOriginalCountry();
//                            if (!TextUtils.isEmpty(connectedCountry))
//                                label = originalCountry + "_" + connectedCountry + "_" + apiMethod;
//                            else
//                                label = originalCountry + "_" + apiMethod;
//                            if (mTracker != null) {
//
//                                AnalyticsUtils.sendEventTimedOut(mContext, mTracker, label);
//                            }
//                            Log.e(TAG, "Timeout error send");
//                            TraceErrorSend traceErrorSend = new TraceErrorSend(mContext, mContext.getString(R.string.trace_error_timeout), "Error: "+ errorCode,
//                                    messageRest, originalCountry, connectedNode);
//                            traceErrorSend.sentError();
//                        }
//                    }
                    break;
            }
        }
        return retrofitError;
    }
}