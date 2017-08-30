package com.privatix.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.privatix.MainActivity;
import com.privatix.R;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserPaymentWrapper;
import com.privatix.api.models.answer.subscription.TraceErrorWrapper;
import com.privatix.api.models.request.TraceError;
import com.privatix.api.models.request.UserPayment;
import com.privatix.model.PurchaseTable;
import com.privatix.model.TraceErrorTable;
import com.privatix.services.MyVpnListenerService;
import com.privatix.utils.Constants;
import com.privatix.utils.NotificationUtils;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;

import org.strongswan.android.logic.VpnStateService;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Lotar on 08.04.2016.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    public static final String TAG = NetworkChangeReceiver.class.getSimpleName();
    //SharedPreferences sp;
    private MyVpnListenerService mService;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            checkIfHasNotSavedPurchase(context);
            checkIfHasNotSavedErrorTrace(context);
        }

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // sp = context.getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
        //sp.edit().putBoolean(PrefKeys.IS_START_MAIN_FIRST_TIME, false).apply();

        IBinder iBinder = peekService(context, new Intent(context, MyVpnListenerService.class));
        if (iBinder != null)
            try {
                mService = ((MyVpnListenerService.LocalBinder) iBinder).getService();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }


        SharedPreferences sp = context.getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
        boolean isNeedCheck = sp.getBoolean(PrefKeys.NETWORK_ALERTS, PrefKeys.NETWORK_ALERTS_DEFAULT);
        if (isNeedCheck && (mService == null || mService.getState() != VpnStateService.State.CONNECTED)) {
            if (activeNetInfo != null && activeNetInfo.isConnected()) {
                if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
                    if (wifiConfigurations != null) {
                        for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
                            if (wifiConfiguration.status == WifiConfiguration.Status.CURRENT) {
                                if (Utils.isSecure(wifiConfiguration)) {
                                    NotificationUtils.closeNotification(context, Constants.NOTIFY_ID_RISK);
                                } else {
                                    showNotification(context);
                                }
                                break;
                            }
                        }
                    }
                } else if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    showNotification(context);
                }
            }
        }
    }

    private void showNotification(Context context) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_report_white_18dp)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.notification_text))
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(context.getString(R.string.notification_text)));
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // NOTIFY_ID_RISK allows you to update the notification later on.
        mNotificationManager.notify(Constants.NOTIFY_ID_RISK, mBuilder.build());
    }


    public void checkIfHasNotSavedPurchase(Context context) {
        List<PurchaseTable> purchaseTableList = PurchaseTable.listAll(PurchaseTable.class);
        for (int i = 0; i < purchaseTableList.size(); i++) {
            PurchaseTable currentPurchase = purchaseTableList.get(i);
            sendPurchaseData(context, currentPurchase.getSid(), currentPurchase.getToken());
        }
    }


    public void checkIfHasNotSavedErrorTrace(Context context) {
        List<TraceErrorTable> traceErrorTables = TraceErrorTable.listAll(TraceErrorTable.class);
        for (int i = 0; i < traceErrorTables.size(); i++) {
            TraceErrorTable traceErrorTable = traceErrorTables.get(i);
            sendTraceErrorData(context, traceErrorTable);
        }
    }


    public void sendPurchaseData(final Context context, final String sid, final String token) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                String softwareName = context.getString(R.string.software_name) + Utils.getAppVersion(context);
                UserPayment userPayment = new UserPayment(token);
                ApiClient.getClient(context.getApplicationContext(), null).userPayment(softwareName, sid, userPayment, new CancelableCallback<UserPaymentWrapper>(new Callback<UserPaymentWrapper>() {
                    @Override
                    public void success(UserPaymentWrapper userPaymentWrapper, Response response) {
                        PurchaseTable.deleteAll(PurchaseTable.class);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        error.printStackTrace();
                    }
                }));
            }
        };
        thread.start();
    }


    public void sendTraceErrorData(final Context context, final TraceErrorTable traceErrorTable) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                TraceError traceError = new TraceError(traceErrorTable.getType(), traceErrorTable.getDatetime(), traceErrorTable.getSubscriptionUuid(),
                        traceErrorTable.getError(), traceErrorTable.getErrorTrace(), traceErrorTable.getSourceCountry(), traceErrorTable.getConnectionNode());
                ApiClient.getClient(context.getApplicationContext(), null).traceError(traceErrorTable.getSoftwareName(), traceErrorTable.getSid(), traceError, new CancelableCallback<TraceErrorWrapper>(new Callback<TraceErrorWrapper>() {
                    @Override
                    public void success(TraceErrorWrapper traceErrorWrapper, Response response) {
                        traceErrorTable.delete();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        error.printStackTrace();
                    }
                }));
            }
        };
        thread.start();
    }
}
