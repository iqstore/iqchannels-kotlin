package ru.iqchannels.sdk;

public class Log {
    private static boolean LOGGING = true;

    public static void configure(boolean state) {
        synchronized(Log.class) {
            LOGGING = state;
        }
    }

    public static void d(String tag, String message) {
        if (LOGGING) android.util.Log.d(tag, message);
    }

    public static void i(String tag, String message) {
        d(tag, message);
    }

    public static void e(String tag, String message) {
        if (LOGGING) android.util.Log.e(tag, message);
    }
}
