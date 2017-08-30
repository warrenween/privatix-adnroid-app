package com.privatix.async;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Created by Lotar on 05.10.2016.
 */

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    public static final String TAG = DownloadImageTask.class.getSimpleName();
    private ImageView bmImage;
    private String linkToGo;
    private Context mContext;

    public DownloadImageTask(Context context, ImageView bmImage, String linkToGo) {
        this.bmImage = bmImage;
        this.linkToGo = linkToGo;
        this.mContext = context;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.e(TAG,"onPreExecute");
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        Log.e(TAG,"doInBackground");
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        Log.e(TAG,"onPostExecute");
        bmImage.setVisibility(View.VISIBLE);
        bmImage.setImageBitmap(result);
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebURL(linkToGo);
            }
        });
    }


    private void openWebURL(String inURL) {
        if (mContext != null) {
            Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(inURL));
            mContext.startActivity(browse);
        }
    }
}
