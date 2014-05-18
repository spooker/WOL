package net.spooker.WakeOnLan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.*;
import android.widget.Button;
import android.widget.ProgressBar;
import com.google.gson.Gson;
import net.spooker.WakeOnLan.services.MagicPacketService;
import org.javatuples.Quartet;
import org.javatuples.Quintet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 4/5/2014.
 */
public class SendPacketsActivity extends Activity
{
    ProgressDialog barProgressDialog;
    Button sendPacketsBtn;
    private final Gson gson = new Gson();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendpackets);
        sendPacketsBtn = (Button) findViewById(R.id.sendPacketsBtn);
        sendPacketsBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String mac = "70:71:bc:19:1b:c3";
                final String ip = "spooker.noip.me";
                final String numberOfPacketsToSend = "150";

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND,60);
                final Long when  = (Long) calendar.getTimeInMillis();

                //Create listOfParameterObjects
                final Quartet<String,String,String,Long> parameterObject = new Quartet<String,String,String,Long>(mac, ip, numberOfPacketsToSend, when);
                final List<Quartet<String,String,String,Long>> listOfParameterObjects = new ArrayList<Quartet<String,String,String,Long>>();
                listOfParameterObjects.add(parameterObject);

                //convert to its String representation
                String listOfParameterObjectsString = gson.toJson(listOfParameterObjects);

                //StartService
                Intent intent = new Intent(SendPacketsActivity.this, MagicPacketService.class);
                intent.putExtra("listOfParameterObjects",listOfParameterObjectsString);
                startService(intent);
            }
        });
    }
}