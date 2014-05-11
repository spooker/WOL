package net.spooker.WakeOnLan.utils;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 11/5/2014.
 */
public class Utils
{
    private static final String TAG = "Utils";

    public static void printSharedPreferences(SharedPreferences sharedPreferences)
    {
        Map<String, ?> all = sharedPreferences.getAll();
        Set<? extends Map.Entry<String, ?>> entrySet = all.entrySet();
        Iterator<? extends Map.Entry<String, ?>> iterator = entrySet.iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, ?> next = iterator.next();
            logInfo("next = " + next);
        }
    }

    private static void logInfo(String msg)
    {
        Log.i(TAG, msg);
    }

    private static void logException(String msg, Exception e)
    {
        Log.e(TAG, msg, e);
    }
}
