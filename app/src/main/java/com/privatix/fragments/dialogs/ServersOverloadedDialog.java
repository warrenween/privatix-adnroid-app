package com.privatix.fragments.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privatix.MainActivity;
import com.privatix.R;

import java.util.Random;


public class ServersOverloadedDialog extends DialogFragment implements View.OnClickListener {
    public static final String TAG = ServersOverloadedDialog.class.getSimpleName();
    public static final long ONE_SECOND = 1000;
    public static final String TTL = "ttl";
    TextView tv_premium, tv_watch_video, tv_queue_number;
    MainActivity mainActivity;
    int startAmount;
    Handler handler;
    Runnable runnable;
    int ttl = -1;
    int subtrahend;

    public ServersOverloadedDialog() {
        // Required empty public constructor
    }

    public static ServersOverloadedDialog newInstance(int ttl) {
        ServersOverloadedDialog serversOverloadedDialog = new ServersOverloadedDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(TTL, ttl);
        serversOverloadedDialog.setArguments(bundle);
        return serversOverloadedDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
        startAmount = generateStartsAmount();
        Bundle bundle = getArguments();
        if (bundle != null)
            ttl = bundle.getInt(TTL);

        subtrahend = startAmount / ttl;
        Log.d(TAG, "subtrahend " + subtrahend);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_server_overloaded, container, false);
        initView(view);


        setQueueNumber(startAmount);
        Log.d(TAG, "startAmount " + startAmount);
        startCountdownTimer();
        return view;
    }


    public void startCountdownTimer() {
        handler = new Handler();
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                int sub = generateSubtrahendAmount((int) Math.round(subtrahend * 1.3), (int) Math.round(subtrahend * 0.8));
                startAmount -= sub;
                Log.d(TAG, "sub " + sub + " ,startAmount " + startAmount);
                if (startAmount > 0) {
                    setQueueNumber(startAmount);
                    handler.postDelayed(runnable, ONE_SECOND);
                } else {
                    dismiss();
                    mainActivity.startVpnAfterAd();
                }
            }
        }, ONE_SECOND);
    }


    public void initView(View view) {
        tv_premium = (TextView) view.findViewById(R.id.tv_premium);
        tv_watch_video = (TextView) view.findViewById(R.id.tv_watch_video);
        tv_queue_number = (TextView) view.findViewById(R.id.tv_queue_number);

        tv_premium.setOnClickListener(this);
        tv_watch_video.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_premium:
                mainActivity.switchConnect.setChecked(false);
                mainActivity.startPremiumScreen();
                dismiss();
                break;
            case R.id.tv_watch_video:
                mainActivity.startVideoServersOverloaded();
                dismiss();
                break;
        }
    }

    public void setQueueNumber(int queueNumber) {
        SpannableString content = new SpannableString(String.valueOf(queueNumber));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tv_queue_number.setText(content);
    }

    public int generateSubtrahendAmount(int maxValue, int minValue) {
        Random r = new Random();
        return r.nextInt(maxValue - minValue) + minValue;
    }


    public int generateStartsAmount() {
        Random r = new Random();
        return r.nextInt(800 - 500) + 500;
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
        if (handler != null && runnable != null)
            handler.removeCallbacks(runnable);
    }

    //    @NonNull
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
//                .setTitle(getString(R.string.servers_overloaded_dialog_title))
//
//                .setView(R.layout.fragment_server_overloaded)
//                .setPositiveButton("Skip by watching a video",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int whichButton) {
//                                dismiss();
//                            }
//                        }
//                )
//                .setNegativeButton("Premium", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                })
//                .create();
//
//        dialog.setCanceledOnTouchOutside(true);
//
//        return dialog;
//    }


}
