package net.spooker.WakeOnLan.services;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import net.spooker.WakeOnLan.SendPacketsActivity;
import net.spooker.WakeOnLan.SendWolPacketsTask;
import org.javatuples.Quintet;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Administrator on 5/5/2014.
 */
public class MagicPacketService extends Service
{
    private static final String TAG = "MagicPacketService";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    //ScheduledExecutorService scheduledTaskExecutor = Executors.newScheduledThreadPool(5);
    private ScheduledExecutorService scheduledTaskExecutor = Executors.newSingleThreadScheduledExecutor();

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
            logInfo("Start of handleMessage");
            // Normally we would do some work here, like download a file.
            synchronized (this)
            {
                logInfo("Start of SYNC CODE in handleMessage");
                try
                {
                    Bundle data = msg.getData();
                    final String mac = data.getString("mac");
                    final String ip = data.getString("ip");
                    final String numberOfPacketsToSend = data.getString("numberOfPacketsToSend");
                    final String delay = data.getString("delay");
                    final Date now = new Date();
                    final Quintet quintet = new Quintet(mac, ip, numberOfPacketsToSend, delay, now);

                    ScheduledFuture scheduledFuture = scheduledTaskExecutor.schedule(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            logInfo("scheduleFuture thread started . Thread.currentThread() = " + Thread.currentThread());
                            new SendWolPacketsTask(MagicPacketService.this).execute(mac, ip, numberOfPacketsToSend);
                            logInfo("scheduleFuture thread ended . Thread.currentThread() = " + Thread.currentThread());
                        }
                    }, Integer.parseInt(delay), TimeUnit.SECONDS);
                }
                catch (Exception e)
                {
                    logException("Exception in handleMessage", e);
                }
                logInfo("End of SYNC CODE in handleMessage. msg.getData() = " + msg.getData());
            }

            logInfo("End of handleMessage()");
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            logInfo("Stopping service");
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate()
    {
        logInfo("Start of onCreate()");
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
        logInfo("End of onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        logInfo("Start of onStartCommand()");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        //Get extras from intent as Bundle to pass them to the message
        Bundle extras = intent.getExtras();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        logInfo("End of onStartCommand()");
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

    private void logException(String msg, Exception e)
    {
        Log.e(TAG, msg, e);
    }
}
