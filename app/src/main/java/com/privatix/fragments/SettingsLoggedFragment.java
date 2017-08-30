package com.privatix.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.privatix.GetStartedActivity;
import com.privatix.PremiumActivity;
import com.privatix.R;
import com.privatix.SettingsActivity;
import com.privatix.model.NotificationTable;
import com.privatix.model.ProfileTable;
import com.privatix.utils.Helper;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.privatix.utils.interfaces.OnFragmentInteractionListener;

import org.strongswan.android.logic.VpnStateService;

import java.util.List;

public class SettingsLoggedFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    TextView tv_app_version;
    String email;
    String plan;
    String premiumExpiresAt;
    SharedPreferences sp;
    SettingsActivity settingsActivity;
    ProgressDialog progressDialog;
    boolean logoutStart = false;
    private OnFragmentInteractionListener mListener;

    public SettingsLoggedFragment() {
        // Required empty public constructor
    }

    public static SettingsLoggedFragment newInstance() {
        return new SettingsLoggedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTable.get(0);
        email = currentProfile.getUserEmail();
        plan = currentProfile.getPlan();
        premiumExpiresAt = currentProfile.getPremiumExpiresAt();
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.settings_logged, container, false);
        TextView tv_email = (TextView) view.findViewById(R.id.tv_email);
        TextView tv_account_type = (TextView) view.findViewById(R.id.tv_account_type);
        TextView tv_premium_expires_date = (TextView) view.findViewById(R.id.tv_premium_expires_date);
        TextView tv_logout = (TextView) view.findViewById(R.id.tv_logout);
        Switch switchNetworkAlert = (Switch) view.findViewById(R.id.switchNetworkAlert);
        Switch switchConnectOnStartup = (Switch) view.findViewById(R.id.switchConnectOnStartup);
        Switch switchSendErrorLogs = (Switch) view.findViewById(R.id.switchSendErrorLogs);

        sp = getActivity().getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
        boolean isNetworkAlertEnabled = sp.getBoolean(PrefKeys.NETWORK_ALERTS, PrefKeys.NETWORK_ALERTS_DEFAULT);
        switchNetworkAlert.setChecked(isNetworkAlertEnabled);
        switchNetworkAlert.setOnCheckedChangeListener(this);


        boolean connectOnStartup = sp.getBoolean(PrefKeys.CONNECT_ON_STARTUP, PrefKeys.CONNECT_ON_STARTUP_DEFAULT);
        switchConnectOnStartup.setChecked(connectOnStartup);
        switchConnectOnStartup.setOnCheckedChangeListener(this);


        boolean isSendErrorLogs = sp.getBoolean(PrefKeys.SEND_ERROR_LOGS, PrefKeys.SEND_ERROR_LOGS_DEFAULT);
        switchSendErrorLogs.setChecked(isSendErrorLogs);
        switchSendErrorLogs.setOnCheckedChangeListener(this);

        tv_logout.setOnClickListener(this);
        tv_email.setText(email);

        tv_app_version = (TextView) view.findViewById(R.id.tv_app_version);
        TextView tv_get_help = (TextView) view.findViewById(R.id.tv_get_help);
        TextView tv_get_premium = (TextView) view.findViewById(R.id.tv_get_premium);
        TextView tv_sharing = (TextView) view.findViewById(R.id.tv_sharing);
        TextView tv_term_of_service = (TextView) view.findViewById(R.id.tv_term_of_service);
        TextView tv_privacy_policy = (TextView) view.findViewById(R.id.tv_privacy_policy);
        TextView tv_imprint = (TextView) view.findViewById(R.id.tv_imprint);
        TextView tv_leave_feedback = (TextView) view.findViewById(R.id.tv_leave_feedback);

        tv_get_help.setOnClickListener(this);
        tv_sharing.setOnClickListener(this);
        tv_term_of_service.setOnClickListener(this);
        tv_privacy_policy.setOnClickListener(this);
        tv_imprint.setOnClickListener(this);
        tv_get_premium.setOnClickListener(this);
        tv_leave_feedback.setOnClickListener(this);

        if (plan.equals("free")) {
            tv_premium_expires_date.setText(R.string.expiration_date_lifetime);
            tv_account_type.setText(R.string.setting_account_free);
        } else {
            tv_get_premium.setVisibility(View.GONE);
            tv_account_type.setText(R.string.setting_account_premium);
            if (premiumExpiresAt != null)
                tv_premium_expires_date.setText(Helper.convertUnixTimeToDate(Long.valueOf(premiumExpiresAt)));
        }

        setVersionTextView();
        return view;
    }


    public void setVersionTextView() {
        String version = Utils.getAppVersion(getActivity());
        tv_app_version.setText(version);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        settingsActivity = (SettingsActivity) activity;
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        dismissProgressDialog();
    }


    public void logOut() {
//        ProfileTable.deleteAll(ProfileTable.class);
//        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
//        ProfileTable currentProfile = profileTable.get(0);
//        currentProfile.setIs_authorized(false);
//        currentProfile.setIs_verified(false);
//        currentProfile.setPlan("free");
//        currentProfile.save();
        NotificationTable.deleteAll(NotificationTable.class);
        LoginManager.getInstance().logOut();
    }


    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(getActivity(), null,
                "Log out...");
        progressDialog.setCancelable(false);
    }

    private void dismissProgressDialog() {
        if (settingsActivity != null && !settingsActivity.isFinishing() && progressDialog != null)
            progressDialog.dismiss();
    }


//    public void goToStartScreen(){
//        showProgressDialog();
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                dismissProgressDialog();
//                Intent intent = new Intent(getActivity(), GetStartedActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//                getActivity().finish();
//            }
//        }, 1500);
//    }


    public void goToStartScreen() {
        dismissProgressDialog();
        Intent intent = new Intent(getActivity(), GetStartedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }


    public void logOutFinish() {
        if (logoutStart)
            goToStartScreen();
    }


    @SuppressLint("CommitPrefEdits")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_logout:
                logoutStart = true;
                //showProgressDialog();
                logOut();
                sp.edit().putBoolean(PrefKeys.IS_WAS_LOGOUT, true).commit();
                if (settingsActivity.mService != null && (settingsActivity.mService.getState() == VpnStateService.State.CONNECTED || settingsActivity.mService.getState() == VpnStateService.State.CONNECTING)) {
                    showProgressDialog();
                    settingsActivity.disconnectVpn();
                } else
                    goToStartScreen();
                break;
            case R.id.tv_get_help:
                settingsActivity.startGetHelpPage();
                break;
            case R.id.tv_term_of_service:
                settingsActivity.startTosPage();
                break;
            case R.id.tv_sharing:
                settingsActivity.share();
                break;
            case R.id.tv_privacy_policy:
                settingsActivity.startPrivacyPolicyPage();
                break;
            case R.id.tv_imprint:
                settingsActivity.startImprintPage();
                break;
            case R.id.tv_get_premium:
                Intent intentPremium = new Intent(getActivity(), PremiumActivity.class);
                getActivity().startActivity(intentPremium);
                break;
            case R.id.tv_leave_feedback:
                settingsActivity.showRatingDialog();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switchNetworkAlert:
                settingsActivity.changeNetworkAlertPrefs(isChecked);
                break;
            case R.id.switchConnectOnStartup:
                settingsActivity.changeStartUpPrefs(isChecked);
                break;
            case R.id.switchSendErrorLogs:
                sp.edit().putBoolean(PrefKeys.SEND_ERROR_LOGS, isChecked).apply();
                break;
        }

    }
}
