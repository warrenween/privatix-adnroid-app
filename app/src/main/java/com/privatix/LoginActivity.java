package com.privatix;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.SignInButton;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserAuthorizationWrapper;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.request.UserAuthorization;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseAuthenticationActivity implements OnClickListener, ApiErrorListener {
    public static final String TAG = LoginActivity.class.getSimpleName();
    LoginButton loginButton;
    SignInButton signInButtonGoogle;
    TextView tv_email_tips;
    CancelableCallback<UserCheckMailWrapper> userCheckMailCallback;
    CancelableCallback<UserAuthorizationWrapper> userAuthorizationCallback;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initView();
        initProgressView(mLoginFormView, mProgressView);

        initGoogleButton(signInButtonGoogle);
        initFacebookButton(loginButton);


    }

    @Override
    protected void onStart() {
        super.onStart();
        //GA - opened settings screen
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "login");
    }

    public void initView() {
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        TextView tvForgotPassword = (TextView) findViewById(R.id.tv_forgot_password);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        tv_email_tips = (TextView) findViewById(R.id.tv_email_tips);
        signInButtonGoogle = (SignInButton) findViewById(R.id.sign_in_button_google);
        loginButton = (LoginButton) findViewById(R.id.login_button_facebook);
        addEmailChangeListener();

        tvForgotPassword.setOnClickListener(this);
        mEmailSignInButton.setOnClickListener(this);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
    }


    public void addEmailChangeListener() {
        mEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (userCheckMailCallback != null)
                    userCheckMailCallback.cancel();
                if (Utils.isEmailValid(s.toString()))
                    checkMail(s.toString());
                else {
                    tv_email_tips.setVisibility(View.VISIBLE);
                    tv_email_tips.setText(R.string.email_incorrect);
                    tv_email_tips.setTextColor(getResources().getColor(R.color.redErrorColor));
                }
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !Utils.isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!Utils.isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //if (Utils.isNetworkAvailable(this)) {
            //showProgress(true);
//            if (isWasLogout)
//                userActivation(email, password);
//            else
            logIn(email, password);
            // } else {
            // Toast.makeText(this, R.string.check_your_network, Toast.LENGTH_LONG).show();
            //}

        }
    }


    public void checkMail(String login) {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserCheckMail userCheckMail = new UserCheckMail(login);
        ApiClient.getClient(this, mTracker).userCheckMail(softwareName, sid, userCheckMail, userCheckMailCallback = new CancelableCallback<>(new Callback<UserCheckMailWrapper>() {

            @Override
            public void success(UserCheckMailWrapper userCheckMailWrapper, Response response) {
                //if (userCheckMailCallback.isStatusOk()) {
                Log.e("userCheckMailCallback", "success");
                tv_email_tips.setVisibility(View.VISIBLE);
                tv_email_tips.setTextColor(getResources().getColor(R.color.green_premium_title));
                tv_email_tips.setText(R.string.email_correct);
                //}
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse() != null && error.getResponse().getStatus() != 408) {
                    showNotRegisteredMessage();
                } else
                    tv_email_tips.setVisibility(View.INVISIBLE);
                error.printStackTrace();
            }
        }));
    }


    public void showNotRegisteredMessage() {
        tv_email_tips.setTextColor(getResources().getColor(R.color.redErrorColor));
        String signUpRules = getString(R.string.email_not_registered);
        SpannableString ss = new SpannableString(signUpRules);
        ClickableSpan clickableSpanSignUp = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        };
        ss.setSpan(clickableSpanSignUp, 37, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.redErrorColor)), 37, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_email_tips.setVisibility(View.VISIBLE);
        tv_email_tips.setText(ss);
        tv_email_tips.setMovementMethod(LinkMovementMethod.getInstance());
        tv_email_tips.setHighlightColor(getResources().getColor(R.color.redErrorColor));
    }

    public void logIn(final String login, String password) {
        if (!Utils.isNetworkAvailable(LoginActivity.this)) {
            Utils.showToast(R.string.check_your_network);
            showProgress(false);
            return;
        } else
            showProgress(true);
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserAuthorization userAuthorization = new UserAuthorization(login, password);
        ApiClient.getClient(this).userAuthorization(softwareName, sid, userAuthorization, userAuthorizationCallback = new CancelableCallback<>(new Callback<UserAuthorizationWrapper>() {
            @Override
            public void success(UserAuthorizationWrapper userAuthorizationWrapper, Response response) {
                //if (userAuthorizationWrapper.isStatusOk()) {
                sid = userAuthorizationWrapper.getSid();
                authorized(sid, login);
                getSession();

                //GA - login via email
                AnalyticsUtils.sendEventEmailLogin(LoginActivity.this, mTracker);
//                } else {
//                    showProgress(false);
//                    Toast.makeText(LoginActivity.this, userAuthorizationWrapper.getError(), Toast.LENGTH_LONG).show();
//                }
            }

            @Override
            public void failure(RetrofitError error) {
                //Log.e("Error url", error.getUrl() + " url");

                showProgress(false);
                error.printStackTrace();
            }
        }));
    }

    @Override
    public void authenticationSocialFinished(String provider) {
        if (provider.equals("fb")) {
            AnalyticsUtils.sendEventFacebookLogin(LoginActivity.this, mTracker);
        } else if (provider.equals("gp")) {
            AnalyticsUtils.sendEventGooglePlusLogin(LoginActivity.this, mTracker);
        }
    }

    @Override
    public void activationEmailFinished(String email, final String password) {
        logIn(email, password);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.email_sign_in_button:
                attemptLogin();
                break;
            case R.id.tv_forgot_password:
                Intent intent = new Intent(LoginActivity.this, RecoveryActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onError(int errorStatus, int errorCode, String error) {
        if (errorStatus == 403) {
            userActivation("", "", "");
        }
    }


    public void cancelCallbacks() {
        if (userCheckMailCallback != null)
            userCheckMailCallback.cancel();
        if (userAuthorizationCallback != null)
            userAuthorizationCallback.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCallbacks();
    }
}

