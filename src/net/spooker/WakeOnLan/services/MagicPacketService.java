package net.spooker.WakeOnLan.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
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
    private static final Map<Quintet, ScheduledFuture> scheduledFuturesMap = new ConcurrentHashMap<Quintet, ScheduledFuture>();
    private SharedPreferences mPrefs;


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
                try
                {
                    Bundle data = msg.getData();
                    final String mac = data.getString("mac");
                    final String ip = data.getString("ip");
                    final String numberOfPacketsToSend = data.getString("numberOfPacketsToSend");
                    final String delay = data.getString("delay");
                    final Date now = new Date();
                    final Quintet quintet = new Quintet(mac, ip, numberOfPacketsToSend, delay, now);

                    final CountDownLatch latch = new CountDownLatch(1);
                    ScheduledFuture scheduledFuture = scheduledTaskExecutor.schedule(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                logInfo("scheduleFuture thread started . Thread.currentThread() = " + Thread.currentThread());
                                latch.await();                                                                              //Synchronize with parent thread when CountDownLatch reaches 0
                                new SendWolPacketsTask(MagicPacketService.this).execute(mac, ip, numberOfPacketsToSend);
                                removeFromSharedPreferences(quintet);                                                       //remove from storage
                                scheduledFuturesMap.remove(quintet);                                                        //remove from map
                            }
                            catch (InterruptedException e)
                            {
                                logException("Exception in run", e);
                            }
                        }
                    }, Integer.parseInt(delay), TimeUnit.SECONDS);


                    addToSharedPreferences(quintet,null);                                                                   //add to storage
                    scheduledFuturesMap.put(quintet,scheduledFuture);                                                       //add to map
                    latch.countDown();
                }
                catch (Exception e)
                {
                    logException("Exception in handleMessage", e);
                }
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
        mPrefs = getSharedPreferences(getApplicationInfo().name, Context.MODE_PRIVATE);
        printSharedPreferences();

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

    private void printSharedPreferences()
    {
        Map<String, ?> all = mPrefs.getAll();
        Set<? extends Map.Entry<String, ?>> entrySet = all.entrySet();
        Iterator<? extends Map.Entry<String, ?>> iterator = entrySet.iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, ?> next = iterator.next();
            logInfo("next = "+next);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        logInfo("Start of onStartCommand()");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        logInfo("scheduledFuturesMap size " + scheduledFuturesMap.size());

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

    private void addToSharedPreferences(Quintet key,String value)
    {

        SharedPreferences.Editor ed=mPrefs.edit();
        Gson gson = new Gson();
        ed.putString(gson.toJson(key),null);
        ed.commit();
    }

    private void removeFromSharedPreferences(Quintet key)
    {
        SharedPreferences.Editor ed=mPrefs.edit();
        Gson gson = new Gson();
        Quintet quintet = gson.fromJson(gson.toJson(key), Quintet.class);
        ed.remove(gson.toJson(key));
        ed.commit();
    }
}
