package com.privatix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserActivationWrapper;
import com.privatix.api.models.answer.UserRegistrationWrapper;
import com.privatix.api.models.answer.UserSessionWrapper;
import com.privatix.api.models.answer.subscription.SubscriptionSession;
import com.privatix.api.models.request.UserActivation;
import com.privatix.api.models.request.UserOAuth;
import com.privatix.model.NotificationTable;
import com.privatix.model.ProfileTable;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Helper;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A login screen that offers login via email/password.
 */
public abstract class BaseAuthenticationActivity extends BaseAnalyticsActivity implements GoogleApiClient.OnConnectionFailedListener, ApiErrorListener {
    public static final String TAG = BaseAuthenticationActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 9001;
    String sid;
    CallbackManager callbackManager;
    GoogleSignInOptions gso;
    Boolean isFromPremium = false;
    SharedPreferences sp;
    CancelableCallback<UserActivationWrapper> userActivationCallback;
    CancelableCallback<UserSessionWrapper> userSessionCallback;
    CancelableCallback<UserRegistrationWrapper> userRegistrationCallback;
    private GoogleApiClient mGoogleApiClient;
    private View mProgressView;
    private View mAuthenticationFormView;
    FacebookCallback<LoginResult> facebookCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            showProgress(true);
            final String token = loginResult.getAccessToken().getToken();
            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    Log.i(TAG, "Facebook response: " + response.toString());
                    Bundle bFacebookData = getFacebookData(object);
                    assert bFacebookData != null;
                    String email = bFacebookData.getString("email");
                    // Get facebook data from login
                    facebookLoginFinished(email, token);
                }
            });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id, email");
            request.setParameters(parameters);
            request.executeAsync();

            Log.d("onSuccess Facebook", loginResult.getAccessToken().getToken() + "");
        }

        @Override
        public void onCancel() {
            Log.d("Cancel Facebook", "true");
        }

        @Override
        public void onError(FacebookException error) {
            Utils.showToast(error.getLocalizedMessage());
            error.printStackTrace();
        }
    };

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);

        Intent intent = getIntent();
        if (intent != null)
            isFromPremium = intent.getBooleanExtra(PrefKeys.FROM_PREMIUM, false);


        sid = DatabaseUtils.getSid();
        initGooglePlus();
        callbackManager = CallbackManager.Factory.create();
        getSocialHashKey();
    }

    public void initProgressView(View authenticationFormView, View progressView) {
        mAuthenticationFormView = authenticationFormView;
        mProgressView = progressView;
    }

    private Bundle getFacebookData(JSONObject object) {
        Bundle bundle = new Bundle();
        String id = null;
        try {
            id = object.getString("id");
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }

        try {
            URL profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=200&height=150");
            Log.i("profile_pic", profile_pic + "");
            bundle.putString("profile_pic", profile_pic.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        bundle.putString("idFacebook", id);
        try {
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bundle;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            try {
                onBackPressed();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initGooglePlus() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        Scope scope = new Scope(Scopes.PLUS_LOGIN);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(scope)
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(Plus.API)
                .build();
    }

    public void initGoogleButton(SignInButton signInButtonGoogle) {
        //assert signInButtonGoogle != null;
        signInButtonGoogle.setSize(SignInButton.SIZE_WIDE);
        signInButtonGoogle.setScopes(gso.getScopeArray());
        signInButtonGoogle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "sign in google");
                showProgress(true);
                signInGooglePlus();
            }
        });
    }

    public void initFacebookButton(LoginButton loginButton) {
        assert loginButton != null;
        loginButton.setReadPermissions("public_profile", "email");
        loginButton.registerCallback(callbackManager, facebookCallback);
    }

    public void signInGooglePlus() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void getSocialHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void userActivation(final String email, final String token, final String socialType) {
        if (!Utils.isNetworkAvailable(BaseAuthenticationActivity.this)) {
            showProgress(false);
            return;
        }
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserActivation userActivation = Helper.getUserActivationData(this);
        ApiClient.getClient(this).userActivation(softwareName, userActivation, userActivationCallback = new CancelableCallback<>(new Callback<UserActivationWrapper>() {

            @Override
            public void success(UserActivationWrapper userActivationWrapper, Response response) {
                sid = userActivationWrapper.getSid();
                sp.edit().putBoolean(PrefKeys.IS_WAS_LOGOUT, false).apply();
                SubscriptionSession subscriptionSession = userActivationWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String expiresAt = subscriptionSession.getQuotes().getExpiresAt();
                ProfileTable.deleteAll(ProfileTable.class);
                NotificationTable.deleteAll(NotificationTable.class);
                DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, sid,
                        subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(token) && !TextUtils.isEmpty(socialType))
                    loginSocial(email, token, socialType);
            }


            @Override
            public void failure(RetrofitError error) {
                showProgress(false);
                Utils.showToast(error.getLocalizedMessage());
                error.printStackTrace();
            }
        }));
    }

    public void userActivation(final String email, final String password) {
        if (!Utils.isNetworkAvailable(BaseAuthenticationActivity.this)) {
            Utils.showToast(R.string.check_your_network);
            showProgress(false);
            return;
        } else {
            showProgress(true);
        }
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        final UserActivation userActivation = Helper.getUserActivationData(this);
        ApiClient.getClient(this).userActivation(softwareName, userActivation, userActivationCallback = new CancelableCallback<>(new Callback<UserActivationWrapper>() {

            @Override
            public void success(UserActivationWrapper userActivationWrapper, Response response) {
                Log.d(TAG, "user activation success");
                sid = userActivationWrapper.getSid();
                sp.edit().putBoolean(PrefKeys.IS_WAS_LOGOUT, false).apply();
                SubscriptionSession subscriptionSession = userActivationWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String expiresAt = userActivationWrapper.getSubscription().getQuotes().getExpiresAt();
                ProfileTable.deleteAll(ProfileTable.class);
                NotificationTable.deleteAll(NotificationTable.class);
                DatabaseUtils.saveProfile(new ProfileTable(), false, false, "free", userActivationWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, sid,
                        subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userActivationWrapper.getSubscription().getNodes());
                activationEmailFinished(email, password);
            }


            @Override
            public void failure(RetrofitError error) {
                showProgress(false);
                Utils.showToast(error.getLocalizedMessage());
                error.printStackTrace();
            }
        }));
    }

    public void authorized(String sid, String email) {
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTable.get(0);
        currentProfile.setSid(sid);
        currentProfile.setIs_authorized(true);
        currentProfile.setUserEmail(email);
        currentProfile.save();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            //Event - login via G+
            AnalyticsUtils.sendEventGooglePlusLogin(BaseAuthenticationActivity.this, mTracker);
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            return;
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result != null && result.isSuccess()) {
            Log.d(TAG, "handleSignInResult:" + result.isSuccess());
            Log.d(TAG, "handleSignInResult: Status " + result.getStatus().getStatusMessage());
            Log.d(TAG, "handleSignInResult: Status " + result.getSignInAccount().getServerAuthCode());
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String idToken = acct.getIdToken();
            String email = acct.getEmail();
//            if (isWasLogout)
//                userActivation(email, idToken, "gp");
//            else
//                loginSocial(email, idToken, "gp");
            startLoginSocial(email, idToken, "gp");
            Log.d(TAG, "handleSignInResult: AuthCode " + idToken);
            //Log.d(TAG, "handleSignInResult: Id token " + acct.ge());
        } else {
            showProgress(false);
            if (result != null) {
                Log.e(TAG, "handleSignInResult: Status " + result.getStatus().getStatusMessage());
                Log.e(TAG, "handleSignInResult: Status " + result.getStatus().getStatus());
                Log.e(TAG, "handleSignInResult: Status " + result.getStatus().getStatusCode());
            } else {
                Log.e(TAG, "handleSignInResult: null ");
            }
            // Signed out, show unauthenticated UI.
            //updateUI(false);
        }
    }

    public void loginSocial(final String email, String token, final String provider) {
        if (!Utils.isNetworkAvailable(BaseAuthenticationActivity.this)) {
            Utils.showToast(R.string.check_your_network);
            showProgress(false);
        }
        UserOAuth userOAuth = new UserOAuth(token, provider);
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this).userAuthorizationSocial(softwareName, sid, userOAuth, userRegistrationCallback = new CancelableCallback<>(new Callback<UserRegistrationWrapper>() {

            @Override
            public void success(UserRegistrationWrapper userRegistrationWrapper, Response response) {
                sid = userRegistrationWrapper.getSid();
                authorized(sid, email);
                getSession();
                authenticationSocialFinished(provider);
            }

            @Override
            public void failure(RetrofitError error) {
                showProgress(false);
                error.printStackTrace();
                Utils.showToast(error.getLocalizedMessage());
            }
        }));
    }

    public void getSession() {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        ApiClient.getClient(this).getUserSession(softwareName, sid, userSessionCallback = new CancelableCallback<>(new Callback<UserSessionWrapper>() {

            @Override
            public void success(UserSessionWrapper userSessionWrapper, Response response) {
                sp.edit().putBoolean(PrefKeys.STARTED, true).apply();
                SubscriptionSession subscriptionSession = userSessionWrapper.getSubscription();
                String userLoginVpn = subscriptionSession.getLogin();
                String userPasswordVpn = subscriptionSession.getPassword();
                String currentPlan = subscriptionSession.getPlan();
                String expiresAt = subscriptionSession.getQuotes().getExpiresAt();
                List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
                ProfileTable currentProfile = profileTable.get(0);
                DatabaseUtils.saveProfile(currentProfile, currentProfile.isAuthorized(), userSessionWrapper.getIsVerified(), currentPlan,
                        userSessionWrapper.getCountry(), userLoginVpn, userPasswordVpn, expiresAt, sid, subscriptionSession.getSubscriptionId());
                DatabaseUtils.saveNewSubscriptions(userSessionWrapper.getSubscription().getNodes());
                DatabaseUtils.saveNewNotifications(userSessionWrapper.getNotifications());
                if (isFromPremium && currentPlan.equals(getString(R.string.plan_name_free))) {
                    Helper.startPremiumActivity(BaseAuthenticationActivity.this);
                } else {
                    Helper.startMainActivity(BaseAuthenticationActivity.this, false);
                }
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse() != null && error.getResponse().getStatus() != 408) {
                    Log.e("Error code", error.getResponse().getStatus() + " status");
                    userActivation("", "", "");
//                    if (Utils.isNetworkAvailable(BaseAuthenticationActivity.this))
//                        userActivation("", "", "");
//                    else {
//                        showProgress(false);
//                        Toast.makeText(BaseAuthenticationActivity.this, R.string.check_your_network, Toast.LENGTH_LONG).show();
//                    }
                } else {
                    Toast.makeText(BaseAuthenticationActivity.this, R.string.check_your_network, Toast.LENGTH_LONG).show();
                    showProgress(false);
                }

                error.printStackTrace();
            }
        }));
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        showProgress(show, mAuthenticationFormView, mProgressView);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show, final View authenticationFormView, final View progressView) {
        if (authenticationFormView == null || progressView == null)
            return;
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            authenticationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            authenticationFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    authenticationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            authenticationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Google plus", "failed " + connectionResult.getErrorMessage());
    }

    public void facebookLoginFinished(String email, String token) {
        //if (Utils.isNetworkAvailable(BaseAuthenticationActivity.this)) {
        startLoginSocial(email, token, "fb");
//        } else {
//            showProgress(false);
//            Toast.makeText(BaseAuthenticationActivity.this, R.string.check_your_network, Toast.LENGTH_LONG).show();
//        }
    }


    public void startLoginSocial(String email, String token, String provider) {
        loginSocial(email, token, provider);
    }

    public abstract void authenticationSocialFinished(String provider);

    public abstract void activationEmailFinished(String email, final String password);


    public void cancelCallbacks() {
        if (userActivationCallback != null)
            userActivationCallback.cancel();
        if (userSessionCallback != null)
            userSessionCallback.cancel();
        if (userRegistrationCallback != null)
            userRegistrationCallback.cancel();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCallbacks();
    }
}

