package com.privatix;

import android.content.Intent;
import android.graphics.Color;
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
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.SignInButton;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.answer.UserRegistrationWrapper;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.api.models.request.UserRegistration;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A login screen that offers login via email/password.
 */
public class SignUpActivity extends BaseAuthenticationActivity implements OnClickListener, ApiErrorListener {
    public static final String TAG = SignUpActivity.class.getSimpleName();
    LoginButton loginButton;
    SignInButton signInButtonGoogle;
    TextView tv_sign_up_rules, tv_email_tips;
    CancelableCallback<UserCheckMailWrapper> userCheckMailCallback;
    CancelableCallback<UserRegistrationWrapper> registrationCallback;
    LinearLayout ll_main;
    WebView webView;
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
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_sign_up);
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
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "signup");
    }

    @Override
    public void authenticationSocialFinished(String provider) {
        if (provider.equals("fb")) {
            AnalyticsUtils.sendEventSignUpFacebook(SignUpActivity.this, mTracker);
        } else if (provider.equals("gp")) {
            AnalyticsUtils.sendEventSignUpGooglePLus(SignUpActivity.this, mTracker);
        }
    }


    @Override
    public void activationEmailFinished(String email, final String password) {
        signUp(email, password);
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
            mLoginFormView.setVisibility(View.VISIBLE);
            ll_main.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        } else {
            try {
                super.onBackPressed();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void initView() {
        Button mEmailSignUpButton = (Button) findViewById(R.id.email_sign_up_button);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        tv_sign_up_rules = (TextView) findViewById(R.id.tv_sign_up_rules);
        webView = (WebView) findViewById(R.id.webView);
        ll_main = (LinearLayout) findViewById(R.id.ll_main);
        tv_email_tips = (TextView) findViewById(R.id.tv_email_tips);
        signInButtonGoogle = (SignInButton) findViewById(R.id.sign_in_button_google);
        loginButton = (LoginButton) findViewById(R.id.login_button_facebook);

        String signUpRules = getString(R.string.sing_up_rules);
        SpannableString ss = new SpannableString(signUpRules);
        ClickableSpan clickableSpanTermOfService = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                goToUrl(getString(R.string.url_tos));
            }
        };
        ss.setSpan(clickableSpanTermOfService, 31, 47, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClickableSpan clickableSpanPrivacyPolicy = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                goToUrl(getString(R.string.url_privacy_policy));
            }
        };
        ss.setSpan(clickableSpanPrivacyPolicy, 52, signUpRules.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_sign_up_rules.setText(ss);
        tv_sign_up_rules.setMovementMethod(LinkMovementMethod.getInstance());
        tv_sign_up_rules.setHighlightColor(Color.TRANSPARENT);

        mEmailSignUpButton.setOnClickListener(this);

        addEmailChangeListener();

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptSignUp();
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


    public void goToUrl(String url) {
        mLoginFormView.setVisibility(View.GONE);
        ll_main.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignUp() {
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
//            if (Utils.isNetworkAvailable(this)) {
//                showProgress(true);
//            if (isWasLogout)
//                userActivation(email, password);
//            else
            signUp(email, password);
//            } else {
//                Toast.makeText(this, R.string.check_your_network, Toast.LENGTH_LONG).show();
//            }
        }
    }


    public void checkMail(String login) {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserCheckMail userCheckMail = new UserCheckMail(login);
        ApiClient.getClient(this, mTracker).userCheckMail(softwareName, sid, userCheckMail, userCheckMailCallback = new CancelableCallback<>(new Callback<UserCheckMailWrapper>() {

            @Override
            public void success(UserCheckMailWrapper userCheckMailWrapper, Response response) {
                //if (userCheckMailCallback.isStatusOk()) {
                showAlreadyRegisteredMessage();
                Log.e("userCheckMailCallback", "success");
                //}
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse() != null && error.getResponse().getStatus() != 408) {
                    tv_email_tips.setVisibility(View.VISIBLE);
                    tv_email_tips.setTextColor(getResources().getColor(R.color.green_premium_title));
                    tv_email_tips.setText(R.string.email_correct);
                } else
                    tv_email_tips.setVisibility(View.INVISIBLE);
                error.printStackTrace();
            }
        }));
    }


    public void showAlreadyRegisteredMessage() {
        tv_email_tips.setTextColor(getResources().getColor(R.color.redErrorColor));
        String signUpRules = getString(R.string.email_registered);
        SpannableString ss = new SpannableString(signUpRules);
        ClickableSpan clickableSpanLogin = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        };
        ss.setSpan(clickableSpanLogin, 42, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.redErrorColor)), 42, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_email_tips.setVisibility(View.VISIBLE);
        tv_email_tips.setText(ss);
        tv_email_tips.setMovementMethod(LinkMovementMethod.getInstance());
        tv_email_tips.setHighlightColor(getResources().getColor(R.color.redErrorColor));
    }


    public void signUp(final String login, String password) {
        if (!Utils.isNetworkAvailable(SignUpActivity.this)) {
            Utils.showToast(R.string.check_your_network);
            showProgress(false);
            return;
        } else
            showProgress(true);
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserRegistration userRegistration = new UserRegistration(login, password);
        ApiClient.getClient(this).userRegistration(softwareName, sid, userRegistration, registrationCallback = new CancelableCallback<>(new Callback<UserRegistrationWrapper>() {
            @Override
            public void success(UserRegistrationWrapper userRegistrationWrapper, Response response) {
                //if (userRegistrationWrapper.isStatusOk()) {
                Log.e("userRegistrationWrapper", "success");
                sid = userRegistrationWrapper.getSid();
                authorized(sid, login);
                getSession();
                Toast.makeText(SignUpActivity.this, R.string.sign_up_complete, Toast.LENGTH_LONG).show();

                //GA
                AnalyticsUtils.sendEventSignUpEmail(SignUpActivity.this, mTracker);

//                } else {
//                    showProgress(false);
//                    Toast.makeText(SignUpActivity.this, userRegistrationWrapper.getError(), Toast.LENGTH_LONG).show();
//                }
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                showProgress(false);
            }
        }));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.email_sign_up_button:
                attemptSignUp();
                break;
        }

    }


    @Override
    public void onError(int errorStatus, int errorCode, String error) {
        if (errorStatus == 403) {
            userActivation("", "");
        }
    }


    public void cancelCallbacks() {
        if (userCheckMailCallback != null)
            userCheckMailCallback.cancel();
        if (registrationCallback != null)
            registrationCallback.cancel();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCallbacks();
    }
}