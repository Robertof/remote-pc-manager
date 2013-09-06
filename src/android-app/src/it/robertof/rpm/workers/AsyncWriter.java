package it.robertof.rpm.workers;

import it.robertof.rpm.R;
import it.robertof.rpm.utils.DialogBuilder;
import it.robertof.rpm.utils.MethodUtils;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Scanner;
import android.app.Activity;
import android.os.AsyncTask;

public class AsyncWriter
    extends AsyncTask<WorkerConfig, Void, Boolean>
{
    //private Service service;
    private WorkerConfig config;
    @Override
    protected Boolean doInBackground (WorkerConfig... params)
    {
        InputStream stream = null;
        //service = (Service) params[1];
        config = params[0];
        final Activity activity = config.activity;
        try
        {
            final HttpURLConnection conn = (HttpURLConnection) config.url[0].openConnection();
            conn.setReadTimeout (7500);
            conn.setConnectTimeout (7500);
            conn.setRequestMethod ("GET");
            conn.setDoInput (true);
            conn.connect();
            if (conn.getResponseCode() != 200)
            {
                activity.runOnUiThread (new Runnable() {
                    public void run() {
                        try
                        {
                            DialogBuilder.buildDialog (
                                    MethodUtils.mergeStrings (activity.getResources(),
                                            R.string.err_fail_conn, ": Got response code: ", Integer.toString (conn.getResponseCode())))
                                 .show (activity.getFragmentManager(), Double.toString (Math.random()));
                        }
                        catch (Exception ignored) {}
                    }
                });
                return false;
            }
            stream = conn.getInputStream();
            final Scanner scanner = new Scanner (stream).useDelimiter ("\\A");
            final String response = ( scanner.hasNext() ? scanner.next() : "" ).trim();
            if (!response.equals ("OK"))
            {
                activity.runOnUiThread (new Runnable() {
                    public void run() {
                        int index = 0;
                        if (response.startsWith ("e:"))
                            index = 2;
                        DialogBuilder.buildDialog (
                                MethodUtils.mergeStrings (activity.getResources(), R.string.err_generic, ": ", response.substring (index)))
                            .show (activity.getFragmentManager(), Double.toString (Math.random()));
                    }
                });
                return false;
            }
            return true;
        }
        catch (final Exception ex)
        {
            activity.runOnUiThread (new Runnable() {
                public void run() {
                    DialogBuilder.buildDialog (MethodUtils.mergeStrings (activity.getResources(), R.string.err_generic, ": ", ex.toString()))
                        .show (activity.getFragmentManager(), Double.toString (Math.random()));
                }
            });
        }
        finally
        {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    protected void onPostExecute (Boolean result)
    {
        config.onPostExecute (result);
        /*GeneralTogglePage page = service.associatedPage;
        if (page == null)
        {
            Log.w ("RemotePCManager", "onPostExecute with service " + service + " has no associated page");
            return;
        }
        page.workDialog.dismiss();
        ControlActivity.sStatusChecker.resume();
        if (!result)
            return;
        ToggleButton datBtn = (ToggleButton) page.view.findViewById (service.toggleBtnId);
        datBtn.setEnabled (false);
        ((TextView) page.view.findViewById (service.progressLayoutWatDoingLabel)).setText ((datBtn.isChecked() ? service.waitingOn : service.waitingOff));
        page.view.findViewById (service.progressLayoutId).setVisibility (View.VISIBLE);*/
    }
}
