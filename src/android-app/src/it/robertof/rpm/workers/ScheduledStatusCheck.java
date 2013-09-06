package it.robertof.rpm.workers;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.utils.ServiceStrings.Service;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Handles the 'alive' action from the remote server.
 * When initialized with begin(), this class will check
 * each 5 seconds if any service changes its status.
 * It will change the onChecked option accordingly.
 * ** NOTE ** It is important to destroy the StatusChecker
 * before the app is closed or pause by calling stop(). 
 * @author Robertof
 *
 */
public class ScheduledStatusCheck
{
    private Future<?> task;
    private boolean temporaryHalt;
    private Activity activity;
    public boolean initialized;
    private final URL triggerURL = MethodUtils.generateTriggerUrl ("alive");
    public Map<Service, Boolean> lastMap;
    
    public ScheduledStatusCheck (Activity act)
    {
        this.activity = act;
    }
    
    /**
     * Starts this ScheduledStatusCheck.
     * @throws java.lang.RuntimeException if this ScheduledStatusCheck
     * is already running.
     */
    public void begin()
    {
        //System.out.println ("begin() called");
        if (this.initialized)
            throw new RuntimeException ("begin() called while initialized is true");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        this.temporaryHalt = false;
        this.initialized = true;
        this.task = scheduler.scheduleAtFixedRate (new Runnable() {
            public void run() {
                if (temporaryHalt) return;
                InputStream recvData = null;
                //System.out.println ("StatusChecker running");
                try
                {
                    HttpURLConnection conn = (HttpURLConnection) triggerURL.openConnection();
                    conn.setReadTimeout (7500);
                    conn.setConnectTimeout (7500);
                    conn.setRequestMethod ("GET");
                    conn.setDoInput (true);
                    conn.connect();
                    if (conn.getResponseCode() != 200)
                    {
                        MethodUtils.displayToast (activity, Toast.LENGTH_LONG, R.string.err_fail_conn, ":", Integer.toString (conn.getResponseCode()));
                        return;
                    }
                    recvData = conn.getInputStream();
                    Scanner readScanner = new Scanner (recvData).useDelimiter ("\\A");
                    String response = ( readScanner.hasNext() ? readScanner.next() : "" ).trim();
                    recvData.close();
                    if (!response.startsWith ("{"))
                    {
                        MethodUtils.displayToast (activity, Toast.LENGTH_LONG, R.string.err_generic, ": Server sent: ", response);
                        return;
                    }
                    if (temporaryHalt) return;
                    Map<Service, Boolean> data = MethodUtils.parseStatusJson (response);
                    if (data == null)
                    {
                        MethodUtils.displayToast (activity, Toast.LENGTH_LONG, R.string.err_generic, ": Invalid JSON: ", response);
                        return;
                    }
                    lastMap = data;
                    for (Map.Entry<Service, Boolean> mappings : lastMap.entrySet())
                    {
                        final Service datService = mappings.getKey();
                        final boolean value = mappings.getValue();
                        if (datService.associatedPage != null)
                        {
                            final View mainView = datService.associatedPage.view;
                            if (mainView == null) continue;
                            final ToggleButton tb = (ToggleButton) mainView.findViewById (datService.toggleBtnId);
                            if (tb == null || (tb.isEnabled() && tb.isChecked() == value))
                                continue;
                            tb.post (new Runnable() {
                                @Override
                                public void run ()
                                {
                                    if (tb.isEnabled() && tb.isChecked() != value)
                                        tb.setChecked (value);
                                    else if (!tb.isEnabled() && tb.isChecked() == value)
                                    {
                                        tb.setEnabled (true);
                                        mainView.findViewById (datService.progressLayoutId).setVisibility (View.GONE);
                                    }
                                }
                            });
                        }
                        else
                            Log.w ("RemotePCManager", "Got a service which isn't in our map (probably uninitialized page): " + datService);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    if (!MethodUtils.checkConnection (ControlActivity.sConnManager))
                    {
                        MethodUtils.fatalErrorDialog (activity, R.string.err_no_conn);
                        stop (false);
                        return;
                    }
                    MethodUtils.displayToast (activity, Toast.LENGTH_LONG, R.string.err_fail_conn, ": ", ex.toString());
                }
                finally
                {
                    try {
                        if (recvData != null)
                            recvData.close();
                    } catch (Exception ignored) {}
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Stops, without halting the thread, this
     * ScheduledStatusCheck. Calls to pause()
     * while the ScheduledStatusCheck is already paused
     * will be ignored.
     */
    public void pause()
    {
        this.temporaryHalt = true;
    }
    
    /**
     * Resumes, if previously paused with {@link #pause()}, this
     * ScheduledStatusCheck.
     */
    public void resume()
    {
        this.temporaryHalt = false;
    }
    
    /**
     * Returns the status of this ScheduledStatusCheck.
     * @return True if it is running (i.e, not paused with {@link #pause()})
     */
    public boolean isRunning()
    {
        return this.temporaryHalt;
    }
    
    /**
     * Destroys this ScheduledStatusCheck.
     * @param force True if you want the thread to be immediately stopped, without
     * waiting for its task to complete.
     * @throws java.lang.RuntimeException If this ScheduledStatusCheck is not running.
     */
    public void stop (boolean force)
    {
        //System.out.println ("stop() called");
        if (this.task == null)
            throw new RuntimeException ("stop() called while task is null");
        this.task.cancel (force);
        this.task = null;
        this.initialized = false;
    }
}