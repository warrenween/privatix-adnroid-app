package com.privatix.async;

import android.os.AsyncTask;

import org.strongswan.android.logic.TrustedCertificateManager;

/**
 * Created by Lotar on 30.06.2016.
 */

/**
 * Class that loads the cached CA certificates.
 */
public class LoadCertificatesTask extends AsyncTask<Void, Void, TrustedCertificateManager> {

    @Override
    protected TrustedCertificateManager doInBackground(Void... params) {
        return TrustedCertificateManager.getInstance().load();
    }

    @Override
    protected void onPostExecute(TrustedCertificateManager result) {
    }
}
