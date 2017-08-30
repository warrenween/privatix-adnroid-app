package com.privatix.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.privatix.model.SubscriptionTable;
import com.privatix.services.StartVpnService;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.List;

/**
 * Created by Lotar on 05.08.2016.
 */
public class BootReceiver extends BroadcastReceiver {
    public static final String TAG = BootReceiver.class.getSimpleName();
    VpnProfile vpnProfile;
    Context mContext;
    List<SubscriptionTable> subscriptionTables;
    SubscriptionTable mCurrentTable;
    private VpnProfileDataSource mDataSource;
    private BroadcastReceiver mNetworkStateCheckListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.e(TAG, "onReceive");
            context.startService(new Intent(context, StartVpnService.class));

//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    startVpnIfNeeded();
//                    mDataSource.close();
//                }
//            }, 60000);
            //startCheckReceiver();

            Log.e(TAG, "End work");
        }
    }


}
