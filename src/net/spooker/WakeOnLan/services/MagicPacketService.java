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
    private final ScheduledExecutorService scheduledTaskExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final Map<ParameterObject, ScheduledFuture> scheduledFuturesMap = new ConcurrentHashMap<ParameterObject, ScheduledFuture>();
    private SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    /**
     * Factory method to make the desired Intent.
     */
    public static Intent makeIntent(Context context)
    {
        // Create the Intent that's associated to the DownloadService
        // class.
        Intent intent = new Intent(context, MagicPacketService.class);
        return intent;
    }

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
            final Semaphore mutex = new Semaphore(1, true);
            ScheduledFuture scheduledFuture = null;

            try
            {
                final Bundle data = msg.getData();
                final ParameterObject parameterObject = gson.fromJson((String) data.get("parameterObject"), ParameterObject.class);
                logInfo("parameterObject = " + parameterObject);

                final String mac = parameterObject.getMac();
                final String ip = parameterObject.getIp();
                final Integer numberOfPacketsToSend = parameterObject.getNumberOfPacketsToSend();
                final Long createdDt = parameterObject.getCreatedDt();
                final Long scheduledDt = parameterObject.getScheduledDt();
                final TimeUnit timeUnit = parameterObject.getTimeUnit();
                final Long delay = scheduledDt - createdDt;

                if (delay >= 0)
                {
                    logInfo("waiting to acquire the mutex .ThreadId = " + Thread.currentThread().getId());
                    mutex.acquire();
                    logInfo("mutex aquired .ThreadId = " + Thread.currentThread().getId());

                    final Runnable runnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            logInfo("scheduleFuture thread started . ThreadId = " + Thread.currentThread().getId());
                            try
                            {

                                logInfo("waiting to acquire the mutex .ThreadId = " + Thread.currentThread().getId());
                                mutex.acquire(); //Synchronize with parent thread so that we can save it to storage and the map first
                                logInfo("mutex aquired .ThreadId = " + Thread.currentThread().getId());

                                logInfo("Sending Magic Packets");
                                //Send a Magic Packet
                                MagicPacket.send(mac, ip);
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
                            finally
                            {
                                logInfo("Removing from SharedPreferences . ThreadId = " + Thread.currentThread().getId());
                                removeFromSharedPreferences(parameterObject); //remove from storage
                                logInfo("Removing from Map . ThreadId = " + Thread.currentThread().getId());
                                scheduledFuturesMap.remove(parameterObject); //remove from map

                                logInfo("releasing mutex . ThreadId = " + Thread.currentThread().getId());
                                mutex.release();
                            }
                            logInfo("scheduleFuture thread ended . ThreadId = " + Thread.currentThread().getId());
                        }
                    };

                    scheduledFuture = scheduledTaskExecutor.schedule(runnable, delay, timeUnit);
                    logInfo("Adding to SharedPreferences . ThreadId = " + Thread.currentThread().getId());
                    addToSharedPreferences(parameterObject, null); //add to storage
                    logInfo("Adding to Map = . ThreadId = " + Thread.currentThread().getId());
                    scheduledFuturesMap.put(parameterObject, scheduledFuture); //add to map

                } else
                {
                    logInfo("Scheduled time for this event is in the past.");
                    logInfo("Removing from SharedPreferences . ThreadId = " + Thread.currentThread().getId());
                    removeFromSharedPreferences(parameterObject); //remove from storage
                }
            }
            catch (Exception e)
            {
                if (scheduledFuture != null)
                {
                    scheduledFuture.cancel(true);
                    logInfo("Cancelled scheduledFuture");
                }
                logException("Exception in handleMessage", e);
            }
            finally
            {
                logInfo("releasing mutex . ThreadId = " + Thread.currentThread().getId());
                mutex.release();
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
        Type genericType = new TypeToken<List<ParameterObject>>()
        {
        }.getType();
        final List<ParameterObject> listOfParameterObjects = gson.fromJson((String) intent.getExtras().get("listOfParameterObjects"), genericType);

        for (ParameterObject parameterObject : listOfParameterObjects)
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

    private void addToSharedPreferences(ParameterObject key, String value)
    {

        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(gson.toJson(key), value);
        ed.commit();
    }

    private void removeFromSharedPreferences(ParameterObject key)
    {
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.remove(gson.toJson(key));
        ed.commit();
    }

    public static final class ParameterObject
    {
        private final String mac;
        private final String ip;
        private final Integer numberOfPacketsToSend;
        private final Long createdDt;
        private final Long scheduledDt;
        private final TimeUnit timeUnit;

        //Default private constructor will ensure no unplanned construction of class
        private ParameterObject(String mac, String ip, Integer numberOfPacketsToSend, Long createdDt, Long scheduledDt, TimeUnit timeUnit)
        {
            this.mac = mac;
            this.ip = ip;
            this.numberOfPacketsToSend = numberOfPacketsToSend;
            this.createdDt = createdDt;
            this.scheduledDt = scheduledDt;
            this.timeUnit = timeUnit;
        }

        //Factory method to store object creation logic in single place
        public static ParameterObject createNewInstance(String mac, String ip, Integer numberOfPacketsToSend, Long createdDt, Long scheduledDt, TimeUnit timeUnit)
        {
            return new ParameterObject(mac, ip, numberOfPacketsToSend, createdDt, scheduledDt, timeUnit);
        }

        public String getMac()
        {
            return mac;
        }

        public String getIp()
        {
            return ip;
        }

        public Integer getNumberOfPacketsToSend()
        {
            return numberOfPacketsToSend;
        }

        public Long getCreatedDt()
        {
            return createdDt;
        }

        public Long getScheduledDt()
        {
            return scheduledDt;
        }

        public TimeUnit getTimeUnit()
        {
            return timeUnit;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ParameterObject that = (ParameterObject) o;

            if (!createdDt.equals(that.createdDt)) return false;
            if (!ip.equals(that.ip)) return false;
            if (!mac.equals(that.mac)) return false;
            if (!numberOfPacketsToSend.equals(that.numberOfPacketsToSend)) return false;
            if (!scheduledDt.equals(that.scheduledDt)) return false;
            if (timeUnit != that.timeUnit) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = mac.hashCode();
            result = 31 * result + ip.hashCode();
            result = 31 * result + numberOfPacketsToSend.hashCode();
            result = 31 * result + createdDt.hashCode();
            result = 31 * result + scheduledDt.hashCode();
            result = 31 * result + timeUnit.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return "ParameterObject{" +
                    "mac='" + mac + '\'' +
                    ", ip='" + ip + '\'' +
                    ", numberOfPacketsToSend=" + numberOfPacketsToSend +
                    ", createdDt=" + createdDt +
                    ", scheduledDt=" + scheduledDt +
                    ", timeUnit=" + timeUnit +
                    '}';
        }
    }

}
