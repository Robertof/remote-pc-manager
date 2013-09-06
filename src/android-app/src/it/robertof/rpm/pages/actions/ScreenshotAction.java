package it.robertof.rpm.pages.actions;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.utils.DeactivableViewPager;
import it.robertof.rpm.utils.DialogBuilder;
import it.robertof.rpm.utils.HoloColorSequenceGenerator;
import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.utils.ServiceStrings.Service;
import it.robertof.rpm.utils.YesNoDialogBuilder;
import it.robertof.rpm.workers.AsyncWriter;
import it.robertof.rpm.workers.ScheduledImageChecker;
import it.robertof.rpm.workers.WorkerConfig;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.json.JSONArray;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ScreenshotAction implements Action
{
    protected final SparseIntArray tabColors = new SparseIntArray();
    private final HoloColorSequenceGenerator generator = new HoloColorSequenceGenerator();
    private int pageCounter = 1;
    private List<String> monitorInfo;
    private boolean doneDownloading;
    private boolean waitingForScreenshot;
    
    @Override
    public int getPageCount()
    {
        return this.pageCounter;
    }

    @Override
    public int getLayoutId()
    {
        return R.layout.action_screenshot;
    }

    @Override
    public void onInflate (View inflated, Activity activity, int page)
    {
        TextView text = (TextView) inflated.findViewById (R.id.screenshotViewText);
        TextView res  = (TextView) inflated.findViewById (R.id.screenshotMonitorRes);
        //System.out.println ("onInflate (child) called, doneDownloading " + this.doneDownloading + ", self hashCode " + this.hashCode ());
        int clr = tabColors.get (page, -1);
        if (clr == -1)
        {
            if (page == 0)
            {
                WebcamAction instance = (WebcamAction) ActionDB.WEBCAM.getInstance();
                if (page == 0 && instance.tabColors.indexOfKey (0) >= 0)
                    generator.addToAlreadyGenerated (instance.tabColors.get (0));
            }
            clr = generator.randomColor();
            tabColors.put (page, clr);
        }
        inflated.setBackgroundResource (clr);
        if (page == 0)
        {
            res.setVisibility (View.GONE);
            if (this.doneDownloading && !this.waitingForScreenshot)
            {
                text.setVisibility (View.VISIBLE);
                inflated.findViewById (R.id.screenshotProgressBar).setVisibility (View.GONE);
                text.setText (R.string.screenshot);
            }
            else
            {
                text.setVisibility (View.GONE);
                inflated.findViewById (R.id.screenshotProgressBar).setVisibility (View.VISIBLE);
                if (!this.doneDownloading)
                    new AsyncMonitorReader().execute (inflated, activity);
            }
        }
        else if (page == this.pageCounter - 1)
        {
            res.setVisibility (View.GONE);
            if (this.monitorInfo == null)
                text.setText (R.string.no_monitors);
            else
                text.setText (R.string.every_monitor);
        }
        else
        {
            if (this.monitorInfo == null)
                text.setText ("DAFAQ");
            else
            {
                text.setText ("Monitor " + page);
                res.setVisibility (View.VISIBLE);
                res.setText ("(" + this.monitorInfo.get (page - 1) + ")");
            }
        }
    }
    
    @Override
    public void handleClickEvent (final View view, final Activity activity, final int page)
    {
        if (page == 0)
            MethodUtils.displayToast (activity, ( this.doneDownloading && !this.waitingForScreenshot ?
                    R.string.wrong_page : R.string.wait_please ), Toast.LENGTH_SHORT);
        else
        {
            if (this.monitorInfo == null || ScheduledImageChecker.currentInstance != null ||
                    ControlActivity.sStatusChecker.lastMap == null ||
                    !ControlActivity.sStatusChecker.lastMap.containsKey (Service.PC) ||
                    !ControlActivity.sStatusChecker.lastMap.get (Service.PC).booleanValue())
            {
                MethodUtils.displayToast (activity, (this.monitorInfo == null ?
                        R.string.why_no_monitors :
                            ( ScheduledImageChecker.currentInstance != null ?
                                    R.string.download_already_queued :
                                    R.string.err_screenshot_pc_turned_off )), Toast.LENGTH_LONG);
                return;
            }
            final boolean lastPage = page == this.pageCounter - 1;
            final String msg = ( lastPage ?
                    activity.getString (R.string.screenshot_confirmation_evmon) :
                    activity.getString (R.string.screenshot_confirmation_monitor, page));
            YesNoDialogBuilder.buildDialog (msg, R.string.btn_yes, R.string.btn_undo, new DialogInterface.OnClickListener() {
                @Override
                public void onClick (DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    startAsyncWorker (view, activity, ( lastPage ? -1 : page - 1 ));
                }
            }).show (activity.getFragmentManager(), Double.toString (Math.random()));            
        }
    }
    
    private void startAsyncWorker (final View view, Activity activity, int monitor)
    {
        final Resources res = activity.getResources();
        final ProgressDialog dawg = ProgressDialog.show (activity, res.getString (R.string.loading),
                res.getString (R.string.wait_please), true, false);
        final WorkerConfig config = new WorkerConfig (MethodUtils.generateTriggerUrl ("do_action",
                "param=screenshot&monitor=" + monitor), activity) {
            @Override
            public void onPostExecute (boolean result)
            {
                dawg.dismiss();
                if (!result)
                    return;
                MethodUtils.displayToast (activity, R.string.waiting_for_screenshot, Toast.LENGTH_LONG);
                waitingForScreenshot = true;
                ActionDB.SCREENSHOT.getPager().setCurrentItem (0, true);
                ActionDB.SCREENSHOT.getPager().setAdapter (ActionDB.SCREENSHOT.getPager().getAdapter());
                ((DeactivableViewPager) ActionDB.SCREENSHOT.getPager()).setSwipable (false);
                // prepare the ScheduledImageChecker
                //onInflate (view, activity, 0); // redraw the page
                WorkerConfig schimgConf = new WorkerConfig (new URL[] { MethodUtils.generateTriggerUrl ("uploaded",
                        "context=screenshot"), MethodUtils.generateTriggerUrl ("get_uploaded_image", "context=screenshot")
                        }, activity) {
                    @Override
                    public void onPostExecute (boolean result)
                    {
                        // result is false if the download has been
                        // enqueued, true when the download is complete
                        if (!result)
                        {
                            MethodUtils.displayToast (activity, R.string.download_in_progress, Toast.LENGTH_LONG);
                            return;
                        }
                        // if completed, reset everything
                        waitingForScreenshot = false;
                        ActionDB.SCREENSHOT.getPager().setAdapter (ActionDB.SCREENSHOT.getPager().getAdapter());
                        ((DeactivableViewPager) ActionDB.SCREENSHOT.getPager()).setSwipable (true);
                        MethodUtils.displayToast (activity, R.string.download_completed, Toast.LENGTH_LONG);
                    }
                    
                    @Override
                    public void onError (final String message)
                    {
                        activity.runOnUiThread (new Runnable() {
                            public void run()
                            {
                                waitingForScreenshot = false;
                                ActionDB.SCREENSHOT.getPager().setAdapter (ActionDB.SCREENSHOT.getPager().getAdapter());
                                ((DeactivableViewPager) ActionDB.SCREENSHOT.getPager()).setSwipable (true);
                                DialogBuilder.buildDialog (MethodUtils.mergeStrings (activity.getResources(), R.string.err_generic, ": ", message))
                                    .show (activity.getFragmentManager(), Double.toString (Math.random()));
                            }
                        });
                    }
                };
                schimgConf.setContext ("screenshot");
                ScheduledImageChecker imgChecker = new ScheduledImageChecker (schimgConf);
                imgChecker.begin();
            }
        };
        new AsyncWriter().execute (config);
    }
    
    private class AsyncMonitorReader extends AsyncTask<Object, Void, List<String>>
    {
        private View inflated;
        
        @Override
        protected void onPreExecute()
        {
            ((DeactivableViewPager) ActionDB.SCREENSHOT.getPager()).setSwipable (false);
        }

        @Override
        protected List<String> doInBackground (Object... params)
        {
            inflated = (View) params[0];
            final Activity activity = (Activity) params[1];
            InputStream stream = null;
            try
            {
                final HttpURLConnection conn = (HttpURLConnection) MethodUtils.generateTriggerUrl ("enumerate_monitors").openConnection();
                conn.setReadTimeout (7500);
                conn.setConnectTimeout (7500);
                conn.setRequestMethod ("GET");
                conn.setDoInput (true);
                conn.connect();
                if (conn.getResponseCode() != 200)
                {
                    try
                    {
                        DialogBuilder.buildDialog (MethodUtils.mergeStrings (activity.getResources(),
                                R.string.err_fail_conn, ": Got response code: ", Integer.toString (conn.getResponseCode())))
                                .show (activity.getFragmentManager(), Double.toString (Math.random()));
                    } catch (Exception ignored) {}
                    return null;
                }
                stream = conn.getInputStream();
                final Scanner scanner = new Scanner (stream).useDelimiter ("\\A");
                final String response = ( scanner.hasNext() ? scanner.next() : "" ).trim();
                stream.close();
                if (!response.startsWith ("["))
                {
                    activity.runOnUiThread (new Runnable() {
                        public void run() {
                            int index = 0;
                            if (response.startsWith ("e:"))
                                index = 2;
                            DialogBuilder.buildDialog (MethodUtils.mergeStrings (activity.getResources(),
                                    R.string.err_generic, ": ", response.substring (index)))
                                    .show (activity.getFragmentManager(), Double.toString (Math.random()));
                        }
                    });
                    return null;
                }
                JSONArray arr = new JSONArray (response);
                List<String> retval = new ArrayList<String>();
                for (int i = 0; i < arr.length(); i++)
                    retval.add (arr.getString (i));
                return retval;
            }
            catch (final Exception ex)
            {
                activity.runOnUiThread (new Runnable() {
                    public void run()
                    {
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
            return null;
        }
        
        @Override
        protected void onPostExecute (List<String> result)
        {
            inflated.findViewById (R.id.screenshotProgressBar).setVisibility (View.GONE);
            TextView text = (TextView) inflated.findViewById (R.id.screenshotViewText);
            text.setVisibility (View.VISIBLE);
            text.setText (R.string.screenshot);
            ScreenshotAction.this.doneDownloading = true;
            if (result != null)
                ScreenshotAction.this.monitorInfo = result;
            ScreenshotAction.this.pageCounter = 1 + ( result != null ? result.size() + 1 : 1 );
            //System.out.println (ScreenshotAction.this.pageCounter);
            ActionDB.SCREENSHOT.getPager().getAdapter().notifyDataSetChanged();
            ((DeactivableViewPager) ActionDB.SCREENSHOT.getPager()).setSwipable (true);
        }
    }
}
