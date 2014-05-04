package net.spooker.WakeOnLan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.*;
import android.widget.Button;
import net.mafro.android.wakeonlan.MagicPacket;

import java.io.IOException;

public class StartUpActivity extends Activity
{
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button goToSendPacketsBtn = (Button) findViewById(R.id.goToSendPacketsBtn);
        goToSendPacketsBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(StartUpActivity.this,SendPacketsActivity.class);
                startActivity(intent);
            }
        });

    }
}
