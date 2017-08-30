package com.privatix.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.R;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.async.TraceErrorSend;
import com.privatix.model.Hosts;
import com.privatix.model.ProfileTable;
import com.privatix.model.SpeedCheckerCountyTable;
import com.privatix.model.SubscriptionTable;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.Constants;
import com.privatix.utils.NotificationUtils;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.privatix.utils.interfaces.MyVpnStateListener;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.VpnStateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Lotar on 12.04.2016.
 */
public class MyVpnListenerService extends VpnStateService {
    final String TAG = MyVpnListenerService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private final List<MyVpnStateListener> mListeners = new ArrayList<>();
    public Tracker mTracker;
    Handler handler;
    String lastCountry = "", currentCountryCode;
    String originalCountry = "";
    Map<String, Boolean> usedGateway = new HashMap<>();
    SubscriptionTable mCurrentSubscriptionTable;
    VpnProfile vpnProfile;
    Runnable runnable;
    NotificationCompat.Builder statusNotificationBuilder;
    String currentHost;
    boolean shouldStartReconnect = true;
    SharedPreferences sp;
    long datetime;
    private VpnProfileDataSource mDataSource;
    private SpeedCheckVpnService mSpeedCheckService;
    private final ServiceConnection mSpeedCheckVpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSpeedCheckService = ((SpeedCheckVpnService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSpeedCheckService = null;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, MODE_PRIVATE);
        Log.d(TAG, "onCreate");
        initTracker();
        bindService(new Intent(this, SpeedCheckVpnService.class),
                mSpeedCheckVpnServiceConnection, Service.BIND_AUTO_CREATE);
    }


    public void initTracker() {
        ApplicationClass application = (ApplicationClass) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    public void startReconnectTimer() {
        cancelReconnectTimer();
        Log.e(TAG, "startReconnectTimer");
        mDataSource = new VpnProfileDataSource(this);
        mDataSource.open();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                reconnect();
            }
        };
        handler.postDelayed(runnable, Constants.MAX_TIME_FOR_CONNECTING);
    }

    public void cancelReconnectTimer() {
        if (handler != null) {
            handler.removeCallbacks(runnable);
            Log.d(TAG, "remove callbacks");
        }
        if (mDataSource != null)
            mDataSource.close();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        cancelReconnectTimer();
        if (mDataSource != null)
            mDataSource.close();
        if (mSpeedCheckService != null)
            this.unbindService(mSpeedCheckVpnServiceConnection);
    }


    public void stopAndShowError() {
        shouldStartReconnect = false;
        notifyListeners();
        disconnect();
        NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_VPN_STATUS);
        stopForeground(true);
    }

    public void sendErrorLogs(String error, String originalCountry, String node) {
        boolean isNeedSendLogs = sp.getBoolean(PrefKeys.SEND_ERROR_LOGS, PrefKeys.SEND_ERROR_LOGS_DEFAULT);
        if (isNeedSendLogs) {
            TraceErrorSend traceErrorSend = new TraceErrorSend(this, getString(R.string.trace_error_connect), error, originalCountry, node);
            traceErrorSend.sentError();
        }
    }

    @Override
    public void handleChangeState() {
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        if (profileTables.size() > 0) {
            ProfileTable currentProfile = profileTables.get(0);
            originalCountry = currentProfile.getOriginalCountry();
        }

        VpnStateService.State state = getState();
        Log.d(TAG, "handleChangeState: " + state.name());
        VpnStateService.ErrorState error = getErrorState();
        Log.d(TAG, "handleChangeState: " + error.name());
        VpnProfile profile = getProfile();
        if (profile != null) {
            lastCountry = profile.getName();
            currentHost = profile.getGateway();
            currentCountryCode = profile.getCountryCode();
            Log.d(TAG, "Current country " + lastCountry);
            Log.d(TAG, "Current host " + currentHost);
        }

        if (error != VpnStateService.ErrorState.NO_ERROR) {
            Log.e(TAG, "error: " + error.name());

            if (error == ErrorState.GENERIC_ERROR) {
                stopAndShowError();
                return;
            }

            if (state == State.CONNECTING) {
                sendErrorLogs(error.name(), originalCountry, currentHost);
                if (error != ErrorState.LOOKUP_FAILED) {
                    AnalyticsUtils.sendEventConnectionFailed(this, mTracker, generateLabel());
                }
            }


            if (state != VpnStateService.State.DISABLED && shouldStartReconnect) {
                startReconnectTimer();
            } else {
                stopAndShowError();
                return;
            }
            shouldStartReconnect = true;
            return;
        }
        checkMail("");
        notifyListeners();
        switch (state) {
            case DISABLED:
                NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_VPN_STATUS);
                sp.edit().putString(PrefKeys.STARTUP_COUNTRY, "").apply();
                stopForeground(true);
                stopCheckVpn();
                break;
            case CONNECTING:
                showStatusNotification("Authenticating...");
                shouldStartReconnect = true;
                break;
            case CONNECTED:
                showStatusNotification("You're connected to Privatix!");
                usedGateway.clear();
                cancelReconnectTimer();
                cancelNotifications();
                sp.edit().putString(PrefKeys.LAST_CONNECTION_COUNTRY, profile.getName()).apply();
                sp.edit().putString(PrefKeys.STARTUP_COUNTRY, profile.getName()).apply();
                sp.edit().putBoolean(PrefKeys.SHOULD_TRY_NEXT_COUNTRY, false).apply();
                checkSpeedVpn(currentHost, currentCountryCode);
                break;
            case DISCONNECTING:
                break;
            case SERVER_DOWN:
                if (Utils.isNetworkAvailable(this)) {
                    NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_VPN_STATUS);
                    showStatusNotification("Authenticating...");
                    Log.e(TAG, "server_down: start reconnect timer, with " + Constants.MAX_TIME_FOR_CONNECTING + " delay");
                    startReconnectTimer();
                }
                break;
        }
    }


    public String generateLabel() {
        if (!TextUtils.isEmpty(originalCountry))
            return originalCountry + "_" + lastCountry + "_" + currentHost;
        return null;
    }

    public void cancelNotifications() {
        NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_CONNECTION_LOST);
        NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_RISK);
        NotificationUtils.closeNotification(this, Constants.NOTIFY_ID_DISCONNECTED);
    }


    public void registerListener(MyVpnStateListener myVpnStateListener) {
        Log.d(TAG, "registerListener");
        mListeners.add(myVpnStateListener);
    }


    public void unregisterListener(MyVpnStateListener myVpnStateListener) {
        Log.d(TAG, "unregisterListener");
        mListeners.remove(myVpnStateListener);
    }


    public void showStatusNotification(String status) {
        if (statusNotificationBuilder != null)
            showVpnStatusNotification(this, statusNotificationBuilder, status, R.drawable.ic_shield_status_bar);
        else {
            statusNotificationBuilder = new NotificationCompat.Builder(this);
            showVpnStatusNotification(this, statusNotificationBuilder, status, R.drawable.ic_shield_status_bar);
        }
    }


    public void setAndStartVpn(SubscriptionTable subscriptionTable) {
        vpnProfile = mDataSource.getVpnProfile(1);
        if (vpnProfile == null) {
            vpnProfile = new VpnProfile();
            setVpnProfile(subscriptionTable);
            mDataSource.insertProfile(vpnProfile);
        } else {
            setVpnProfile(subscriptionTable);
            mDataSource.updateVpnProfile(vpnProfile);
        }

        Log.d(TAG, "vpn profile id" + vpnProfile.getId() + "");
        Log.d(TAG, "vpn profile name " + vpnProfile.getName() + "name");
        Log.d(TAG, "vpn profile userName " + vpnProfile.getUsername() + " my");
        Log.d(TAG, "vpn profile password " + vpnProfile.getPassword() + " pass");
        Log.d(TAG, "vpn profile gateway " + vpnProfile.getGateway() + " gateway");
        Log.d(TAG, "vpn profile country code " + vpnProfile.getCountryCode() + " country code");

        Bundle profileInfo = new Bundle();
        profileInfo.putLong(VpnProfileDataSource.KEY_ID, vpnProfile.getId());
        profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, vpnProfile.getUsername());
        profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, vpnProfile.getPassword());
        startVpnProfile(profileInfo);
    }


    public void setVpnProfile(SubscriptionTable subscriptionTable) {
        ProfileTable currentProfile;
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        currentProfile = profileTables.get(0);

        vpnProfile.setName(subscriptionTable.getCountry());
        vpnProfile.setVpnType(VpnType.IKEV2_EAP);
        vpnProfile.setGateway(currentHost);
        vpnProfile.setUsername(currentProfile.getCurrentVpnLogin());
        vpnProfile.setPassword(currentProfile.getCurrentVpnPassword());
        vpnProfile.setCountryCode(subscriptionTable.getCountryCode());
    }


    private void startVpnProfile(Bundle profileInfo) {
        prepareVpnService(profileInfo);
    }


    /**
     * Prepare the VpnService. If this succeeds the current VPN profile is
     * started.
     *
     * @param profileInfo a bundle containing the information about the profile to be started
     */
    protected void prepareVpnService(Bundle profileInfo) {
        Intent intent;
        //FIX: NOT REINSTALL - ANDROID ASK PERMISSION BUG
        try {
            intent = VpnService.prepare(this);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
            return;
        }
        /* store profile info until the user grants us permission */
        if (intent == null) {	/* user already granted permission to use VpnService */
            Intent intentCharonVpn = new Intent(this, CharonVpnService.class);
            intentCharonVpn.putExtras(profileInfo);
            startService(intentCharonVpn);
        }
    }


    public void notifyListeners() {
        ///Log.d(TAG, "called");
        for (MyVpnStateListener listener : mListeners) {
            listener.stateChanged();
        }
    }


    public void reconnect() {
        mCurrentSubscriptionTable = null;
        //We saved only 1 copy of each country so result will be with one record
        Log.d(TAG, "last country reconnect " + lastCountry);
        List<SubscriptionTable> countriesList = SubscriptionTable.find(SubscriptionTable.class,
                "country = ?", lastCountry);
        if (countriesList.size() > 0) {
            SubscriptionTable currentCountry = countriesList.get(0);
            //Get list of all hosts for current country
            List<Hosts> hosts = currentCountry.getHosts();
            Log.d(TAG, "current host reconnect: " + currentHost);
            usedGateway.put(currentHost, true);
            for (int i = 0; i < hosts.size(); i++) {
                Log.d(TAG, "try host to use: " + hosts.get(i).getHost());
                if (usedGateway.get(hosts.get(i).getHost()) == null) {
                    usedGateway.put(hosts.get(i).getHost(), true);
                    currentHost = hosts.get(i).getHost();
                    mCurrentSubscriptionTable = currentCountry;
                    Log.d(TAG, "found another one " + hosts.get(i).getHost());
                    break;
                }
            }
        }

        if (mCurrentSubscriptionTable == null) {
            boolean shouldStartNextCountry = sp.getBoolean(PrefKeys.SHOULD_TRY_NEXT_COUNTRY, false);
            Log.e(TAG, "shouldStartNextCountry " + shouldStartNextCountry);
            if (!shouldStartNextCountry) {
                Log.d(TAG, "mCurrentSubscriptionTable null ");
                NotificationUtils.showNotification(getApplicationContext(), getString(R.string.vpn_connection_lost));
                shouldStartReconnect = false;
                usedGateway.clear();
                setState(State.DISABLED);
                //handleChangeState();
            } else {
                sp.edit().putBoolean(PrefKeys.SHOULD_TRY_NEXT_COUNTRY, false).apply();
                tryAnotherCountry();
            }
        } else {
            setAndStartVpn(mCurrentSubscriptionTable);
        }
    }


    public void tryAnotherCountry() {
        usedGateway.clear();
        List<SubscriptionTable> countriesList = SubscriptionTable.listAll(SubscriptionTable.class);
        for (SubscriptionTable currentSubscriptionTable : countriesList) {
            if (!currentSubscriptionTable.getCountry().equals(lastCountry)) {
                lastCountry = currentSubscriptionTable.getCountry();
                setAndStartVpn(currentSubscriptionTable);
                break;
            }
        }
    }


    public void showVpnStatusNotification(Context context, NotificationCompat.Builder builder,
                                          String text, int smallIcon) {
        android.app.Notification notification = NotificationUtils.getVpnStatusNotification(context, builder, text, smallIcon);
        startForeground(Constants.NOTIFY_ID_VPN_STATUS, notification);
    }


    /**
     * This method used only for make first request after vpn disconnect.
     * Because first request usually failed. Reason of failed hard to understand.
     * It isn't mean anything. We doesn't care about result of this request.
     */
    public void checkMail(String login) {
        if (!Utils.isNetworkAvailable(this))
            return;
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        if (profileTables.size() == 0)
            return;
        ProfileTable currentProfile = profileTables.get(0);
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserCheckMail userCheckMail = new UserCheckMail(login);
        ApiClient.getClient(this, null).userCheckMail(softwareName, currentProfile.getSid(), userCheckMail, new CancelableCallback<UserCheckMailWrapper>(new Callback<UserCheckMailWrapper>() {

            @Override
            public void success(UserCheckMailWrapper userCheckMailWrapper, Response response) {
                Log.d(TAG, "success");
            }

            @Override
            public void failure(RetrofitError error) {
            }
        }));
    }

    public void stopCheckVpn() {
        if (mSpeedCheckService != null) {
            mSpeedCheckService.stopSpeedCheck();
        }
    }

    public void checkSpeedVpn(String currentHostVpn, String currentCountryCode) {
        if (mSpeedCheckService != null) {
            Log.d("speed_check", "vpn: start check module");
            List<SpeedCheckerCountyTable> checkerCountyTables =
                    SpeedCheckerCountyTable.listAll(SpeedCheckerCountyTable.class);

            List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);

            String currentCountrySet = profileTables.get(0).getOriginalCountry() + "_" +
                    currentCountryCode;

            Log.d("speed_check", "vpn: countrySet: " + currentCountrySet);

            boolean needToCheck = true;

            for (int i = 0; i < checkerCountyTables.size(); i++) {
                if (checkerCountyTables.get(i).getCountryCode()
                        .equals(currentCountrySet)) {
                    Log.d("speed_check", "vpn: countrySet: " + currentCountrySet + " already checked");
                    needToCheck = false;
                }
            }
            if (needToCheck) {
                mSpeedCheckService.startSpeedCheck(currentHostVpn, profileTables.get(0).getOriginalCountry(), currentCountryCode);
                Log.d("speed_check", "vpn: countrySet for check: " + currentCountrySet);
            }
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MyVpnListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyVpnListenerService.this;
        }
    }

}
