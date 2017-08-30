package com.privatix.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.R;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserActivationWrapper;
import com.privatix.api.models.answer.UserSessionWrapper;
import com.privatix.api.models.answer.subscription.SubscriptionSession;
import com.privatix.api.models.request.UserActivation;
import com.privatix.model.Hosts;
import com.privatix.model.ProfileTable;
import com.privatix.model.SubscriptionTable;
import com.privatix.utils.AlarmUtils;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Helper;
import com.privatix.utils.Utils;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.VpnStateService;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UpdateService extends Service {
    public static final String TAG = UpdateService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    public Tracker mTracker;
    String sid;
    VpnProfile vpnProfile;
    CancelableCallback<UserSessionWrapper> userSessionCallback;
    CancelableCallback<UserActivationWrapper> userActivationCallback;
    private VpnProfileDataSource mDataSource;
    private MyVpnListenerService myVpnListenerService;
    private final ServiceConnection myVpnListenerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            myVpnListenerService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "myVpnListenerServiceConnection connected");
            try {
                myVpnListenerService = ((MyVpnListenerService.LocalBinder) service).getService();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initTracker();
        Log.d(TAG, "onCreate");
    }


    public void initTracker() {
        ApplicationClass application = (ApplicationClass) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        if (profileTable.size() == 0) {
            finishWork();
            return START_NOT_STICKY;
        }
        ProfileTable currentProfile = profileTable.get(0);
        sid = currentProfile.getSid();

        if (!TextUtils.isEmpty(sid)) {
            bindService(new Intent(this, MyVpnListenerService.class),
                    myVpnListenerServiceConnection, 0);
            getSession();
        } else
            finishWork();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userSessionCallback != null)
            userSessionCallback.cancel();
        if (userActivationCallback != null)
            userActivationCallback.cancel();
        if (myVpnListenerService != null) {
            this.unbindService(myVpnListenerServiceConnection);
        }
        if (mDataSource != null)
            mDataSource.close();
    }

    public void getSession() {
        Log.d(TAG, "getSession");
        if (!Utils.isNetworkAvailable(this)) {
            finishWork();
            return;
        }
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this, mTracker).getUserSession(softwareName, sid, userSessionCallback = new CancelableCallback<>(new Callback<UserSessionWrapper>() {

            @Override
            public void success(UserSessionWrapper userSessionWrapper, Response response) {
                Log.d(TAG, "getSession success");
                new SaveResults().execute(userSessionWrapper);
            }

            @Override
            public void failure(RetrofitError error) {
                processError(error.getResponse());
                error.printStackTrace();
            }


        }));
    }


    public void processError(Response response) {
        if (response != null && response.getStatus() != 408 && (response.getStatus() != 502 || response.getStatus() != 504)) {
            Log.d("Error code", response.getStatus() + " status");
            userActivation();
        } else {
            finishWork();
        }
    }


    public void userActivation() {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        final UserActivation userActivation = Helper.getUserActivationData(this);
        ApiClient.getClient(this, mTracker).userActivation(softwareName, userActivation, userActivationCallback = new CancelableCallback<>(new Callback<UserActivationWrapper>() {

            @Override
            public void success(UserActivationWrapper userActivationWrapper, Response response) {
                sid = userActivationWrapper.getSid();
                SubscriptionSession subscriptionSession = userActivationWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String expiresAt = subscriptionSession.getQuotes().getExpiresAt();

                ProfileTable.deleteAll(ProfileTable.class);
                DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(),
                        userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
            }


            @Override
            public void failure(RetrofitError error) {
                finishWork();
                error.printStackTrace();
            }
        }));
    }

    public void restartAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlarmUtils alarmUtils = new AlarmUtils(this);
            alarmUtils.setAlarm(true);
        }
    }

    public void finishWork() {
        restartAlarm();
        stopSelf();
    }


    public void saveActivationResults(UserActivationWrapper userActivationWrapper) {
        sid = userActivationWrapper.getSid();
        SubscriptionSession subscriptionSession = userActivationWrapper.getSubscription();
        String userLoginVpn = subscriptionSession.getLogin();
        String userPasswordVpn = subscriptionSession.getPassword();
        String expiresAt = subscriptionSession.getQuotes().getExpiresAt();

        ProfileTable.deleteAll(ProfileTable.class);
        DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(),
                userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
        DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
    }


    public void saveResults(UserSessionWrapper userSessionWrapper) {
        SubscriptionSession subscriptionSession = userSessionWrapper.getSubscription();
        String userLoginVpn = subscriptionSession.getLogin();
        String userPasswordVpn = subscriptionSession.getPassword();
        String expiresAt = subscriptionSession.getQuotes().getExpiresAt();
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        if (profileTables.size() > 0) {
            ProfileTable currentProfile = profileTables.get(0);
            DatabaseUtils.saveProfile(currentProfile, currentProfile.isAuthorized(), userSessionWrapper.getIsVerified(), userSessionWrapper.getSubscription().getPlan(),
                    userSessionWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
        }
//        SubscriptionTable.deleteAll(SubscriptionTable.class);
//        for (int i = 0; i < userSessionWrapper.getSubscription().getNodes().size(); i++) {
//            Nodes node = userSessionWrapper.getSubscription().getNodes().get(i);
//            SubscriptionTable subscriptionTable = new SubscriptionTable(node.getCountry(), node.getCountryCode(), node.getFree());
//            subscriptionTable.save();
//            for (int j = 0; j < node.getHosts().size(); j++) {
//                Log.d(TAG, "host "+ node.getHosts().get(j).getHost());
//                if(node.getHosts().get(j).getHost().equals("gb-node1.privatix.net"))
//                    continue;
//                Hosts host = new Hosts(node.getHosts().get(j).getHost(), subscriptionTable);
//                host.save();
//                Log.d(TAG, "host save "+ node.getHosts().get(j).getHost());
//            }
//        }
        DatabaseUtils.saveNewSubscriptions(userSessionWrapper.getSubscription().getNodes());
        DatabaseUtils.saveNewNotifications(userSessionWrapper.getNotifications());
    }


    public void checkCurrentVpn() {
        boolean hostFound = false;
        if (myVpnListenerService != null && myVpnListenerService.getState() == VpnStateService.State.CONNECTED) {
            VpnProfile currentProfile = myVpnListenerService.getProfile();
            String currentGateway = currentProfile.getGateway();
            String currentCountryCode = currentProfile.getCountryCode();
            List<SubscriptionTable> countriesList = SubscriptionTable.find(SubscriptionTable.class,
                    "country_code = ?", currentCountryCode);
            if (countriesList.size() > 0) {
                SubscriptionTable currentCountry = countriesList.get(0);
                List<Hosts> hosts = currentCountry.getHosts();
                for (Hosts host : hosts) {
                    if (currentGateway.equals(host.getHost())) {
                        hostFound = true;
                        break;
                    }
                }
                if (!hostFound && hosts.size() > 0)
                    setAndStartVpn(currentCountry, hosts.get(0).getHost());
                else {
                    Log.d(TAG, "host not found or new host size = 0");
                }

            }
        } else if (myVpnListenerService != null) {
            Log.e(TAG, "myVpnListenerService state" + myVpnListenerService.getState());
        } else {
            Log.e(TAG, "myVpnListenerService state null");
        }
    }


    public void setAndStartVpn(SubscriptionTable subscriptionTable, String host) {
        mDataSource = new VpnProfileDataSource(this);
        mDataSource.open();
        vpnProfile = mDataSource.getVpnProfile(1);
        if (vpnProfile == null) {
            vpnProfile = new VpnProfile();
            setVpnProfile(subscriptionTable, host);
            mDataSource.insertProfile(vpnProfile);
        } else {
            setVpnProfile(subscriptionTable, host);
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


    public void setVpnProfile(SubscriptionTable subscriptionTable, String host) {
        ProfileTable currentProfile;
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        currentProfile = profileTables.get(0);

        vpnProfile.setName(subscriptionTable.getCountry());
        vpnProfile.setVpnType(VpnType.IKEV2_EAP);
        vpnProfile.setGateway(host);
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
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
            return;
        }
        /* store profile info until the user grants us permission */
        if (intent == null) {
            Intent intentCharonVpn = new Intent(this, CharonVpnService.class);
            intentCharonVpn.putExtras(profileInfo);
            startService(intentCharonVpn);
        }
    }


    public class SaveResults extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            if (params[0] instanceof UserSessionWrapper) {
                UserSessionWrapper userSessionWrapper = (UserSessionWrapper) params[0];
                saveResults(userSessionWrapper);
            } else if (params[0] instanceof UserActivationWrapper) {
                UserActivationWrapper userActivationWrapper = (UserActivationWrapper) params[0];
                saveActivationResults(userActivationWrapper);
            }
            checkCurrentVpn();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "onPostExecute");
            finishWork();
        }
    }


//    public class SaveResults extends AsyncTask<UserSessionWrapper, Void, Void> {
//
//        @Override
//        protected Void doInBackground(UserSessionWrapper... params) {
//            UserSessionWrapper userSessionWrapper = params[0];
//            saveResults(userSessionWrapper);
//            checkCurrentVpn();
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void result) {
//            Log.d(TAG, "onPostExecute");
//            finishWork();
//        }
//    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public UpdateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return UpdateService.this;
        }
    }
}
