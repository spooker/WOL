package net.spooker.WakeOnLan.services;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import net.spooker.WakeOnLan.SendPacketsActivity;
import net.spooker.WakeOnLan.SendWolPacketsTask;

/**
 * Created by Administrator on 5/5/2014.
 */
public class MagicPacketService extends Service
{
    private static final String TAG = "MagicPacketService";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler
    {
        public ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.


            synchronized (this)
            {
                try
                {
                    Bundle data = msg.getData();
                    String mac = data.getString("mac");
                    String ip = data.getString("ip");
                    String numberOfPacketsToSend = data.getString("numberOfPacketsToSend");
                    new SendWolPacketsTask(MagicPacketService.this).execute(mac, ip, numberOfPacketsToSend);
                }
                catch (Exception e)
                {
                    logException("Exception in handleMessage",e);
                }
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate()
    {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

//        String mac = intent.getStringExtra("mac");
//        String ip = intent.getStringExtra("ip");
//        String numberOfPacketsToSend = intent.getStringExtra("numberOfPacketsToSend");
//        logInfo("mac = "+mac);
//        logInfo("ip = "+ip);
//        logInfo("numberOfPacketsToSend = "+numberOfPacketsToSend);

        //Get extras from intent as Bundle to pass them to the message
        Bundle extras = intent.getExtras();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void logInfo(String msg)
    {
        Log.i(TAG, msg);
    }
    private void logException(String msg,Exception e)
    {
        Log.e(TAG, msg, e);
    }
}
