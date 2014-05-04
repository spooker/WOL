package net.spooker.WakeOnLan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.*;
import android.widget.Button;
import android.widget.ProgressBar;

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

                new SendWolPacketsTask(SendPacketsActivity.this).execute("70:71:bc:19:1b:c3","spooker.noip.me","20");
            }
        });
    }
}