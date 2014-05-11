package net.spooker.WakeOnLan.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import net.spooker.WakeOnLan.utils.Utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 11/5/2014.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver
{
private static final String TAG = "BootCompletedBroadcastReceiver";
    private SharedPreferences sharedPreferences;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            logInfo("onReceive started");
            sharedPreferences = context.getSharedPreferences(context.getApplicationInfo().name,Context.MODE_PRIVATE);

            Utils.printSharedPreferences(sharedPreferences); //print SharedPreferences

        }

        private void logInfo(String msg)
        {
            Log.i(TAG, msg);
        }

        private void logException(String msg, Exception e)
        {
            Log.e(TAG, msg, e);
        }

}
