package com.privatix.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.privatix.R;

import org.strongswan.android.logic.TrustedCertificateManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by Lotar on 01.07.2016.
 */
public class CertificateUtils {
    /**
     * Import the file pointed to by the given URI as a certificate.
     */
    public static void importCertificate(Activity activity, SharedPreferences sp) {
        X509Certificate certificate = parseCertificate(activity.getApplicationContext());
        if (certificate == null) {
            Toast.makeText(activity.getApplicationContext(), R.string.cert_import_failed, Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }
        if (storeCertificate(certificate, sp)) {
            Toast.makeText(activity.getApplicationContext(), R.string.cert_imported_successfully, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity.getApplicationContext(), "Store certificate failed", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load the file from the given URI and try to parse it as X.509 certificate.
     *
     * @return certificate or null
     */
    public static X509Certificate parseCertificate(Context context) {
        X509Certificate certificate = null;
        try {
            InputStream is;
            try {
                is = context.getAssets().open("ca.crt");
                //int size = is.available();
                //byte[] buffer = new byte[size]; //declare the size of the byte array with size of the file
                //is.read(buffer); //read file
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                certificate = (X509Certificate) factory.generateCertificate(is);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //CertificateFactory factory = CertificateFactory.getInstance("X.509");
            //InputStream in = getContentResolver().openInputStream(uri);

			/* we don't check whether it's actually a CA certificate or not */
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        return certificate;
    }

    /**
     * Try to store the given certificate in the KeyStore.
     *
     * @param certificate
     * @return whether it was successfully stored
     */
    public static boolean storeCertificate(X509Certificate certificate, SharedPreferences sp) {
        try {
            KeyStore store = KeyStore.getInstance("LocalCertificateStore");
            store.load(null, null);
            store.setCertificateEntry(null, certificate);
            TrustedCertificateManager.getInstance().reset();
            sp.edit().putBoolean(PrefKeys.CA_IMPORTED, true).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
