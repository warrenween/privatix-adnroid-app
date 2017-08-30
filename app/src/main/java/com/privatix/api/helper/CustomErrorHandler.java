package com.privatix.api.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;
import com.orm.SugarApp;
import com.privatix.ApplicationClass;
import com.privatix.R;
import com.privatix.async.TimeoutErrorSend;
import com.privatix.fragments.dialogs.SimpleDialog;
import com.privatix.services.MyVpnListenerService;
import com.privatix.utils.Constants;
import com.privatix.utils.Utils;

import org.strongswan.android.data.VpnProfile;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class CustomErrorHandler implements ErrorHandler {
    public static final String TAG = CustomErrorHandler.class.getSimpleName();
    public Tracker mTracker;
    private AppCompatActivity activity;
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
                connectedNode = profile.getGateway();
                connectedCountry = profile.getCountryCode();
            }
            if (activity != null)
                activity.unbindService(myVpnListenerServiceConnection);
        }
    };

    public CustomErrorHandler(AppCompatActivity activity) {
        this.activity = activity;
        activity.bindService(new Intent(activity, MyVpnListenerService.class),
                myVpnListenerServiceConnection, 0);
        initTracker();
    }

    private void initTracker() {
        ApplicationClass application = (ApplicationClass) activity.getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    @Override
    public Throwable handleError(RetrofitError retrofitError) {
        Response r = retrofitError.getResponse();
        String messageToast = "";
        String messageRest = "";
        Context context = SugarApp.getSugarContext();
        int errorCode = -1;
        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {
            Log.e(TAG, "network error: message: " + retrofitError.getMessage() + " cause message: " + retrofitError.getCause().toString() +
                    " loc message: " + retrofitError.getLocalizedMessage() +
                    " SuccessType" + retrofitError.getSuccessType());
            Log.e(TAG, retrofitError.getCause().toString());
//            Log.e(TAG , "Error body " +retrofitError.getBody().toString()+"");
//            RestError restError = ((RestError) retrofitError.getBodyAs(RestError.class));
//            Log.e(TAG , "Error code " +restError.getError_code()+"");
//            Log.e(TAG , "Error message " +restError.getMessage()+"");
            if (retrofitError.getResponse() != null) {
                Log.e(TAG, " Reason: " + retrofitError.getResponse().getReason());
            }
            if (retrofitError.getCause().toString().equals(SugarApp.getSugarContext().getString(R.string.api_error_timeout))) {
                TimeoutErrorSend timeoutErrorSend = new TimeoutErrorSend(SugarApp.getSugarContext(), mTracker, retrofitError, retrofitError.getCause().toString(),
                        errorCode, connectedCountry, connectedNode);
                timeoutErrorSend.timeOutError();
            }
        } else if (r != null && activity != null) {
            try {
                RestError restError = ((RestError) retrofitError.getBodyAs(RestError.class));
                messageRest = restError.getMessage();
                errorCode = restError.getError_code();
            } catch (Exception e) {
                Log.e(TAG, "catch error " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            Log.e("API Error", "Status " + r.getStatus() + " errorCode: " + errorCode + " error message: " + messageRest);
            switch (r.getStatus()) {
                case 400:
                    if (errorCode == Constants.JSON_SCHEMA_ERROR)
                        showErrorDialog(activity, "", context.getString(R.string.invalid_data));
                    else
                        showErrorDialog(activity, context.getString(R.string.wrong_data_sent), context.getString(R.string.error_details, messageRest));
                    break;
                case 403:
                    messageToast = "";
                    if (errorCode == Constants.DEVICE_LIMIT_REACHED)
                        showErrorDialog(activity, context.getString(R.string.device_limit_reached_title), context.getString(R.string.device_limit_reached_text));
                    else
                        ((ApiErrorListener) activity).onError(403, errorCode, messageRest);
                    break;
                case 404:
                    break;
                case 408:
                    Log.e(TAG, "error 408");
                    messageToast = context.getString(R.string.timed_out);
                    TimeoutErrorSend timeoutErrorSend = new TimeoutErrorSend(SugarApp.getSugarContext(), mTracker, retrofitError, messageRest, errorCode, connectedCountry, connectedNode);
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
//                            AnalyticsUtils.sendEventTimedOut(activity, mTracker, label);
//                            Log.e(TAG, "Timeout error send");
//                            TraceErrorSend traceErrorSend = new TraceErrorSend(SugarApp.getSugarContext(), SugarApp.getSugarContext().getString(R.string.trace_error_timeout),
//                                    "Error: "+ errorCode,  messageRest, originalCountry, connectedNode);
//                            traceErrorSend.sentError();
//                        }
//                    }
                    break;
                case 422:
                    showErrorDialog(activity, "", context.getString(R.string.invalid_credentials));
                    break;
                case 500:
                    messageToast = context.getString(R.string.server_error) + " " + context.getString(R.string.error_details, messageRest);
                    break;
                default:
                    messageToast = messageRest;
            }


        }
        if (messageToast != null && !TextUtils.isEmpty(messageToast)) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            final String finalMessage = messageToast;
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(ApplicationClass.getContext(), finalMessage, Toast.LENGTH_LONG).show();
                    Utils.showToast(finalMessage);
                }
            };
            mainHandler.post(myRunnable);
        }
        return retrofitError;
    }


    private void showErrorDialog(final AppCompatActivity activity, final String title, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleDialog errorDialog = SimpleDialog.newInstance(title, message);
                    errorDialog.show(activity.getSupportFragmentManager(), SimpleDialog.class.getSimpleName());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(SugarApp.getSugarContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}