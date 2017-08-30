package com.privatix;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserActivationWrapper;
import com.privatix.api.models.answer.UserPaymentWrapper;
import com.privatix.api.models.answer.UserSessionWrapper;
import com.privatix.api.models.answer.subscription.SubscriptionSession;
import com.privatix.api.models.request.UserActivation;
import com.privatix.api.models.request.UserPayment;
import com.privatix.fragments.dialogs.TellWhatThinkDialog;
import com.privatix.model.NotificationTable;
import com.privatix.model.OriginalCountrySpeedCheckerTable;
import com.privatix.model.ProfileTable;
import com.privatix.model.PurchaseTable;
import com.privatix.services.SpeedCheckNoVpnService;
import com.privatix.utils.AlarmUtils;
import com.privatix.utils.Constants;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Helper;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;

import java.util.Calendar;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public abstract class BaseMainActivity extends BaseAnalyticsActivity {
    private static final String TAG = BaseMainActivity.class.getSimpleName();
    public AlertDialog mErrorDialog;
    SharedPreferences sp;

    CancelableCallback<UserSessionWrapper> userSessionCallback;
    CancelableCallback<UserActivationWrapper> userActivationCallback;
    ProgressDialog progressDialog;
    ProfileTable currentProfile;
    String sid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, MODE_PRIVATE);

        versionControl();


        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        if (profileTable.size() > 0) {
            currentProfile = profileTable.get(0);
            sid = currentProfile.getSid();
        } else {
            Utils.showToast("Sorry, something going wrong. Try to update data");
            goToStartScreen();
        }


        startUpdateAlarm();
    }


    public void goToStartScreen() {
        Intent intent = new Intent(this, GetStartedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("");
    }


    public void checkIfNeedReviewDialog() {
        Log.e(TAG, "checkIfNeedReviewDialog");
        //Log.d("ratingDialog", "check");
        long currentData = Calendar.getInstance().getTimeInMillis();
        //Log.d("ratingDialog", "cd: " + currentData);
        long oldData = sp.getLong(PrefKeys.TIME_KEY, 0);
        //Log.d("ratingDialog", "od: " + oldData);
        long timeDifference = currentData - oldData;
        if (timeDifference < Constants.ONE_DAY) {
            //Log.d("ratingDialog", "dif: " + timeDifference);
            return;
        }
        boolean needShow = sp.getBoolean(PrefKeys.NEED_SHOW_KEY, true);
        Log.e(TAG, "need show review dialog");
        if (needShow) {
            //Log.d("ratingDialog", "show");
            TellWhatThinkDialog tellWhatThinkDialog = TellWhatThinkDialog.newInstance();
            try {
                tellWhatThinkDialog.show(getFragmentManager(), TellWhatThinkDialog.class.getSimpleName());
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }


    private void versionControl() {
        int oldVersionCode;
        int versionCode = BuildConfig.VERSION_CODE;
        oldVersionCode = sp.getInt(PrefKeys.VERSION_KEY, 0);
        if (versionCode != oldVersionCode)
            changeVersion(versionCode);
    }

    private void changeVersion(int newVersionCode) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PrefKeys.VERSION_KEY, newVersionCode);
        editor.putInt(PrefKeys.COUNTER_KEY, 0);
        editor.putBoolean(PrefKeys.NEED_SHOW_KEY, true);
        editor.putLong(PrefKeys.TIME_KEY, System.currentTimeMillis());
        editor.apply();
    }


    public void startPremiumScreen() {
        Intent intent = new Intent(BaseMainActivity.this, PremiumActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(BaseMainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userSessionCallback != null)
            userSessionCallback.cancel();
        if (userActivationCallback != null)
            userActivationCallback.cancel();
        Log.d(TAG, "onDestroy");
    }


    public void showErrorDialog(final int textId) {
        Log.d(TAG, "showErrorDialog");
        mErrorDialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.error_introduction) + " " + getString(textId))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        clearError();
                        dialog.dismiss();
//                        Log.d(TAG, "onClick ok");
//                        if(!TextUtils.isEmpty(sid) && textId!= R.string.error_generic)
//                            getSession(sid);
                    }
                }).create();
        mErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "onDismiss");
                if (!TextUtils.isEmpty(sid) && textId != R.string.error_generic)
                    getSession(sid);
                mErrorDialog = null;
            }
        });
        if (!isFinishing()) {
            try {
                mErrorDialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    public void showSimpleErrorDialog(final int textId) {
        mErrorDialog = new AlertDialog.Builder(this)
                .setMessage(getString(textId))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        clearError();
                        dialog.dismiss();
                    }
                }).create();
        mErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "simple onDismiss");
                if (!TextUtils.isEmpty(sid) && textId != R.string.error_generic)
                    getSession(sid);
                mErrorDialog = null;
            }
        });
        if (!isFinishing()) {
            try {
                mErrorDialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    public void showNoConnectionCloseAppDialog(int textId) {
        AlertDialog noConnectionDialog = new AlertDialog.Builder(this)
                .setMessage(getString(textId))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        clearError();
                        dialog.dismiss();
                        finish();

                    }
                }).create();
        if (!isFinishing()) {
            try {
                noConnectionDialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }
    }


    public void hideErrorDialog() {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    public abstract void clearError();

    public void checkSpeedNoVpn(ProfileTable currentProfile) {
        if (!Utils.isMyServiceRunning(this, SpeedCheckNoVpnService.class)) {
            //load list of all original already checked countries
            List<OriginalCountrySpeedCheckerTable> list = OriginalCountrySpeedCheckerTable.listAll(
                    OriginalCountrySpeedCheckerTable.class);

            //get current original country

            //flag to know ig current original country already checked
            boolean needToCheck = true;

            if (list.size() == 0) {
                Log.d("speed_check", "no_vpn: Started check country " + currentProfile.getOriginalCountry());
                startService(new Intent(this, SpeedCheckNoVpnService.class));
            } else {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getOriginalCountry().equals(currentProfile.getOriginalCountry())) {
                        Log.d("speed_check", "no_vpn: " + currentProfile.getOriginalCountry() + " already checked");
                        needToCheck = false;
                    }
                }
                if (needToCheck) {
                    Log.d("speed_check", "no_vpn: Started check country " + currentProfile.getOriginalCountry());
                    startService(new Intent(this, SpeedCheckNoVpnService.class));
                }
            }
        }
    }


    public void checkIfHasNotSavedPurchase() {
        List<PurchaseTable> purchaseTableList = PurchaseTable.listAll(PurchaseTable.class);
        for (int i = 0; i < purchaseTableList.size(); i++) {
            PurchaseTable currentPurchase = purchaseTableList.get(i);
            sendPurchaseData(currentPurchase.getSid(), currentPurchase.getToken());
        }
    }


    public void startUpdateAlarm() {
        AlarmUtils alarmUtils = new AlarmUtils(this);
        alarmUtils.setAlarm(false);
    }

    public void cancelUpdateAlarm() {
        AlarmUtils alarmUtils = new AlarmUtils(this);
        alarmUtils.cancelAlarm();
    }


    public void sendPurchaseData(String sid, String token) {
        UserPayment userPayment = new UserPayment(token);
        Log.d("sid_debug", sid);
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this, mTracker).userPayment(softwareName, sid, userPayment, new CancelableCallback<UserPaymentWrapper>(new Callback<UserPaymentWrapper>() {
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


    public void saveResults(UserSessionWrapper userSessionWrapper) {
        String userLoginVpn = userSessionWrapper.getSubscription().getLogin();
        String userPasswordVpn = userSessionWrapper.getSubscription().getPassword();
        String expiresAt = userSessionWrapper.getSubscription().getQuotes().getExpiresAt();
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTable.get(0);
        DatabaseUtils.saveProfile(currentProfile, currentProfile.isAuthorized(), userSessionWrapper.getIsVerified(), userSessionWrapper.getSubscription().getPlan(),
                userSessionWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, currentProfile.getSid(), userSessionWrapper.getSubscription().getSubscriptionId());
        DatabaseUtils.saveNewSubscriptions(userSessionWrapper.getSubscription().getNodes());
        DatabaseUtils.saveNewNotifications(userSessionWrapper.getNotifications());
    }


    public void saveActivationResults(UserActivationWrapper userActivationWrapper) {
        sid = userActivationWrapper.getSid();
        String userLoginVpn = userActivationWrapper.getSubscription().getLogin();
        String userPasswordVpn = userActivationWrapper.getSubscription().getPassword();
        String expiresAt = userActivationWrapper.getSubscription().getQuotes().getExpiresAt();
        NotificationTable.deleteAll(NotificationTable.class);
        ProfileTable.deleteAll(ProfileTable.class);
        DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(),
                userLoginVpn, userPasswordVpn, expiresAt, sid, userActivationWrapper.getSubscription().getSubscriptionId());
        DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
    }


    private void showProgressDialog() {
        try {
            progressDialog = ProgressDialog.show(this, null,
                    "Try to restore data...");
            progressDialog.setCancelable(false);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void dismissProgressDialog() {
        if (!isFinishing() && progressDialog != null)
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

    }


    public void getSession(String sid) {
        Log.d(TAG, "getSession");
        if (!Utils.isNetworkAvailable(this)) {
            return;
        }
        showProgressDialog();
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this, mTracker).getUserSession(softwareName, sid, userSessionCallback = new CancelableCallback<>(new Callback<UserSessionWrapper>() {

            @Override
            public void success(UserSessionWrapper userSessionWrapper, Response response) {
                Log.d(TAG, "getSession success");
                new SaveResults().execute(userSessionWrapper);
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                processError(error.getResponse());
                //Utils.showToast("Restore data failed");
            }


        }));
    }

    public void processError(Response response) {
        if (response != null && response.getStatus() != 408 && (response.getStatus() != 502 || response.getStatus() != 504)) {
            Log.d("Error code", response.getStatus() + " status");
            userActivation();
        } else {
            dismissProgressDialog();
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
                NotificationTable.deleteAll(NotificationTable.class);
                ProfileTable.deleteAll(ProfileTable.class);
                DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(),
                        userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
                dismissProgressDialog();
            }


            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                dismissProgressDialog();
            }
        }));
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
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "onPostExecute");
            dismissProgressDialog();
        }
    }
}
