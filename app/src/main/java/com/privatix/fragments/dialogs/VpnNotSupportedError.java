package com.privatix.fragments.dialogs;

/**
 * Created by Lotar on 30.06.2016.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.privatix.R;

/**
 * Class representing an error message which is displayed if VpnService is
 * not supported on the current device.
 */
public class VpnNotSupportedError extends DialogFragment {
    static final String ERROR_MESSAGE_ID = "org.strongswan.android.VpnNotSupportedError.MessageId";

    public static void showWithMessage(Activity activity, int messageId) {
        Bundle bundle = new Bundle();
        bundle.putInt(ERROR_MESSAGE_ID, messageId);
        VpnNotSupportedError dialog = new VpnNotSupportedError();
        dialog.setArguments(bundle);
        dialog.show(activity.getFragmentManager(), VpnNotSupportedError.class.getSimpleName());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final int messageId = arguments.getInt(ERROR_MESSAGE_ID);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.vpn_not_supported_title)
                .setMessage(messageId)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).create();
    }
}
