package it.robertof.rpm.workers;

import it.robertof.rpm.R;
import it.robertof.rpm.utils.MethodUtils;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

public class ScheduledImageChecker
{
    private WorkerConfig mConfiguration;
    private boolean mStopped;
    private int mFailedAttempts = 0;
    private BroadcastReceiver mDownloadCompleteRecv;
    private BroadcastReceiver mNotifyClickRecv;
    public static ScheduledImageChecker currentInstance;
    
    public ScheduledImageChecker (WorkerConfig config)
    {
        this.mConfiguration = config;
        if (currentInstance != null)
            throw new RuntimeException ("currentInstance != null");
        currentInstance = this;
    }
    
    public void begin()
    {
        final ScheduledExecutorService serv = Executors.newSingleThreadScheduledExecutor();
        this.mStopped = false;
        serv.schedule (new Runnable() {
            public void run()
            {
                InputStream buffer = null;
                try
                {
                    HttpURLConnection conn = (HttpURLConnection) mConfiguration.url[0].openConnection();
                    conn.setReadTimeout (7500);
                    conn.setConnectTimeout (7500);
                    conn.setRequestMethod ("GET");
                    conn.setDoInput (true);
                    conn.connect();
                    if (conn.getResponseCode() != 200 && mFailedAttempts++ >= 2)
                    {
                        mConfiguration.onError ("Got response code: " + conn.getResponseCode());
                        destroy();
                        return;
                    }
                    mFailedAttempts = 0;
                    buffer = conn.getInputStream();
                    Scanner scanner = new Scanner (buffer).useDelimiter ("\\A");
                    String res = ( scanner.hasNext() ? scanner.next() : "" ).trim();
                    buffer.close();
                    if (!res.equals ("NOPE") && !res.equals ("OK"))
                    {
                        if (res.startsWith ("e:"))
                            res = res.substring (2);
                        mConfiguration.onError (res);
                        destroy();
                        return;
                    }
                    if (res.equals ("OK"))
                    {
                        // queue the download using the new
                        // downloadmanager API in gingerbread
                        // (thanks http://stackoverflow.com/a/3028660)
                        DownloadManager.Request request = new DownloadManager.Request (Uri.parse (mConfiguration.url[1].toString()));
                        request.setDescription (mConfiguration.activity.getString (R.string.download_desc));
                        request.setTitle (mConfiguration.activity.getString (R.string.download_title));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility (DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        SimpleDateFormat out = new SimpleDateFormat ("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
                        File dir = new File (Environment.getExternalStoragePublicDirectory (Environment.DIRECTORY_PICTURES), "RPM/");
                        dir.mkdirs();
                        request.setDestinationInExternalPublicDir (Environment.DIRECTORY_PICTURES,
                                "RPM/rpm-" + mConfiguration.context + "-" + out.format (new Date()) + ".jpg");
                        mDownloadCompleteRecv = new BroadcastReceiver() {
                            @Override
                            public void onReceive (Context context,
                                    Intent intent)
                            {
                                mConfiguration.onPostExecute (true);
                                destroy();
                            }
                        };
                        mNotifyClickRecv = new BroadcastReceiver() {
                            @Override
                            public void onReceive (Context context,
                                    Intent intent)
                            {
                                // TODO: do something useful
                                MethodUtils.displayToast (mConfiguration.activity, R.string.download_toast, Toast.LENGTH_SHORT);
                            }
                        };
                        mConfiguration.activity.registerReceiver (mDownloadCompleteRecv, new IntentFilter (DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                        mConfiguration.activity.registerReceiver (mNotifyClickRecv, new IntentFilter (DownloadManager.ACTION_NOTIFICATION_CLICKED));
                        ((DownloadManager) mConfiguration.activity.getSystemService (Context.DOWNLOAD_SERVICE)).enqueue (request);
                        mConfiguration.onPostExecute (false);
                        return;
                    }
                    if (!mStopped)
                        serv.schedule (this, 10, TimeUnit.SECONDS);
                }
                catch (Exception ex)
                {
                    mConfiguration.onError (ex.toString());
                    destroy();
                    return;
                }
            }
        }, 10, TimeUnit.SECONDS);
    }
    
    public void suspendExecution()
    {
        this.mStopped = true;
    }
    
    public void destroy()
    {
        if (this.mDownloadCompleteRecv != null)
            this.mConfiguration.activity.unregisterReceiver (this.mDownloadCompleteRecv);
        if (this.mNotifyClickRecv != null)
            this.mConfiguration.activity.unregisterReceiver (this.mNotifyClickRecv);
        this.mConfiguration = null;
        this.mStopped = true;
        currentInstance = null;
    }
}
