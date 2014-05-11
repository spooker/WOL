package net.spooker.WakeOnLan.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Administrator on 11/5/2014.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver
{
private static final String TAG = "BootCompletedBroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent)
        {
            logInfo("onReceive started");
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
