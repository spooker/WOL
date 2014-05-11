package net.spooker.WakeOnLan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.*;
import android.widget.Button;
import android.widget.ProgressBar;
import net.spooker.WakeOnLan.services.MagicPacketService;

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
                final String numberOfPacketsToSend = "2";
                final String delay = "25";


                        Intent intent = new Intent(SendPacketsActivity.this, MagicPacketService.class);
                        intent.putExtra("mac", mac);
                        intent.putExtra("ip", ip);
                        intent.putExtra("numberOfPacketsToSend", numberOfPacketsToSend);
                        intent.putExtra("delay", delay);
                        startService(intent);



                //new SendWolPacketsTask(SendPacketsActivity.this).execute(mac,ip,numberOfPacketsToSend);
            }
        });
    }
}