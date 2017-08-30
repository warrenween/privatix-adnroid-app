package com.privatix.fragments.dialogs;


import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.MainActivity;
import com.privatix.R;
import com.privatix.model.ProfileTable;
import com.privatix.utils.AnalyticsUtils;

import java.util.List;


public class TellWhatThinkDialog extends DialogFragment implements View.OnClickListener {
    MainActivity mainActivity;
    View rootView;
    TextView tvGood;
    TextView tvBad;
    String originalCountry;
    private Tracker mTracker;

    public static TellWhatThinkDialog newInstance() {
        return new TellWhatThinkDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationClass application = (ApplicationClass) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        rootView = inflater.inflate(R.layout.fragment_tell_what_thingh_dialog, container, false);
        tvGood = (TextView) rootView.findViewById(R.id.good);
        tvBad = (TextView) rootView.findViewById(R.id.bad);
        tvGood.setOnClickListener(this);
        tvBad.setOnClickListener(this);


        List<ProfileTable> profileTableList = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTableList.get(0);
        originalCountry = currentProfile.getOriginalCountry();

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bad) {
            //GA - rating bad answer
            AnalyticsUtils.sendEventBadRating(getActivity(), mTracker, originalCountry);
            showHelpImproveDialog();
        } else {
            //GA - rating good answer
            AnalyticsUtils.sendEventGoodRating(getActivity(), mTracker, originalCountry);
            showRatingDialog();

        }
        dismiss();
    }

    public void showHelpImproveDialog() {
        HelpImproveDialog helpImproveDialog = HelpImproveDialog.newInstance();
        helpImproveDialog.show(getFragmentManager(), HelpImproveDialog.class.getSimpleName());
    }


    public void showRatingDialog() {
        RatingDialog ratingDialog = RatingDialog.newInstance(originalCountry);
        ratingDialog.show(getFragmentManager(), RatingDialog.class.getSimpleName());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

}
