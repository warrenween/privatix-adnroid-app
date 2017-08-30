package com.privatix.fragments.dialogs;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.privatix.MainActivity;
import com.privatix.R;
import com.privatix.model.ProfileTable;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.zendesk.sdk.feedback.impl.BaseZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.zendesk.sdk.model.access.AnonymousIdentity;
import com.zendesk.sdk.model.access.Identity;
import com.zendesk.sdk.network.impl.ZendeskConfig;

import java.util.Arrays;
import java.util.List;


public class HelpImproveDialog extends DialogFragment implements View.OnClickListener {
    public static final String TAG = HelpImproveDialog.class.getSimpleName();
    Context mContext;
    View rootView;
    TextView tvSure;
    TextView tvNotNow;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String additionalData;


    public HelpImproveDialog() {
        // Required empty public constructor
    }


    public static HelpImproveDialog newInstance() {
        return new HelpImproveDialog();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_help_improve_dialog, container, false);
        tvSure = (TextView) rootView.findViewById(R.id.sure);
        tvNotNow = (TextView) rootView.findViewById(R.id.not_now);
        tvSure.setOnClickListener(this);
        tvNotNow.setOnClickListener(this);
        sharedPreferences = getActivity().getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);

        return rootView;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onClick(View v) {
        editor = sharedPreferences.edit();
        if (v.getId() == R.id.not_now) {
            int newCounter = sharedPreferences.getInt(PrefKeys.COUNTER_KEY, 0);
            //Log.e(TAG, "new Counter " + newCounter);
            if (mContext instanceof MainActivity) {
                //Log.e(TAG, "Context instanceof MainActivity ");
                editor.putInt(PrefKeys.COUNTER_KEY, ++newCounter);
                if (newCounter > 2)
                    editor.putBoolean(PrefKeys.NEED_SHOW_KEY, false);
            }
        } else {
            editor.putBoolean(PrefKeys.NEED_SHOW_KEY, false);
            startZendeskTicket();
        }
        editor.apply();
        dismiss();
    }


    public void startZendeskTicket() {
        String subscriptionId = "", email = "";
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        if (profileTable.size() > 0) {
            ProfileTable currentProfile = profileTable.get(0);
            subscriptionId = currentProfile.getSubscriptionId();
            email = currentProfile.getUserEmail();

        }
        AnonymousIdentity.Builder builder = new AnonymousIdentity.Builder();
        if (!TextUtils.isEmpty(email))
            builder.withEmailIdentifier(email);
        Identity anonymousIdentity = builder.build();


        additionalData = "Android " + "Version: " + Build.VERSION.RELEASE + ";" + " Device model:" + Build.MODEL + ";";
        additionalData += " App Version: " + Utils.getAppVersion(getActivity()) + ";";
        additionalData += " subscriptionId: " + subscriptionId + ";";

        ZendeskConfig.INSTANCE.setIdentity(anonymousIdentity);
        ZendeskConfig.INSTANCE.setContactConfiguration(new SampleFeedbackConfiguration());


//        String appVersion = String.format(Locale.US, "Version %s", BuildConfig.VERSION_NAME);
//        CustomField customFieldAppVersion = new CustomField(0L, appVersion);
//
//        String osVersion = String.format(Locale.US, "Android %s, Version %s", Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
//        CustomField customFieldOsVersion = new CustomField(1L, osVersion);

//        ZendeskConfig.INSTANCE.setCustomFields(Arrays.asList(customFieldAppVersion, customFieldOsVersion));

        Intent intent = new Intent(getActivity(), ContactZendeskActivity.class);
        startActivity(intent);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Log.e(TAG, "onAttach");
        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Log.e(TAG, "onDetach");
        mContext = null;
    }

    // Configures the Contact Zendesk component
    class SampleFeedbackConfiguration extends BaseZendeskFeedbackConfiguration {

        @Override
        public String getRequestSubject() {
            return "Feedback from Android app";
        }

        @Override
        public String getAdditionalInfo() {
            return additionalData;
        }


        @Override
        public List<String> getTags() {
            return Arrays.asList("Android", Build.VERSION.RELEASE, Build.MODEL);
        }
    }


}
