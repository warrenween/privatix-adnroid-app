package com.privatix.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.widget.TextView;

import com.privatix.R;


public class DeviceLimitDialog extends DialogFragment {


    public DeviceLimitDialog() {
        // Required empty public constructor
    }

    public static DeviceLimitDialog newInstance() {
        return new DeviceLimitDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.device_limit_reached_title))
                .setMessage(getString(R.string.device_limit_reached_text))
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
//        SpannableString s =
//                new SpannableString(getString(R.string.device_limit_reached_text));
//        Linkify.addLinks(s, Linkify.WEB_URLS);

        TextView tv = (TextView) getDialog().findViewById(android.R.id.message);
//        SpannableString ss = new SpannableString( getString(R.string.device_limit_reached_text));
//        ClickableSpan clickableSpanTermOfService = new ClickableSpan() {
//            @Override
//            public void onClick(View widget) {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://privatix.com"));
//                startActivity(intent);
//            }
//        };
//        ss.setSpan(clickableSpanTermOfService, 11, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setGravity(Gravity.RIGHT);
        //tv.setText("");
        //tv.setText(Html.fromHtml("<a href=\'http://www.nip.org.np\'>See more</a>"));
        //tv.setMovementMethod(LinkMovementMethod.getInstance());
    }


}
