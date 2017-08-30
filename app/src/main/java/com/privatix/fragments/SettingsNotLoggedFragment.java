package com.privatix.fragments;

import android.app.Activity;
import android.app.Fragment;
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

import com.privatix.LoginActivity;
import com.privatix.PremiumActivity;
import com.privatix.R;
import com.privatix.SettingsActivity;
import com.privatix.SignUpActivity;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.privatix.utils.interfaces.OnFragmentInteractionListener;

public class SettingsNotLoggedFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    SettingsActivity settingsActivity;
    OnFragmentInteractionListener mListener;
    TextView tv_app_version;
    Switch switchNetworkAlert, switchConnectOnStartup, switchSendErrorLogs;
    SharedPreferences sp;

    public SettingsNotLoggedFragment() {
        // Required empty public constructor
    }

    public static SettingsNotLoggedFragment newInstance() {
        return new SettingsNotLoggedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.settings_not_logged, container, false);
        initView(view);
        setVersionTextView();

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

        return view;
    }


    public void initView(View view) {
        TextView tv_login = (TextView) view.findViewById(R.id.tv_login);
        TextView tv_sign_up = (TextView) view.findViewById(R.id.tv_signup);
        TextView tv_get_help = (TextView) view.findViewById(R.id.tv_get_help);
        TextView tv_term_of_service = (TextView) view.findViewById(R.id.tv_term_of_service);
        TextView tv_sharing = (TextView) view.findViewById(R.id.tv_sharing);
        TextView tv_privacy_policy = (TextView) view.findViewById(R.id.tv_privacy_policy);
        TextView tv_imprint = (TextView) view.findViewById(R.id.tv_imprint);
        TextView tv_leave_feedback = (TextView) view.findViewById(R.id.tv_leave_feedback);
        TextView tv_get_premium = (TextView) view.findViewById(R.id.tv_get_premium);
        tv_app_version = (TextView) view.findViewById(R.id.tv_app_version);
        switchNetworkAlert = (Switch) view.findViewById(R.id.switchNetworkAlert);
        switchConnectOnStartup = (Switch) view.findViewById(R.id.switchConnectOnStartup);
        switchSendErrorLogs = (Switch) view.findViewById(R.id.switchSendErrorLogs);


        tv_get_help.setOnClickListener(this);
        tv_sharing.setOnClickListener(this);
        tv_term_of_service.setOnClickListener(this);
        tv_privacy_policy.setOnClickListener(this);
        tv_imprint.setOnClickListener(this);
        tv_login.setOnClickListener(this);
        tv_sign_up.setOnClickListener(this);
        tv_leave_feedback.setOnClickListener(this);
        tv_get_premium.setOnClickListener(this);
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_login:
                startActivity(new Intent(getActivity(), LoginActivity.class));
                break;
            case R.id.tv_signup:
                startActivity(new Intent(getActivity(), SignUpActivity.class));
                break;
            case R.id.tv_get_help:
                settingsActivity.startGetHelpPage();
                break;
            case R.id.tv_sharing:
                settingsActivity.share();
                break;
            case R.id.tv_term_of_service:
                settingsActivity.startTosPage();
                break;
            case R.id.tv_privacy_policy:
                settingsActivity.startPrivacyPolicyPage();
                break;
            case R.id.tv_imprint:
                settingsActivity.startImprintPage();
                break;
            case R.id.tv_leave_feedback:
                settingsActivity.showRatingDialog();
                break;
            case R.id.tv_get_premium:
                Intent intentPremium = new Intent(getActivity(), PremiumActivity.class);
                getActivity().startActivity(intentPremium);
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
