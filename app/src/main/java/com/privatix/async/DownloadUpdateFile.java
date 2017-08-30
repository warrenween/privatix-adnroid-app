package com.privatix.async;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.privatix.utils.PrefKeys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Lotar on 12.10.2016.
 */

public class DownloadUpdateFile extends AsyncTask<String, Void, String> {
    public static final String TAG = DownloadUpdateFile.class.getSimpleName();
    private Context mContext;
    private SharedPreferences sp;

    public DownloadUpdateFile(Context context) {
        mContext = context;
        sp = context.getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, Context.MODE_PRIVATE);


    }

    @Override
    protected String doInBackground(String... params) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        String urlLink = params[0] + "?" + System.currentTimeMillis();
        Log.d(TAG, urlLink);
        try {
            URL url = new URL(urlLink);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            Log.d(TAG, "fileLength  " + fileLength);
            // download the file
            input = connection.getInputStream();
            output = mContext.openFileOutput("update.txt", Context.MODE_PRIVATE);

            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null)
            Log.e(TAG, "Download error: " + result);
        else {
            String appVersionPlayMarket = getTextFromFile();
            Log.d(TAG, "File download successfully " + appVersionPlayMarket);
            float appVersion = 0.0f;
            try {
                appVersion = Float.valueOf(appVersionPlayMarket);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            //Log.d(TAG, "File download successfully and saved " + appVersion);
            sp.edit().putFloat(PrefKeys.PLAY_MARKET_VERSION, appVersion).apply();
        }
    }


    private String getTextFromFile() {
        File file = new File(mContext.getFilesDir(), "update.txt");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
}
