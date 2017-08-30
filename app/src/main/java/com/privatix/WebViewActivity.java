package com.privatix;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.privatix.utils.Utils;

public class WebViewActivity extends Activity {
    public static final String URL = "url";
    WebView webView;
    String mUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!Utils.isTabletMoreThanSevenInches(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.fragment_webview);
        if (null != intent) {
            mUrl = intent.getStringExtra(URL);
        }
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(mUrl);
    }
}
