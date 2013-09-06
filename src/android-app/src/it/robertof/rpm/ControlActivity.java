package it.robertof.rpm;

import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.workers.ScheduledImageChecker;
import it.robertof.rpm.workers.ScheduledStatusCheck;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ControlActivity extends FragmentActivity {
    /* 
     * CONFIGURATION
     * You should edit just this. Nothing else.
     */
    // The path of trigger_action.php
    public static final String CFG_TRIGGER_URL = "http://example.com/trigger_action.php";
    // The SHA-1 hash of your 'app' password. It will also be
    // used as the password for the main login of the app.
    public static final String CFG_PASSWORD_HASH = "1234";
    /*
     * END OF CONFIGURATION
     * You can now recompile this app.
     * Enjoy.
     */
    public static ConnectivityManager sConnManager;
    public static String passwordUsed;
    public static ScheduledStatusCheck sStatusChecker;
    public static ViewPager pager;
    PagerAdapter adapter;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_control_pager);
        getActionBar().setDisplayHomeAsUpEnabled (true);
        passwordUsed = getIntent().getStringExtra (MainActivity.PWD_MSG_INTENT);
        if (passwordUsed == null)
        {
            MethodUtils.fatalErrorDialog (this, "Activity started without a password");
            return;
        }
        sConnManager = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        sStatusChecker = new ScheduledStatusCheck (this);
        pager = (ViewPager) findViewById (R.id.pager);
        adapter = new it.robertof.rpm.pages.PagerAdapter (this, getSupportFragmentManager());
        pager.setAdapter (adapter);
        sStatusChecker.begin();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate (R.menu.controlmenu, menu);
        return true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (sStatusChecker != null && sStatusChecker.initialized)
            sStatusChecker.stop (true);
        if (ScheduledImageChecker.currentInstance != null)
            ScheduledImageChecker.currentInstance.suspendExecution();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (sStatusChecker != null && !sStatusChecker.initialized)
            sStatusChecker.begin();
        if (ScheduledImageChecker.currentInstance != null)
            ScheduledImageChecker.currentInstance.begin();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (sStatusChecker != null && sStatusChecker.initialized)
            sStatusChecker.stop (true);
        if (ScheduledImageChecker.currentInstance != null)
            ScheduledImageChecker.currentInstance.destroy();
    }
    
    @Override
    public void finish()
    {
        if (sStatusChecker != null && sStatusChecker.initialized)
            sStatusChecker.stop (true);
        super.finish();
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask (this);
                return true;
            case R.id.about:
                MethodUtils.showAboutDialog (this);
                return true;
        }
        return super.onOptionsItemSelected (item);
    }
}
