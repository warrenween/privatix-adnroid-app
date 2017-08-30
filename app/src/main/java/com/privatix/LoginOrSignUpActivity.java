package com.privatix;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.PrefKeys;

public class LoginOrSignUpActivity extends BaseAnalyticsActivity implements View.OnClickListener {
    Button btn_sign_in, btn_sign_up;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_or_sign_up);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        btn_sign_in = (Button) findViewById(R.id.btn_sign_in);
        btn_sign_up = (Button) findViewById(R.id.btn_sign_up);

        btn_sign_in.setOnClickListener(this);
        btn_sign_up.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, "login&signup");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sign_up:
                Intent intentSignUp = new Intent(LoginOrSignUpActivity.this, SignUpActivity.class);
                intentSignUp.putExtra(PrefKeys.FROM_PREMIUM, true);
                startActivity(intentSignUp);
                break;
            case R.id.btn_sign_in:
                Intent intentLogin = new Intent(LoginOrSignUpActivity.this, LoginActivity.class);
                intentLogin.putExtra(PrefKeys.FROM_PREMIUM, true);
                startActivity(intentLogin);
                break;
        }
    }
}
