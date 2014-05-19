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
import com.google.gson.reflect.TypeToken;
import net.mafro.android.wakeonlan.MagicPacket;
import net.spooker.WakeOnLan.utils.Utils;
import org.javatuples.Quartet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    private static final Map<Quartet<String, String, String, Long>, ScheduledFuture> scheduledFuturesMap = new ConcurrentHashMap<Quartet<String, String, String, Long>, ScheduledFuture>();
    private SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();


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
                    Gson gson = new Gson();
                    Type quartet = new TypeToken<Quartet<String, String, String, Long>>()
                    {
                    }.getType();
                    final Quartet<String, String, String, Long> parameterObject = gson.fromJson((String) data.get("parameterObject"), quartet);
                    logInfo("parameterObject = " + parameterObject);

                    final String mac = parameterObject.getValue0();
                    final String ip = parameterObject.getValue1();
                    final String numberOfPacketsToSend = parameterObject.getValue2();
                    final Long when = parameterObject.getValue3();
                    final Long now = (Long) Calendar.getInstance().getTimeInMillis();
                    final Long delay = when - now;

                    if (delay >= 0)
                    {
                        final CountDownLatch latch = new CountDownLatch(1);
                        ScheduledFuture scheduledFuture = scheduledTaskExecutor.schedule(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    logInfo("scheduleFuture thread started . Thread.currentThread() = " + Thread.currentThread());
                                    latch.await(); //Synchronize with parent thread so that we can save it to storage and the map first
                                    MagicPacket.send(mac, ip); //Send a Magic Packet
                                    removeFromSharedPreferences(parameterObject); //remove from storage
                                    scheduledFuturesMap.remove(parameterObject); //remove from map
                                    logInfo("scheduleFuture thread ended . Thread.currentThread() = " + Thread.currentThread());
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                                catch (SocketException e)
                                {
                                    e.printStackTrace();
                                }
                                catch (UnknownHostException e)
                                {
                                    e.printStackTrace();
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }, delay, TimeUnit.MILLISECONDS);


                        addToSharedPreferences(parameterObject, null); //add to storage
                        scheduledFuturesMap.put(parameterObject, scheduledFuture); //add to map
                        latch.countDown();
                    } else
                    {
                        logInfo("Scheduled time for this event is in the past. Removing event from storage and map");
                        removeFromSharedPreferences(parameterObject); //remove from storage
                        scheduledFuturesMap.remove(parameterObject); //remove from map
                    }
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
        sharedPreferences = getSharedPreferences(getApplicationInfo().name, Context.MODE_PRIVATE);
        Utils.printSharedPreferences(sharedPreferences);

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
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
        logInfo("scheduledFuturesMap size " + scheduledFuturesMap.size());

        //read params that were passed to the intent's extras
        Type listOfQuartet = new TypeToken<List<Quartet<String, String, String, Long>>>()
        {
        }.getType();
        final List<Quartet<String, String, String, Long>> listOfParameterObjects = gson.fromJson((String) intent.getExtras().get("listOfParameterObjects"), listOfQuartet);

        for (Quartet<String, String, String, Long> parameterObject : listOfParameterObjects)
        {
            //convert to its String representation
            String parameterObjectString = gson.toJson(parameterObject);
            Bundle extras = new Bundle();
            extras.putString("parameterObject", parameterObjectString);

            // send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.setData(extras);
            mServiceHandler.sendMessage(msg);
        }


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

    private void addToSharedPreferences(Quartet<String, String, String, Long> key, String value)
    {

        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(gson.toJson(key), value);
        ed.commit();
    }

    private void removeFromSharedPreferences(Quartet<String, String, String, Long> key)
    {
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.remove(gson.toJson(key));
        ed.commit();
    }


}
