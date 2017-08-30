package com.privatix.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.privatix.MainActivity;
import com.privatix.PremiumActivity;
import com.privatix.R;

/**
 * Created by Lotar on 06.07.2016.
 */
public class NotificationUtils {
    static Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


    public static void showNotification(Context context, String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_shield_status_bar)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text));
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);


        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntentUpdate =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntentUpdate);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // NOTIFY_ID_RISK allows you to update the notification later on.
        mNotificationManager.notify(Constants.NOTIFY_ID_CONNECTION_LOST, mBuilder.build());
    }

    public static void closeNotification(Context context, int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

    public static android.app.Notification getVpnStatusNotification(Context context,
                                                                    NotificationCompat.Builder builder,
                                                                    String text, int smallIcon) {
        builder
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setAutoCancel(false)
                .setSmallIcon(smallIcon);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setAction("com.privatix.action.ad.main");

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntentUpdate =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        builder.setContentIntent(resultPendingIntentUpdate);

        // NOTIFY_ID_RISK allows you to update the notification later on.
        android.app.Notification notification = builder.build();

        notification.flags |= android.app.Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    public static void showAdNotification(Context context, int id, String text, int smallIcon) {
        if (text == null || TextUtils.isEmpty(text)) {
            return;
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text));

        switch (smallIcon) {
            case Constants.NOTIFICATION_SMALL_ICON_START_RATE:
                mBuilder.setSmallIcon(R.drawable.ic_star_rate_white_18dp);
                break;
            case Constants.NOTIFICATION_SMALL_ICON_REPORT:
                mBuilder.setSmallIcon(R.drawable.ic_report_white_18dp);
                break;
        }


        // Creates an explicit intent for an Activity in your app
        Intent resultIntent;
        if (id == Constants.NOTIFY_ID_DISCONNECTED) {
            resultIntent = new Intent(context, MainActivity.class);
            resultIntent.setAction("com.privatix.action.ad.main");
        } else {
            resultIntent = new Intent(context, PremiumActivity.class);
            resultIntent.setAction("com.privatix.action.ad.premium");
        }


        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_NO_CREATE
//                );
//        if (resultPendingIntent != null){
//            Log.e(TAG, "resultPendingIntent not null");
//            return;
//        }
        PendingIntent resultPendingIntentUpdate =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntentUpdate);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // NOTIFY_ID_RISK allows you to update the notification later on.
        android.app.Notification notification = mBuilder.build();
        notification.flags |= android.app.Notification.FLAG_ONLY_ALERT_ONCE;
        mNotificationManager.notify(id, notification);
    }
}
