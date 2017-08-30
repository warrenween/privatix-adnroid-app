package com.privatix.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.privatix.services.UpdateService;

/**
 * Created by Lotar on 10.07.2016.
 */
public class AlarmUtils {
    public static final String ALARM_ACTION = "com.privatix.update_service";
    public static final long ALARM_REPEAT_TIME = 15 * Constants.ONE_MINUTE;
    Context mContext;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;


    public AlarmUtils(Context context) {
        mContext = context;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void setAlarm(boolean isNeedUpdate) {
        alarmIntent = getUpdateServiceIntent(isNeedUpdate);
        if (alarmIntent != null)
            createAlarm(alarmIntent);
    }


    public PendingIntent getUpdateServiceIntent(boolean isNeedUpdate) {
        Intent intent = new Intent(mContext, UpdateService.class);
        intent.setAction(ALARM_ACTION);
        PendingIntent alarmIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null && !isNeedUpdate) {
            Log.d("AlarmUtils", "alarm not null");
            return null;
        }
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void createAlarm(PendingIntent alarmIntentUpdateCurrent) {
        long startTime = System.currentTimeMillis() + ALARM_REPEAT_TIME;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, alarmIntentUpdateCurrent);
        } else {
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, startTime,
                    ALARM_REPEAT_TIME, alarmIntentUpdateCurrent);
        }
    }


    public void cancelAlarm() {
        alarmIntent = getUpdateServiceIntent(false);
        if (alarmIntent != null)
            alarmMgr.cancel(alarmIntent);
    }
}
