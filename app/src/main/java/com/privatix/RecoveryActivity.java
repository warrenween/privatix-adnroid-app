package com.privatix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.privatix.api.ApiClient;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.answer.UserRecoverWrapper;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.api.models.request.UserRecover;
import com.privatix.fragments.dialogs.SimpleDialog;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RecoveryActivity extends BaseAnalyticsActivity implements ApiErrorListener {
    public static final String TAG = RecoveryActivity.class.getSimpleName();
    Button btn_recover, btn_back;
    String sid;
    LinearLayout ll_password_recovered, ll_back;
    TextView tv_email_tips;
    CancelableCallback<UserCheckMailWrapper> userCheckMailWrapper;
    private AutoCompleteTextView mEmailView;
    private View mProgressView;
    private View passwordRecoveryForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recovery);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initView();
        sid = DatabaseUtils.getSid();
    }


    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "recover");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void initView() {
        btn_recover = (Button) findViewById(R.id.btn_recover);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        passwordRecoveryForm = findViewById(R.id.password_recovery_form);
        mProgressView = findViewById(R.id.recovery_progress);
        ll_password_recovered = (LinearLayout) findViewById(R.id.ll_password_recovered);
        ll_back = (LinearLayout) findViewById(R.id.ll_back);
        tv_email_tips = (TextView) findViewById(R.id.tv_email_tips);

        btn_back = (Button) findViewById(R.id.btn_back);

        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btn_recover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRecover();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (userCheckMailWrapper != null)
                    userCheckMailWrapper.cancel();
                if (isEmailValid(s.toString()))
                    checkMail(s.toString());
                else {
                    tv_email_tips.setVisibility(View.VISIBLE);
                    tv_email_tips.setText(R.string.email_incorrect);
                    tv_email_tips.setTextColor(getResources().getColor(R.color.redErrorColor));
                }
            }
        });
    }


    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRecover() {
        // Reset errors.
        mEmailView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
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
            showProgress(true);
            recovery(email);
        }
    }


    public void checkMail(String login) {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserCheckMail userCheckMail = new UserCheckMail(login);
        ApiClient.getClient(this, mTracker).userCheckMail(softwareName, sid, userCheckMail, userCheckMailWrapper = new CancelableCallback<>(new Callback<UserCheckMailWrapper>() {

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
                Intent intent = new Intent(RecoveryActivity.this, SignUpActivity.class);
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


    public void recovery(String login) {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserRecover userRecover = new UserRecover(login);
        ApiClient.getClient(this, mTracker).userRecover(softwareName, sid, userRecover, new CancelableCallback<UserRecoverWrapper>(new Callback<UserRecoverWrapper>() {
            @Override
            public void success(UserRecoverWrapper userRecoverWrapper, Response response) {
                //if (userRecoverWrapper.isStatusOk()) {
                //Toast.makeText(RecoveryActivity.this, "Message sent to your email", Toast.LENGTH_LONG).show();
                    /*SimpleDialog recoverDialog = SimpleDialog.newInstance("", getString(R.string.check_email));
                    recoverDialog.show(getSupportFragmentManager(), "dialogRecover");*/
                    /*Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();*/
                showProgressWithoutForm(false);
                ll_password_recovered.setVisibility(View.VISIBLE);

                //GA - password recovery
                AnalyticsUtils.sendEventPasswordRecoverySuccess(RecoveryActivity.this, mTracker);

//                } else {
//                    Log.e("Error", "error recovery");
//                    Toast.makeText(RecoveryActivity.this, userRecoverWrapper.getError(), Toast.LENGTH_LONG).show();
//                    showProgress(false);
//                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Error", "error recovery");
                showProgress(false);
                if (error.getResponse() == null) {
                    Toast.makeText(RecoveryActivity.this, R.string.check_your_network, Toast.LENGTH_LONG).show();
                } else if (error.getResponse().getStatus() == 500) {
                    SimpleDialog recoverDialog = SimpleDialog.newInstance("", getString(R.string.no_user_found));
                    recoverDialog.show(getSupportFragmentManager(), "dialogRecover");
                }
                error.printStackTrace();
            }
        }));

    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            passwordRecoveryForm.setVisibility(show ? View.GONE : View.VISIBLE);
            passwordRecoveryForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    //passwordRecoveryForm.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            passwordRecoveryForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgressWithoutForm(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onError(int errorStatus, int errorCode, String error) {
    }

}
