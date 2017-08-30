package com.privatix.fragments.dialogs;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.privatix.R;

/**
 * Created by ross on 03.05.16.
 */
public class SimpleCustomDialog extends DialogFragment {
    public static final String DIALOG_TITLE = "dialog_title";
    public static final String DIALOG_MESSAGE = "dialog_message";
    String mTitle, mMessage;


    public SimpleCustomDialog() {
        // Required empty public constructor
    }

    public static SimpleCustomDialog newInstance(String title, String message) {
        SimpleCustomDialog fragment = new SimpleCustomDialog();
        Bundle bundle = new Bundle();
        bundle.putString(DIALOG_TITLE, title);
        bundle.putString(DIALOG_MESSAGE, message);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mTitle = bundle.getString(DIALOG_TITLE);
            mMessage = bundle.getString(DIALOG_MESSAGE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog;

        dialog = new AlertDialog.Builder(getActivity())
                .setView(R.layout.dialog_first_connection)
                .setTitle(mTitle)
                .setPositiveButton("ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        }
                )
                .create();

        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        TextView tv = (TextView) getDialog().findViewById(android.R.id.message);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }
}

