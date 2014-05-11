package net.spooker.WakeOnLan.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.spooker.WakeOnLan.SendPacketsActivity;
import net.spooker.WakeOnLan.services.MagicPacketService;
import net.spooker.WakeOnLan.utils.Utils;
import org.javatuples.Quintet;

import java.lang.reflect.Type;
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

            Map<String, ?> all = sharedPreferences.getAll();
            Set<? extends Map.Entry<String, ?>> entrySet = all.entrySet();
            Iterator<? extends Map.Entry<String, ?>> iterator = entrySet.iterator();
            while (iterator.hasNext())
            {
                Map.Entry<String, ?> next = iterator.next();
                String quintetJson = next.getKey();

                Gson gson = new Gson();
                Type quintetType = new TypeToken<Quintet<String,String,String,Long,Long>>() {}.getType();
                Quintet<String,String,String,Long,Long> quintet = gson.fromJson(quintetJson,quintetType);

                final String mac = quintet.getValue0();
                final String ip = quintet.getValue1();
                final String numberOfPacketsToSend = quintet.getValue2();
                final Long when = quintet.getValue3();

                logInfo("=================");
                logInfo("mac = " + mac);
                logInfo("ip = " + ip);
                logInfo("numberOfPacketsToSend = " + numberOfPacketsToSend);
                logInfo("when = " + when);

                Intent intent1 = new Intent(context, MagicPacketService.class);
                intent1.putExtra("mac", mac);
                intent1.putExtra("ip", ip);
                intent1.putExtra("numberOfPacketsToSend", numberOfPacketsToSend);
                intent1.putExtra("when", when);
                context.startService(intent1);
            }



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
