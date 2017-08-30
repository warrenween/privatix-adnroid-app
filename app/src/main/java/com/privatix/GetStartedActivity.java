package com.privatix;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.privatix.api.ApiClient;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserActivationWrapper;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.answer.UserSessionWrapper;
import com.privatix.api.models.answer.subscription.SubscriptionSession;
import com.privatix.api.models.request.UserActivation;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.async.DownloadUpdateFile;
import com.privatix.model.NotificationTable;
import com.privatix.model.ProfileTable;
import com.privatix.services.ExitCheckerService;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.CertificateUtils;
import com.privatix.utils.Constants;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Helper;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GetStartedActivity extends BaseAnalyticsActivity implements ApiErrorListener, View.OnClickListener {
    public static final String TAG = GetStartedActivity.class.getSimpleName();
    Button btn_get_protected;
    String sid = "";
    TextView tv_login;
    SharedPreferences sp;
    Boolean started;
    CancelableCallback<UserActivationWrapper> userActivationCallback;
    CancelableCallback<UserSessionWrapper> userSessionCallback;
    Boolean isWasLogout;
    int countOfTrying = 0;
    boolean bootStart;
    //private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
//        initFlurry();
//        if (!Utils.isTabletMoreThanSevenInches(this)) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
        super.onCreate(savedInstanceState);
//
//        ApplicationClass application = (ApplicationClass) getApplication();
//        mTracker = application.getDefaultTracker();
        //initFlurry();


        Intent intent = getIntent();
        if (intent != null) {
            bootStart = intent.getBooleanExtra(Constants.BOOT_START_EXTRA, false);
        }
        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
        checkStartAnalytics();
        isWasLogout = sp.getBoolean(PrefKeys.IS_WAS_LOGOUT, false);
        started = sp.getBoolean(PrefKeys.STARTED, false);
        if (!sp.getBoolean(PrefKeys.CA_IMPORTED, false)) {
            CertificateUtils.importCertificate(this, sp);
        }

        if (isWasLogout) {
            setContentView(R.layout.activity_get_started_empty);
            checkMail("");
            userActivation(true);
            return;
        }


        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        if (profileTable.size() == 0 || !started) {
            setContentView(R.layout.activity_get_started);
            userActivation(false);
            initView();
        } else {
            ProfileTable currentProfile = profileTable.get(0);
            sid = currentProfile.getSid();
            setContentView(R.layout.activity_get_started_empty);
            getSession();
        }

        String stringUrl = getString(R.string.download_apk_link) + getString(R.string.download_apk_update_file_name);
        new DownloadUpdateFile(this).execute(stringUrl);
    }


//    public void initFlurry() {
//        new FlurryAgent.Builder()
//                .withListener(this)
//                .withLogEnabled(true)
//                .withLogLevel(Log.INFO)
//                .build(this, getString(R.string.flurry_api_key));
//    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "start_screen");

        AnalyticsUtils.sendEventAppOpen(this, sp, mTracker);
        startAppStatusCheckerService();
        AnalyticsUtils.sendEventCheckForCompetitors(this, sp, mTracker);
        AnalyticsUtils.checkForNotSentEvents(this, mTracker);
    }

    public void initView() {
        btn_get_protected = (Button) findViewById(R.id.btn_get_protected);
        tv_login = (TextView) findViewById(R.id.tv_login);

        btn_get_protected.setOnClickListener(this);
        tv_login.setOnClickListener(this);
    }


    public void startMainActivity() {
        if (!sid.equals("")) {
            AnalyticsUtils.sendEventAppInstall(GetStartedActivity.this, sp, mTracker);
            sp.edit().putBoolean(PrefKeys.STARTED, true).apply();
            Intent intent = new Intent(GetStartedActivity.this, MainActivity.class);
            intent.putExtra(Constants.BOOT_START_EXTRA, bootStart);
            startActivity(intent);
            finish();
        } else {
            userActivation(true);
            //Utils.showToast(R.string.check_your_network);
        }
    }


    public void startLoginActivity() {
        if (!sid.equals("")) {
            Intent intent = new Intent(GetStartedActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            userActivation(false);
            // Utils.showToast(R.string.cannot_login);
        }
    }


    public void setProtectedButton(boolean isEnabled) {
        if (btn_get_protected != null)
            btn_get_protected.setEnabled(isEnabled);
    }


    /**
     * This method used only for make first request after vpn disconnect.
     * Because first request usually failed. Reason of failed hard to understand.
     * It isn't mean anything. We doesn't care about result of this request.
     */
    public void checkMail(String login) {
        if (!Utils.isNetworkAvailable(this))
            return;
        if (TextUtils.isEmpty(sid))
            return;
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserCheckMail userCheckMail = new UserCheckMail(login);
        ApiClient.getClient(this, null).userCheckMail(softwareName, sid, userCheckMail, new CancelableCallback<UserCheckMailWrapper>(new Callback<UserCheckMailWrapper>() {

            @Override
            public void success(UserCheckMailWrapper userCheckMailWrapper, Response response) {
                Log.d(TAG, "success");
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
            }
        }));
    }


    public void userActivation(final boolean isNeedStartNextActivity) {
        if (!Utils.isNetworkAvailable(this)) {
            if (isWasLogout)
                startActivityWithOutInternet();
            else
                Utils.showToast(R.string.check_your_network);
            return;
        }
        setProtectedButton(false);
        final String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        final UserActivation userActivation = Helper.getUserActivationData(this);
        ApiClient.getClient(this, mTracker).userActivation(softwareName, userActivation, userActivationCallback = new CancelableCallback<>(new Callback<UserActivationWrapper>() {

            @Override
            public void success(UserActivationWrapper userActivationWrapper, Response response) {
                Log.d("success", "true");
                sp.edit().putBoolean(PrefKeys.IS_WAS_LOGOUT, false).apply();
                isWasLogout = false;
                //if (userActivationWrapper.isStatusOk()) {
                sid = userActivationWrapper.getSid();
                SubscriptionSession subscriptionSession = userActivationWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String expiresAt = subscriptionSession.getQuotes().getExpiresAt();

                ProfileTable.deleteAll(ProfileTable.class);
                NotificationTable.deleteAll(NotificationTable.class);
                DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(),
                        userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());

                if (isNeedStartNextActivity) {
                    Helper.startMainActivity(GetStartedActivity.this, bootStart);
                    finish();
                }
                setProtectedButton(true);
//                } else {
//                    String userActivationError = userActivationWrapper.getError();
//                    if (userActivationError != null) {
//                        DialogsHelper.showSimpleDialog(getSupportFragmentManager(), "", userActivationError);
//                    }
//                }
            }


            @Override
            public void failure(RetrofitError error) {
                setProtectedButton(true);
                error.printStackTrace();
                if (isWasLogout) {
                    countOfTrying++;
                    if (countOfTrying < 3)
                        userActivation(true);
                    else
                        startActivityWithOutInternet();
                } else
                    processErrorActivation(error.getResponse());
            }
        }));
    }


    public String getJsonFromResponse(Response response) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {

            reader = new BufferedReader(new InputStreamReader(response.getBody().in()));

            String line;

            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sb.toString();
    }


    public void getSession() {
        if (!Utils.isNetworkAvailable(this)) {
            startActivityWithOutInternet();
            return;
        }
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this).getUserSession(softwareName, sid, userSessionCallback = new CancelableCallback<>(new Callback<UserSessionWrapper>() {

            @Override
            public void success(UserSessionWrapper userSessionWrapper, Response response) {
                //if (userSessionWrapper.isStatusOk()) {
                SubscriptionSession subscriptionSession = userSessionWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String expiresAt = subscriptionSession.getQuotes().getExpiresAt();
                List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
                ProfileTable currentProfile = profileTable.get(0);
                DatabaseUtils.saveProfile(currentProfile, currentProfile.isAuthorized(), userSessionWrapper.getIsVerified(), userSessionWrapper.getSubscription().getPlan(),
                        userSessionWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userSessionWrapper.getSubscription().getNodes());
                DatabaseUtils.saveNewNotifications(userSessionWrapper.getNotifications());

                Helper.startMainActivity(GetStartedActivity.this, bootStart);
                finish();
//                } else {
//                    Toast.makeText(GetStartedActivity.this, userSessionWrapper.getError(), Toast.LENGTH_LONG).show();
//                }
            }

            @Override
            public void failure(RetrofitError error) {
                Response response = error.getResponse();
                if (response != null)
                    Log.d(TAG, "response status: " + response.getStatus());
                processError(response);
                error.printStackTrace();
            }
        }));
    }


    public void processErrorActivation(Response response) {
        if (response != null && response.getStatus() != 408 && (response.getStatus() != 502 || response.getStatus() != 504)) {
            Log.d("Error code", response.getStatus() + " status");
            Utils.showToast(response.getReason());
            finish();
            //DialogsHelper.showSimpleDialog(getSupportFragmentManager(), "", response.getReason());
        } else if (response != null && (response.getStatus() == 502 || response.getStatus() == 504)) {
            Utils.showToast(R.string.error_server_crash);
        } else {
            Utils.showToast(R.string.check_your_network);
            finish();
        }
    }


    public void processError(Response response) {
        if (response != null && response.getStatus() != 408 && (response.getStatus() != 502 || response.getStatus() != 504)) {
            Log.d("Error code", response.getStatus() + " status");
            userActivation(true);
        } else if (response != null && (response.getStatus() == 502 || response.getStatus() == 504)) {
            Utils.showToast(R.string.error_server_crash);
            finish();
        } else {
            startActivityWithOutInternet();
        }
    }


    @Override
    public void onError(int errorStatus, int errorCode, String error) {
        if (errorStatus == 403) {
            userActivation(false);
        }
    }


    public void startAppStatusCheckerService() {
        sp.edit().putBoolean(PrefKeys.APP_OPEN, true).apply();
        startService(new Intent(this, ExitCheckerService.class));
    }


    public void checkStartAnalytics() {
        AnalyticsUtils.sendEventAppOpen(this, sp, mTracker);
        startAppStatusCheckerService();
        AnalyticsUtils.sendEventCheckForCompetitors(this, sp, mTracker);
        AnalyticsUtils.checkForNotSentEvents(this, mTracker);
    }


    public void cancelCallback() {
        if (userActivationCallback != null)
            userActivationCallback.cancel();
        if (userSessionCallback != null)
            userSessionCallback.cancel();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onCreate");
        //FlurryAgent.setFlurryAgentListener(null);
    }

    private void startActivityWithOutInternet() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(GetStartedActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(Constants.CONNECTION_STATE, false);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_login:
                startLoginActivity();
                break;
            case R.id.btn_get_protected:
                startMainActivity();
                break;
        }
    }
}