package com.privatix.fragments.dialogs;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.MainActivity;
import com.privatix.R;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.PrefKeys;

public class RatingDialog extends DialogFragment implements View.OnClickListener {
    public static final String ORIGINAL_COUNTRY = "originalCountry";
    Context mContext;
    float stars = 0;
    View rootView;
    TextView tvSubmit;
    TextView tvNotNow;
    RatingBar ratingBar;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String originalCountry;

    private Tracker mTracker;

    public static RatingDialog newInstance(String originalCountry) {
        RatingDialog ratingDialog = new RatingDialog();
        Bundle bundle = new Bundle();
        bundle.putString(ORIGINAL_COUNTRY, originalCountry);
        ratingDialog.setArguments(bundle);
        return ratingDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationClass application = (ApplicationClass) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
        originalCountry = getArguments().getString(ORIGINAL_COUNTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        rootView = inflater.inflate(R.layout.fragment_raiting_dialog, container, false);
        tvSubmit = (TextView) rootView.findViewById(R.id.submit);
        tvNotNow = (TextView) rootView.findViewById(R.id.not_now);
        ratingBar = (RatingBar) rootView.findViewById((R.id.rating_bar));


        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                stars = rating;
            }
        });

        tvNotNow.setOnClickListener(this);
        tvSubmit.setOnClickListener(this);
        sharedPreferences = getActivity().getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        editor = sharedPreferences.edit();
        if (v.getId() == R.id.submit) {
            if (stars <= 3) {
                //Event - rating 1,2,3 Star
                AnalyticsUtils.sendEventRating123(getActivity(), mTracker, originalCountry);

                HelpImproveDialog helpImproveDialog = HelpImproveDialog.newInstance();
                helpImproveDialog.show(getFragmentManager(), "improve");
            } else {
                //Event - rating 4,5 Star
                AnalyticsUtils.sendEventRating45(getActivity(), mTracker, originalCountry);

                editor.putBoolean(PrefKeys.NEED_SHOW_KEY, false);
                goToPlayMarket();
            }
        } else {
            if (mContext instanceof MainActivity) {
                int newCounter = sharedPreferences.getInt(PrefKeys.COUNTER_KEY, 0);
                editor.putInt(PrefKeys.COUNTER_KEY, ++newCounter);
                if (newCounter > 2) editor.putBoolean(PrefKeys.NEED_SHOW_KEY, false);
            }
        }
        editor.apply();
        dismiss();
    }

    private void goToPlayMarket() {
        final String appPackageName = getActivity().getPackageName(); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        mContext = null;
    }

}
