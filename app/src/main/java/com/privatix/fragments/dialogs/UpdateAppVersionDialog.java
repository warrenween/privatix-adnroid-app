package com.privatix.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.privatix.R;
import com.privatix.utils.PrefKeys;


public class UpdateAppVersionDialog extends DialogFragment implements View.OnClickListener {
    TextView tvPlayStore, tvDownloadApk, tvDismiss;
    SharedPreferences sp;
    Context context;


    public UpdateAppVersionDialog() {
        // Required empty public constructor
    }

    public static UpdateAppVersionDialog newInstance() {
        return new UpdateAppVersionDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        sp = context.getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.fragment_update_app_version_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(getString(R.string.update_app_dialog_title))
                .setMessage(getString(R.string.update_app_dialog_message))
                .setView(view)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        tvPlayStore = (TextView) view.findViewById(R.id.tvPlayStore);
        tvDownloadApk = (TextView) view.findViewById(R.id.tvDownloadApk);
        tvDismiss = (TextView) view.findViewById(R.id.tvDismiss);

        tvPlayStore.setOnClickListener(this);
        tvDownloadApk.setOnClickListener(this);
        tvDismiss.setOnClickListener(this);

        return dialog;
    }


    public void browseToUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvPlayStore:
                browseToUrl("market://details?id=" + context.getPackageName());
                dismiss();
                break;
            case R.id.tvDownloadApk:
                browseToUrl(context.getString(R.string.download_apk_link) + context.getString(R.string.download_apk_file_name));
                dismiss();
                break;
            case R.id.tvDismiss:
                int count = sp.getInt(PrefKeys.UPDATE_DIALOG_COUNT, 0);
                sp.edit().putInt(PrefKeys.UPDATE_DIALOG_COUNT, ++count).apply();
                dismiss();
                break;

        }
    }
}
