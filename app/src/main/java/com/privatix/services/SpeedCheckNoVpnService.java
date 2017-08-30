package com.privatix.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.model.OriginalCountrySpeedCheckerTable;
import com.privatix.model.ProfileTable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ross on 06.05.16.
 */
public class SpeedCheckNoVpnService extends Service {
    String urlLink = "https://cdn.privatix.com/4mb.jpeg";

    long fileSize;
    long startTime;
    long stopTime;

    int result;

    Timer timer;
    TimerTask timerTask;

    LocalBinder binder = new LocalBinder();
    private Tracker mTracker;

    private boolean isError = false;

    @Override
    public void onCreate() {
        super.onCreate();

        ApplicationClass application = (ApplicationClass) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");

        timer = new Timer();
        schedule();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    void schedule() {
        if (timerTask != null) timerTask.cancel();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mWifi.isConnected()) {
                    new CheckSpeed().execute(urlLink);
                    if (timer != null) timer.cancel();
                }
//                else {
//                    Log.d("speed_test", "no_vpn: not_wife, repeat");
//                }
            }
        };

        //try to run check speed every 2 sec if at start wifi was disable
        timer.schedule(timerTask, 0, 2000);
    }

    class CheckSpeed extends AsyncTask<String, Void, Boolean> {
        ByteArrayOutputStream byteArrayOutputStream;

        @Override
        protected Boolean doInBackground(String... params) {
            int count;
            try {
                String stringUrl = params[0] + "?" + System.currentTimeMillis();
                Log.d("speed_test", "no_vpn: url: " + stringUrl);
                URL url = new URL(stringUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                startTime = System.currentTimeMillis();
                Log.d("speed_test", "no_vpn: loading start: " + startTime);

                int lengthOfFile = connection.getContentLength();
                Log.d("speed_test", "no_vpn: fileSizeIn: " + lengthOfFile);
                fileSize = lengthOfFile;

                InputStream inputStream = new BufferedInputStream(url.openStream(), 1024);

                byteArrayOutputStream = new ByteArrayOutputStream();
                //OutputStream outputStream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test.jpeg");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = inputStream.read(data)) != -1) {
                    total += count;
                    try {
                        byteArrayOutputStream.write(data);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        Toast.makeText(SpeedCheckNoVpnService.this, "Out of memory. Please clean some space.", Toast.LENGTH_LONG).show();
                        stopSelf();
                    }

                    //outputStream.write(data, 0, count);
                    //Log.d("loading progress", ""+(int)((total*100)/lengthOfFile));
                }
                /*outputStream.flush();
                outputStream.close();*/
                byteArrayOutputStream.flush();
                byteArrayOutputStream.close();
                inputStream.close();

            } catch (IOException e) {
                Log.d("speed_test", "no_vpn: ERROR" + e.toString());
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success && byteArrayOutputStream != null) {
                stopTime = System.currentTimeMillis();
                Log.d("speed_test", "no_vpn: stopTime: " + stopTime);

                double spendTime = (stopTime - startTime) / 1000d;

                Log.d("speed_test", "no_vpn: difference: " + spendTime + " sec");

                Log.d("speed_test", "no_vpn: downloadedFileSize: " + byteArrayOutputStream.size());

                //double speed = (byteArrayOutputStream.size() / 1024d) / spendTime;
                double megaByteDownloaded = (byteArrayOutputStream.size() / 1024d) / 1024d;
                double megabytePerSecond = megaByteDownloaded / spendTime;
                double megabitPerSecond = megabytePerSecond * 8d;
                Log.d("speed_test", "no_vpn: Download speed: " + megabitPerSecond + " Mbps");

                result = (int) megabitPerSecond;

                if (result > 0) {

                    List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);

                    mTracker.setScreenName("");
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("speed")
                            .setAction("cdn")
                            .setLabel(profileTables.get(0).getOriginalCountry())
                            .setValue(result)
                            .build());

                    OriginalCountrySpeedCheckerTable newChecked =
                            new OriginalCountrySpeedCheckerTable(profileTables.get(0).getOriginalCountry());
                    newChecked.save();

                    Log.d("speed_check", "no_vpn: Done, speed is: " + result);


                    Log.d("speed_test", "no_vpn: stop_service");
                    stopSelf();
                }
            }

            super.onPostExecute(success);
        }
    }

    public class LocalBinder extends Binder {

        public SpeedCheckNoVpnService getService() {
            return SpeedCheckNoVpnService.this;
        }
    }

}
