package it.robertof.rpm.workers;

import java.net.URL;
import android.app.Activity;

public abstract class WorkerConfig
{
    protected URL[] url;
    protected Activity activity;
    protected String context; // used only by ScheduledImageChecker
    public WorkerConfig (URL target, Activity activity)
    {
        this.url = new URL[] { target };
        this.activity = activity;
    }
    
    public WorkerConfig (URL[] targets, Activity activity)
    {
        this.url = targets;
        this.activity = activity;
    }
    
    public abstract void onPostExecute (boolean result);
    // Used only from ScheduledImageChecker
    public void onError (String msg) {};
    public void setContext (String context)
    {
        this.context = context;
    }
}