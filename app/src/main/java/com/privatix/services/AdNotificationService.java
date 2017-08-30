package com.privatix.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.privatix.R;
import com.privatix.model.NotificationTable;
import com.privatix.utils.Constants;
import com.privatix.utils.NotificationUtils;

import org.strongswan.android.logic.VpnStateService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Lotar on 12.04.2016.
 */
public class AdNotificationService extends Service {
    final String LOG_TAG = AdNotificationService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    Timer timerDisconnected, timerFreeConnected;
    ShowNotificationDisconnected showNotificationDisconnected;
    ShowNotificationFreeConnected showNotificationFreeConnected;
    NotificationTable notificationDisconnected, notificationFreeConnected;
    String notificationDisconnectedText, notificationFreeConnectedText;
    private MyVpnListenerService mService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MyVpnListenerService.LocalBinder) service).getService();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        bindService(new Intent(this, MyVpnListenerService.class),
                mServiceConnection, BIND_AUTO_CREATE);

        startNotificationTimers();


    }


    public void startNotificationTimers() {
        cancelDisconnectedTimer();
        cancelFreeConnectedTimer();
        if (mService == null || mService.getState() != VpnStateService.State.CONNECTED) {
            Log.d(LOG_TAG, "not connected");
            startDisconnectedTimer();
        } else {
            Log.d(LOG_TAG, "connected");
            startFreeConnectionTimer();
        }
    }


    public void startDisconnectedTimer() {
        notificationDisconnected = null;
        List<NotificationTable> notificationTableList = null;
        try {
            notificationTableList = NotificationTable.listAll(NotificationTable.class);
        } catch (RuntimeException e){
            e.printStackTrace();
        }
        if (notificationTableList != null){
            for (NotificationTable notificationTable : notificationTableList) {
                Log.d(LOG_TAG, "notif target " + (notificationTable.getTarget()));
                if (getString(R.string.notification_state_disconnected).equals(notificationTable.getTarget())) {
                    notificationDisconnected = notificationTable;
                    Log.d(LOG_TAG, "notif target " + "equals");
                    break;
                }
            }
        }
        if (notificationDisconnected != null) {
            String period = notificationDisconnected.getPeriod();
            String link = notificationDisconnected.getLink();
            notificationDisconnectedText = notificationDisconnected.getText();
            if (period != null && link != null) {
                timerDisconnected = new Timer();
                showNotificationDisconnected = new ShowNotificationDisconnected();
                timerDisconnected.schedule(showNotificationDisconnected, Long.valueOf(period) * 1000, Long.valueOf(period) * 1000);
            }
        }
    }


    public void startFreeConnectionTimer() {
        notificationFreeConnected = null;
        List<NotificationTable> notificationTableList = NotificationTable.listAll(NotificationTable.class);
        for (NotificationTable notificationTable : notificationTableList) {
            Log.d(LOG_TAG, "notif target " + (notificationTable.getTarget()));
            if (getString(R.string.notification_state_free_connected).equals(notificationTable.getTarget())) {
                notificationFreeConnected = notificationTable;
                Log.d(LOG_TAG, "notif target " + (notificationTable.getTarget()));
                break;
            }
        }
        if (notificationFreeConnected != null) {
            String period = notificationFreeConnected.getPeriod();
            String link = notificationFreeConnected.getLink();
            notificationFreeConnectedText = notificationFreeConnected.getText();
            if (period != null && link != null) {
                timerFreeConnected = new Timer();
                showNotificationFreeConnected = new ShowNotificationFreeConnected();
                timerFreeConnected.schedule(showNotificationFreeConnected, Long.valueOf(period) * 1000, Long.valueOf(period) * 1000);
            }
        }
    }


    public void cancelDisconnectedTimer() {
        if (timerDisconnected != null) {
            timerDisconnected.cancel();
            Log.d(LOG_TAG, "timer not null");
        } else {
            Log.d(LOG_TAG, "timer null");
        }
    }


    public void cancelFreeConnectedTimer() {
        if (timerFreeConnected != null) {
            timerFreeConnected.cancel();
            Log.d(LOG_TAG, "timerFreeConnected not null");
        } else {
            Log.d(LOG_TAG, "timerFreeConnected null");
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        cancelDisconnectedTimer();
        cancelFreeConnectedTimer();
    }

    class ShowNotificationDisconnected extends TimerTask {

        @Override
        public void run() {
            // boolean foreground= false;
            if (getApplicationContext() != null) {
//                try {
//                    foreground = new ForegroundCheckTask().execute(getApplicationContext()).get();
//                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                }
//                if (foreground)
                NotificationUtils.showAdNotification(getApplicationContext(), Constants.NOTIFY_ID_DISCONNECTED, notificationDisconnectedText, Constants.NOTIFICATION_SMALL_ICON_REPORT);
//                else {
//                    cancelDisconnectedTimer();
//                    Log.d(TAG, "App Not Running: cancel Disconnected Timer");
//                }
            }
//            else {
//                cancelDisconnectedTimer();
//                Log.d(TAG, "App Not Running: cancel Disconnected Timer");
//            }
        }
    }

    class ShowNotificationFreeConnected extends TimerTask {
        @Override
        public void run() {
            // Use like this:
//            boolean foreground= false;
            if (getApplicationContext() != null) {
//                try {
//                    foreground = new ForegroundCheckTask().execute(getApplicationContext()).get();
//                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                }
//                if (foreground)
                NotificationUtils.showAdNotification(getApplicationContext(), Constants.NOTIFY_ID_FREE_CONNECTED, notificationFreeConnectedText, Constants.NOTIFICATION_SMALL_ICON_START_RATE);
//                else {
//                    cancelFreeConnectedTimer();
//                    Log.d(TAG, "App Not Running: cancel Free connected Timer");
//                }
            }
//            else {
//                cancelFreeConnectedTimer();
//                Log.d(TAG, "App Not Running: cancel Free connected Timer");
//            }

        }
    }

    class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            final Context context = params[0].getApplicationContext();
            return isAppOnForeground(context);
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public AdNotificationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AdNotificationService.this;
        }
    }


}
