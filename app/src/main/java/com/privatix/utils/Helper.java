package com.privatix.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.privatix.MainActivity;
import com.privatix.PremiumActivity;
import com.privatix.R;
import com.privatix.api.models.request.Device;
import com.privatix.api.models.request.Os;
import com.privatix.api.models.request.Software;
import com.privatix.api.models.request.UserActivation;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Lotar on 01.07.2016.
 */
public class Helper {
    public static UserActivation getUserActivationData(Context context) {
        Os os = new Os(context.getString(R.string.os_name), Build.VERSION.RELEASE, context.getString(R.string.os_family));
        Device device = new Device(Build.MODEL, os);
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        device.setDeviceId(androidId);
        Software software = new Software(context.getString(R.string.software_type), Build.VERSION.RELEASE);
        return new UserActivation(device, software);
    }

    public static String convertUnixTimeToDate(long unixSeconds) {
        Date date = new Date(unixSeconds * 1000L); // *1000 is to convert seconds to milliseconds
        DateFormat dateFormat = DateFormat.getDateInstance(); // the format of your date
        return dateFormat.format(date);
    }

    public static void startMainActivity(Context context, boolean bootStart) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.BOOT_START_EXTRA, bootStart);
        context.startActivity(intent);
    }

    public static void startPremiumActivity(Context context) {
        Intent intent = new Intent(context, PremiumActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
