package com.privatix;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.privatix.fragments.SettingsLoggedFragment;
import com.privatix.fragments.SettingsNotLoggedFragment;
import com.privatix.fragments.dialogs.RatingDialog;
import com.privatix.model.ProfileTable;
import com.privatix.receivers.BootReceiver;
import com.privatix.services.MyVpnListenerService;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.privatix.utils.interfaces.MyVpnStateListener;
import com.privatix.utils.interfaces.OnFragmentInteractionListener;

import org.strongswan.android.logic.VpnStateService;

import java.util.List;

public class SettingsActivity extends BaseAnalyticsActivity implements OnFragmentInteractionListener, MyVpnStateListener {
    public MyVpnListenerService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MyVpnListenerService.LocalBinder) service).getService();
            mService.registerListener(SettingsActivity.this);
        }
    };
    Boolean isVerified, isAuthorized;
    int exitCount = 2;
    SharedPreferences sp;

    @Override
    protected void onStart() {
        super.onStart();

        //GA - opened settings screen
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "settings");

        AnalyticsUtils.sendEventSettings(this, mTracker);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Utils.isTabletMoreThanSevenInches(this)) {
            exitCount = 3;
        }
        bindService(new Intent(this, MyVpnListenerService.class),
                mServiceConnection, 0);
        setContentView(R.layout.activity_settings);
        try {
            Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(myToolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }

        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, MODE_PRIVATE);

        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTable.get(0);
        isAuthorized = currentProfile.isAuthorized();
        isVerified = currentProfile.isVerified();
        if (isAuthorized) {
            //replaceFragment(SettingsLoggedFragment.newInstance(), true);
            navigateToFragment(SettingsLoggedFragment.newInstance(), true);
        } else {
            //replaceFragment(SettingsNotLoggedFragment.newInstance(), true);
            navigateToFragment(SettingsNotLoggedFragment.newInstance(), true);
        }
    }


    public void showRatingDialog() {
        List<ProfileTable> profileTableList = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTableList.get(0);
        String originalCountry = currentProfile.getOriginalCountry();
        RatingDialog ratingDialog = RatingDialog.newInstance(originalCountry);
        ratingDialog.show(getFragmentManager(), RatingDialog.class.getSimpleName());
    }


    @SuppressLint("CommitTransaction")
    @Override
    public void navigateToFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction()
                .replace(R.id.container, fragment);
        if (addToBackStack) {
            ft.addToBackStack(fragment.getClass().getSimpleName());
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }


    public void disconnectVpn() {
        if (mService != null && (mService.getState() == VpnStateService.State.CONNECTED || mService.getState() == VpnStateService.State.CONNECTING)) {
            mService.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        Log.d("back stack", count + "");
        if (count < 2) {
            finish();
        } else {
            getFragmentManager().popBackStack();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            this.unbindService(mServiceConnection);
        }
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mTracker != null) mTracker = null;
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_done) {
            //Toast.makeText(this, "Action done", Toast.LENGTH_LONG).show();
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void changeNetworkAlertPrefs(boolean isNetworkAlertEnable) {
        sp.edit().putBoolean(PrefKeys.NETWORK_ALERTS, isNetworkAlertEnable).apply();
    }

    public void changeStartUpPrefs(boolean isConnectOnStartupEnable) {
        sp.edit().putBoolean(PrefKeys.CONNECT_ON_STARTUP, isConnectOnStartupEnable).apply();
        changeReceiverState(isConnectOnStartupEnable);

    }


    public void changeReceiverState(boolean isConnectOnStartupEnable) {
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        int state;
        if (isConnectOnStartupEnable)
            state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        else
            state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        getPackageManager().setComponentEnabledSetting(receiver,
                state,
                PackageManager.DONT_KILL_APP);
    }


    public void startWebActivity(int resId) {
        Intent intentGetHelp = new Intent(SettingsActivity.this, WebViewActivity.class);
        intentGetHelp.putExtra("url", getString(resId));
        startActivity(intentGetHelp);
    }


    public void startGetHelpPage() {
        startWebActivity(R.string.url_get_help);
    }

    public void startTosPage() {
        startWebActivity(R.string.url_tos);
    }


    public void startPrivacyPolicyPage() {
        startWebActivity(R.string.url_privacy_policy);
    }

    public void startImprintPage() {
        startWebActivity(R.string.url_imprint);
    }


    public void share() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_1) + System.getProperty("line.separator") +
                getString(R.string.share_text_2));
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    @Override
    public void stateChanged() {
        Log.e("Settings activity", "state changed");
        if (mService != null && mService.getState() == VpnStateService.State.DISABLED) {
            Fragment currentFragment = getFragmentManager().findFragmentById(R.id.container);
            if (currentFragment instanceof SettingsLoggedFragment) {
                Log.e("Settings activity", "should log out");
                ((SettingsLoggedFragment) currentFragment).logOutFinish();
            }
        }

    }
}
