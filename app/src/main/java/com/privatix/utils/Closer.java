package com.privatix.utils;

import android.util.Log;

import java.io.Closeable;
import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Created by Lotar on 17.10.2016.
 */

public class Closer {
    public static void closeSilently(Object... xs) {
        // Note: on Android API levels prior to 19 Socket does not implement Closeable
        for (Object x : xs) {
            if (x != null) {
                try {
                    Log.d("Closer", "closing: "+x);
                    if (x instanceof Closeable) {
                        ((Closeable)x).close();
                    } else if (x instanceof Socket) {
                        ((Socket)x).close();
                    } else if (x instanceof DatagramSocket) {
                        ((DatagramSocket)x).close();
                    } else {
                        Log.d("Closer", "cannot close: "+x);
                        throw new RuntimeException("cannot close "+x);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
