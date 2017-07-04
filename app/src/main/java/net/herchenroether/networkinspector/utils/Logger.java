package net.herchenroether.networkinspector.utils;

import android.util.Log;

/**
 * Simple class to output to logcat
 *
 * Created by Adam Herchenroether on 9/7/2016.
 */
public class Logger {
    public static final String TAG = "NetworkInspector";

    public static void info(String text) {
        Log.i(TAG, text);
    }

    public static void warn(String text) {
        Log.w(TAG, text);
    }

    public static void error(String text) {
        Log.e(TAG, text);
    }
}
