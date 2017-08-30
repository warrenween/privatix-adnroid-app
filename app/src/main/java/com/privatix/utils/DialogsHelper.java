package com.privatix.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;

import com.privatix.R;
import com.privatix.fragments.dialogs.SimpleCustomDialog;
import com.privatix.fragments.dialogs.SimpleDialog;

/**
 * Created by Lotar on 30.06.2016.
 */
public class DialogsHelper {
    public static void showFirstSuccessfulConnectionDialog(SharedPreferences sp, Context context, FragmentManager fragmentManager) {
        boolean alreadyShowed = sp.getBoolean(PrefKeys.FIRST_CONNECTION_DIALOG_SHOWED, false);
        if (!alreadyShowed) {
            try {
                SimpleCustomDialog errorDialog = SimpleCustomDialog.newInstance(context.getString(R.string.protection_on), context.getString(R.string.successful_connection_dialog_message));
                errorDialog.show(fragmentManager, SimpleCustomDialog.class.getSimpleName());
                sp.edit().putBoolean(PrefKeys.FIRST_CONNECTION_DIALOG_SHOWED, true).apply();
            } catch (IllegalStateException e) {
                sp.edit().putBoolean(PrefKeys.FIRST_CONNECTION_DIALOG_SHOWED, false).apply();
                e.printStackTrace();
            }

        }
    }


    public static void showSimpleDialog(FragmentManager fragmentManager, String title, String message) {
        SimpleDialog errorDialog = SimpleDialog.newInstance(title, message);
        try {
            errorDialog.show(fragmentManager, SimpleDialog.class.getSimpleName());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
