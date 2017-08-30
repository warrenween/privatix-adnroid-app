package com.privatix.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.privatix.GetStartedActivity;
import com.privatix.async.LoadCertificatesTask;
import com.privatix.model.ProfileTable;
import com.privatix.model.SubscriptionTable;
import com.privatix.utils.Constants;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.CharonVpnService;

import java.util.List;

/**
 * Created by Lotar on 29.09.2016.
 */

public class StartVpnService extends Service {
    public static final String TAG = StartVpnService.class.getSimpleName();
    VpnProfile vpnProfile;
    List<SubscriptionTable> subscriptionTables;
    SubscriptionTable mCurrentTable;
    SharedPreferences sp;
    private VpnProfileDataSource mDataSource;
    private BroadcastReceiver mNetworkStateCheckListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mDataSource = new VpnProfileDataSource(this);
        mDataSource.open();
        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        if (Utils.isNetworkAvailable(StartVpnService.this))
            startVpn();
        else
            startCheckReceiver();
        return START_STICKY;
    }


    public void startVpnIfNeeded() {
        Log.e(TAG, "startVpnIfNeeded");
        String lastCountry = sp.getString(PrefKeys.STARTUP_COUNTRY, "");
        if (!TextUtils.isEmpty(lastCountry)) {
            subscriptionTables = SubscriptionTable.listAll(SubscriptionTable.class);
            for (SubscriptionTable currentTable : subscriptionTables) {
                if (currentTable.getCountry().equals(lastCountry)) {
                    Log.e(TAG, "found equal");
                    mCurrentTable = currentTable;
                    setAndStartVpn(mCurrentTable);
                    break;
                }
            }
        }
    }


    public void startVpn() {
        sp.edit().putBoolean(PrefKeys.SHOULD_TRY_NEXT_COUNTRY, true).apply();
        startVpnIfNeeded();
        stopCheckReceiver();
    }


    public void startCheckReceiver() {
        stopCheckReceiver();
        mNetworkStateCheckListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive network");
                if (Utils.isNetworkAvailable(StartVpnService.this)) {
                    startVpn();
                }
            }
        };
        this.registerReceiver(
                mNetworkStateCheckListener,
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void stopCheckReceiver() {
        if (mNetworkStateCheckListener != null) {
            try {
                unregisterReceiver(mNetworkStateCheckListener);
                mNetworkStateCheckListener = null;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }


    public void setVpnProfile(SubscriptionTable subscriptionTable) {
        ProfileTable currentProfile;
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        currentProfile = profileTables.get(0);

        vpnProfile.setName(subscriptionTable.getCountry());
        vpnProfile.setVpnType(VpnType.IKEV2_EAP);
        vpnProfile.setGateway(subscriptionTable.getHosts().get(0).getHost());
        vpnProfile.setUsername(currentProfile.getCurrentVpnLogin());
        vpnProfile.setPassword(currentProfile.getCurrentVpnPassword());
        vpnProfile.setCountryCode(subscriptionTable.getCountryCode());
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
            Log.e(TAG, "start Service");

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSelf();
                }
            }, 5000);
        } else {
            Log.e(TAG, "can't start Service");
            Intent startAppIntent = new Intent(this, GetStartedActivity.class);
            startAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startAppIntent.putExtra(Constants.BOOT_START_EXTRA, true);
            startActivity(startAppIntent);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        mDataSource.close();
        stopCheckReceiver();
    }

    public class StartVpn extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
