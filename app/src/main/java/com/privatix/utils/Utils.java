package com.privatix.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.widget.Toast;

import com.orm.SugarApp;

import java.util.regex.Pattern;

/**
 * Created by Lotar on 19.11.2015.
 */
public class Utils {
    private static final String EMAIL = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

    public static boolean isTabletMoreThanSevenInches(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (cm != null) {
            info = cm.getActiveNetworkInfo();
        }
        return info != null && info.isConnectedOrConnecting();

    }


    public static boolean isSecure(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return true;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) return true;
        return (config.wepKeys[0] != null);
    }


//    public static void cancelNotification(Context context, int id) {
//        NotificationManager mNotificationManager =
//                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.cancel(id);
//    }


    public static boolean isEmailValid(String email) {
        Pattern pattern = Pattern.compile(EMAIL);
        return pattern.matcher(email).matches();
    }

    public static boolean isPasswordValid(String password) {
        return password.length() >= 1;
    }


    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String isPackageInstalled(String[] packageNames, Context context) {
        String competitors = "";
        PackageManager pm = context.getPackageManager();

        for (String packageName : packageNames) {
            try {
                competitors += pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
                        + "_";
            } catch (PackageManager.NameNotFoundException ignored) {
            }

        }
        //remove spaces
        competitors = competitors.toLowerCase().replace(" ", "");
        //and remove last symbol _
        if (competitors.equals(""))
            return "";
        else
            return competitors.substring(0, competitors.length() - 1);
    }


    public static void showToast(String msg) {
        Toast.makeText(SugarApp.getSugarContext(), msg, Toast.LENGTH_SHORT).show();
    }


    public static void showToast(int resId) {
        Context context = SugarApp.getSugarContext();
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
    }


    public static String getAppVersion(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        assert pInfo != null;
        return pInfo.versionName;
    }


    public static Integer getAppVersionCode(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        assert pInfo != null;
        return pInfo.versionCode;
    }


}
