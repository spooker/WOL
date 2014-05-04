package net.spooker.WakeOnLan;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.Toast;
import net.mafro.android.wakeonlan.MagicPacket;

import java.io.IOException;

/**
 * Created by Administrator on 4/5/2014.
 */
public class SendWolPacketsTask extends AsyncTask<String, Integer, Integer>
{
    private Context context;
    ProgressDialog progressDialog;

    private int numberOfPacketsToSend;

    public SendWolPacketsTask(Context context)
    {
        this.context = context;
    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Packets are being sent");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.show();
    }

    @Override
    protected Integer doInBackground(String... params)
    {
        String mac = params[0];
        String ip = params[1];
        numberOfPacketsToSend = Integer.valueOf(params[2]);
        progressDialog.setMax(numberOfPacketsToSend);

        try
        {

            for (int i = 0; i <= numberOfPacketsToSend; i++)
            {
                MagicPacket.send(mac, ip);
                Thread.sleep(500);
                publishProgress(i);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }


        return numberOfPacketsToSend;
    }

    @Override
    protected void onProgressUpdate(Integer... values)
    {
        super.onProgressUpdate(values);
        //Update progressbar
        progressDialog.setProgress(values[0]);

    }

    @Override
    protected void onPostExecute(Integer result)
    {

        //Dismiss progressDialog
        progressDialog.dismiss();
        Toast.makeText(context,
                numberOfPacketsToSend + " packets were sent", Toast.LENGTH_LONG).show();
    }
}

