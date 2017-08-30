package com.privatix.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.privatix.ApplicationClass;
import com.privatix.model.SpeedCheckerCountyTable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ross on 06.05.16.
 */

public class SpeedCheckVpnService extends Service {

    //long fileSize;
    //long startTime;
    Timer timer;
    TimerTask timerTask;

    private LocalBinder binder = new LocalBinder();
    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationClass application = (ApplicationClass) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    void schedule(final String host, final String originalCountry, final String country) {
        //if (timerTask != null) timerTask.cancel();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mWifi.isConnected()) {
                    new CheckSpeed().execute(host, originalCountry, country);
                    if (timer != null) timer.cancel();
                }
//                else {
//                    Log.d("speed_check", "vpn:not_wife, repeat");
//                }
            }
        };

        //try to run check speed every 2 sec if at start wifi was disable
        timer.schedule(timerTask, 0, 2000);
    }

    public void startSpeedCheck(String host, String originalCountry, String country) {
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        schedule(host, originalCountry, country);
        Log.d("speed_check", "vpn: started Service");
    }

    public void stopSpeedCheck() {
        if (timer != null) timer.cancel();
        Log.d("speed_check", "vpn: stopped Service");
    }

    class CheckSpeed extends AsyncTask<String, Void, Boolean> {
        ByteArrayOutputStream byteArrayOutputStream;
        long startTime;
        long stopTime;
        String country;
        String originalCountry;
        int result;

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                //create url
                String stringUrl = "http://" + params[0] + "/4mb.bin?";

                originalCountry = params[1];
                country = params[2];

                //adding timestamp
                stringUrl += System.currentTimeMillis();

                Log.d("speed_check", "vpn: url: " + stringUrl);

                URL url = new URL(stringUrl);

                URLConnection connection = url.openConnection();
                connection.connect();

                startTime = System.currentTimeMillis();
                Log.d("speed_check", "vpn: startTime: " + new Date(startTime).toString());

                //int lengthOfFile = connection.getContentLength();
                //Log.d("speed_check", "vpn: fileSizeIn: " + lengthOfFile);
                //fileSize = lengthOfFile;

                InputStream inputStream = new BufferedInputStream(url.openStream(), 1024);

                byteArrayOutputStream = new ByteArrayOutputStream();
                //OutputStream outputStream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/4mb.bin");

                byte data[] = new byte[1024];

                //long total = 0;

                while ((inputStream.read(data)) != -1) {
                    //total += count;
                    try {
                        byteArrayOutputStream.write(data);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                    }
                    //outputStream.write(data, 0, count);
                    //Log.d("loading progress", ""+(int)((total*100)/lengthOfFile));
                }
                //outputStream.flush();
                //outputStream.close();

                byteArrayOutputStream.flush();
                byteArrayOutputStream.close();
                inputStream.close();

            } catch (IOException e) {
                Log.d("speed_check", "vpn: ERROR" + e.toString());
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success && byteArrayOutputStream != null) {
                stopTime = System.currentTimeMillis();
                Log.d("speed_check", "vpn: stopTime: " + new Date(stopTime).toString());

                double spendTime = (stopTime - startTime) / 1000d;

                Log.d("speed_check", "vpn: difference: " + spendTime + " sec");

                Log.d("speed_check", "vpn: byteArrayOutputStream: " + byteArrayOutputStream.size());
                Log.d("speed_check", "vpn: byteArrayOutputStream: result " + (byteArrayOutputStream.size() / 1024));
                double megaByteDownloaded = (byteArrayOutputStream.size() / 1024d) / 1024d;
                //double speedKbPerSeconds = (byteArrayOutputStream.size() / 1024d) / spendTime;
                double megabytePerSecond = megaByteDownloaded / spendTime;
                double megabitPerSecond = megabytePerSecond * 8d;
                Log.d("speed_check", "megaByteDownloaded " + megaByteDownloaded);
                Log.d("speed_check", "megabytePerSecond " + megabytePerSecond);
                Log.d("speed_check", "megabitPerSecond " + megabitPerSecond);

                result = (int) megabitPerSecond;

                if (result > 0) {
                    mTracker.setScreenName("protection_on");
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("speed")
                            .setAction("node")
                            .setLabel(originalCountry + "_" + country)
                            .setValue(result)
                            .build());

                    Log.d("speed_check", "vpn: Done, speed is: " + result);

                    SpeedCheckerCountyTable newCheckedCountry =
                            new SpeedCheckerCountyTable(originalCountry + "_" + country);
                    Log.d("speed_check", "vpn: country: " + originalCountry + "_" + country);
                    newCheckedCountry.save();


                    Log.d("speed_check", "vpn: stop_service");
                    stopSelf();
                }


            }
            super.onPostExecute(success);
        }
    }

    public class LocalBinder extends Binder {
        public SpeedCheckVpnService getService() {
            return SpeedCheckVpnService.this;
        }
    }
}
