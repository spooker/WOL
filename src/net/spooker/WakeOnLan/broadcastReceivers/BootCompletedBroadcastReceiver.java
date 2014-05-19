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
import org.javatuples.Quartet;
import org.javatuples.Quintet;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by Administrator on 11/5/2014.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver
{
private static final String TAG = "BootCompletedBroadcastReceiver";
    private SharedPreferences sharedPreferences;
    final Gson gson = new Gson();

        @Override
        public void onReceive(Context context, Intent intent)
        {
            logInfo("onReceive started");
            sharedPreferences = context.getSharedPreferences(context.getApplicationInfo().name,Context.MODE_PRIVATE);
            final List<MagicPacketService.ParameterObject> listOfParameterObjects = new ArrayList<MagicPacketService.ParameterObject>();

            Map<String, ?> all = sharedPreferences.getAll();
            Set<? extends Map.Entry<String, ?>> entrySet = all.entrySet();
            Iterator<? extends Map.Entry<String, ?>> iterator = entrySet.iterator();
            while (iterator.hasNext())
            {
                Map.Entry<String, ?> next = iterator.next();
                String parameterObjectJson = next.getKey();


                MagicPacketService.ParameterObject parameterObject = gson.fromJson(parameterObjectJson,MagicPacketService.ParameterObject.class);

                final String mac = parameterObject.getMac();
                final String ip = parameterObject.getIp();
                final Integer numberOfPacketsToSend = parameterObject.getNumberOfPacketsToSend();
                final Long createdDt = parameterObject.getCreatedDt();
                final Long scheduledDt = parameterObject.getScheduledDt();

                logInfo("=================");
                logInfo("mac = " + mac);
                logInfo("ip = " + ip);
                logInfo("numberOfPacketsToSend = " + numberOfPacketsToSend);
                logInfo("createdDt = " + createdDt);
                logInfo("scheduledDt = " + scheduledDt);

                //Create params
                listOfParameterObjects.add(parameterObject);
            }

            //convert to its String representation
            String paramsString = gson.toJson(listOfParameterObjects);

            //StartService
            Intent intent1 = new Intent(context, MagicPacketService.class);
            intent1.putExtra("listOfParameterObjects",paramsString);
            context.startService(intent1);
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
