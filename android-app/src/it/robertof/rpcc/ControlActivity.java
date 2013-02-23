package it.robertof.rpcc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ControlActivity extends Activity {
    private boolean ignoreEv = false;
    private final String KEEPALIVE_URL = "http://INSERTyourwebsite.com/dIsAlive.php";
    private final String TRIGGER_STATUS_CHANGE_URL = "http://INSERTyourwebsite.com/dTriggerStatusChange.php?key=%s&val=%s";
    private String passwordUsed;
    private CheckPCStateRecurringTask currentTask;
    private ProgressDialog workingProgressDialog;
    private String twPwd = "** INSERT YOUR TEAMVIEWER PASSWORD HERE **";
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_control);
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled (true);
        passwordUsed = getIntent().getStringExtra (MainActivity.PWD_MSG_INTENT);
        final ToggleButton toggle = (ToggleButton) this.findViewById (R.id.toggleButton1);
        final ConnectivityManager connMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        if (!this.checkConnection (connMgr))
        {
            this.noConnectionWindow();
            return;
        }
        currentTask = new CheckPCStateRecurringTask();
        currentTask.startThread();
        //this.toastUtil ("PC status thread started.");
        toggle.setOnCheckedChangeListener (new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged (CompoundButton buttonView, final boolean isChecked) {
                if (ignoreEv)
                {
                    ignoreEv = false;
                    return;
                }
                //final ProgressDialog dialog = ProgressDialog.show (me, "Loading...", "Proceeding, please wait.", true);
                Resources res = getResources();
                String action = ( isChecked ? res.getString (R.string.turnon) : res.getString (R.string.turnoff) );
                String msg = res.getString (R.string.confirm, action, res.getString (R.string.pc_toggle));
                DialogFragment frag = MainActivity.YesNoDialogBuilder.buildDialog (msg, R.string.btn_yes, R.string.btn_undo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        // user just said 'yes'
                        /*if (isChecked != toggle.isChecked())
						{
						    toastUtil ("PC status changed, aborting");
						    return;
						}*/
                        //toastUtil (R.string.wait_please);
                        if (!checkConnection (connMgr))
                        {
                            noConnectionWindow();
                            return;
                        }
                        Resources res = getResources();
                        // since the dialog is not cancelable, we do
                        // not need to lock toggle
                        workingProgressDialog = ProgressDialog.show (ControlActivity.this, res.getString (R.string.loading), res.getString (R.string.wait_please), true, false);
                        new WritePCStateTask().execute ( ( isChecked ? "1" : "0" ));
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        ignoreEv = true;
                        toggle.setChecked (!isChecked);
                        currentTask.setShouldInterrupt (false);
                        dialog.dismiss();
                    }
                });
                frag.setCancelable (false);
                currentTask.setShouldInterrupt (true);
                frag.show (getFragmentManager(), "suredialog");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate (R.menu.controlmenu, menu);
        return true;
    }

    private void showTwDialog()
    {
        new TwDialog().show (getFragmentManager(), "TWDialog");
    }

    public void twCopyId (View view)
    {
        this.copy ("TW id", getResources().getString (R.string.twid));
        toastUtil (R.string.copied);
    }

    public void twCopyPwd (View view)
    {
        this.copy ("TW pwd", this.twPwd);
        toastUtil (R.string.copied);
    }

    private void startTw()
    {
        Intent intent = getPackageManager().getLaunchIntentForPackage ("com.teamviewer.teamviewer.market.mobile");
        if (intent != null)
            this.startActivity (intent);
    }

    private void copy (String label, String text)
    {
        ClipboardManager mgr = (ClipboardManager) getSystemService (Context.CLIPBOARD_SERVICE);
        mgr.setPrimaryClip (ClipData.newPlainText (label, text));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (currentTask == null) return;
        //this.toastUtil ("PC status thread halted.");
        currentTask.setShouldInterrupt (true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (currentTask == null || (currentTask != null && !currentTask.getShouldInterrupt())) return;
        //this.toastUtil ("Resuming PC status thread.");
        currentTask.setShouldInterrupt (false);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (currentTask == null) return;
        currentTask.setShouldStop (true);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask (this);
                return true;
            case R.id.about:
                MainActivity.showAboutDialog (this);
                return true;
            case R.id.twdatabtn:
                this.showTwDialog();
                return true;
            case R.id.starttw:
                this.startTw();
                return true;
        }
        return super.onOptionsItemSelected (item);
    }

    private void noConnectionWindow()
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                MainActivity.SimpleDialogBuilder d = MainActivity.SimpleDialogBuilder.buildDialog (R.string.err_no_conn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        setResult (RESULT_FIRST_USER);
                        finish();
                    }
                });
                d.setCancelable (false);
                d.show (getFragmentManager(), "rddialog");
            }
        });
    }

    private String mergeStrings (Object... strings)
    {
        Resources res = getResources();
        StringBuilder sBuilder = new StringBuilder();
        for (Object str : strings)
        {
            if (str instanceof Integer)
                sBuilder.append (res.getString ((Integer)str));
            else if (str instanceof CharSequence)
                sBuilder.append ((CharSequence)str);
        }
        return sBuilder.toString();
    }

    private boolean checkConnection (ConnectivityManager manager)
    {
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null || (info != null && !info.isConnected()))
            return false;
        return true;
    }

    /*private void connFailed()
	{
		MainActivity.SimpleDialogBuilder.buildDialog (
				R.string.err_fail_conn
		).show (getFragmentManager(), "rdialog");
	}*/

    private void connFailed (final String additionalData)
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                MainActivity.SimpleDialogBuilder.buildDialog (
                        mergeStrings (R.string.err_fail_conn, ": ", additionalData)
                ).show (getFragmentManager(), "rdialog");
            }
        });
    }

    private void toastUtil (final String msg)
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                Toast.makeText (ControlActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toastUtil (final int msg)
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                Toast.makeText (ControlActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connFailedToast()
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                Toast.makeText (ControlActivity.this, R.string.err_fail_conn, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connFailed (final Throwable ex)
    {
        this.runOnUiThread (new Runnable() {
            public void run() {
                connFailed (ex.getMessage());
            }
        });
    }

    private class WritePCStateTask
        extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground (String... params)
        {
            String realURL = String.format (TRIGGER_STATUS_CHANGE_URL, passwordUsed, params[0]);
            InputStream is = null;
            try
            {
                URL url = new URL (realURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout (10000);
                conn.setConnectTimeout (15000);
                conn.setRequestMethod ("GET");
                conn.setDoInput (true);
                conn.connect();
                if (conn.getResponseCode() != 200)
                {
                    connFailed ("Got response code " + conn.getResponseCode());
                    return null;
                }
                is = conn.getInputStream();
                Scanner scanner = new Scanner (is).useDelimiter ("\\A");
                String res = ( scanner.hasNext() ? scanner.next() : "" );
                is.close();
                if (!"OK".equals (res))
                    connFailed ("Server reply: " + res);
            }
            catch (Exception e)
            {
                connFailed (e);
            }
            finally
            {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute (Void result)
        {
            workingProgressDialog.dismiss();
            currentTask.setShouldInterrupt (false);
            ToggleButton sw = (ToggleButton) findViewById (R.id.toggleButton1);
            sw.setEnabled (false);
            ((TextView) findViewById (R.id.watDoingLabel)).setText (( sw.isChecked() ? R.string.waiting_on : R.string.waiting_off ));
            findViewById (R.id.llayout).setVisibility (View.VISIBLE);
            toastUtil (mergeStrings (R.string.done, " ", ( sw.isChecked() ? R.string.waiting_on : R.string.waiting_off )));
        }
    }

    private class CheckPCStateRecurringTask
    {
        private boolean shouldStop = false;
        private boolean shouldInterrupt = false;
        private final int waitingTime = 5000;

        public void startThread()
        {
            new Thread (new Runnable() {
                public void run() {
                    ConnectivityManager mgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
                    final ToggleButton sw = (ToggleButton) findViewById (R.id.toggleButton1);
                    InputStream is = null;
                    try {
                        URL uri = new URL (KEEPALIVE_URL);
                        while (!shouldStop)
                        {
                            if (shouldInterrupt)
                            {
                                Thread.sleep (waitingTime);
                                continue;
                            }
                            //Log.d ("it.robertof.rpcc", "Checking.");
                            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
                            conn.setReadTimeout (10000);
                            conn.setConnectTimeout (15000);
                            conn.setRequestMethod ("GET");
                            conn.setDoInput (true);
                            conn.connect();
                            if (conn.getResponseCode() != 200)
                            {
                                connFailedToast();// ("Got response code " + conn.getResponseCode());
                                return;
                            }
                            is = conn.getInputStream();
                            Scanner scanner = new Scanner (is).useDelimiter ("\\A");
                            String res = ( scanner.hasNext() ? scanner.next() : "" );
                            is.close();
                            final boolean val;
                            if ("ON".equals (res))
                                val = true;
                            else if ("OFF".equals (res))
                                val = false;
                            else
                            {
                                toastUtil ("Unknown reply from the server");
                                val = false;
                            }
                            if (!shouldInterrupt)
                                sw.post (new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        if (sw.isEnabled() && sw.isChecked() != val)
                                        {
                                            ignoreEv = true;
                                            sw.setChecked (val);
                                        }
                                        else if (!sw.isEnabled() && sw.isChecked() == val)
                                        {
                                            sw.setEnabled (true);
                                            findViewById (R.id.llayout).setVisibility (View.GONE);
                                        }
                                        /*else if (!sw.isEnabled() && sw.isChecked() != val && pb.getVisibility() == View.GONE)
                                        {
                                            pb.setVisibility (View.VISIBLE);
                                        }*/
                                    }
                                });
                            Thread.sleep (waitingTime);
                        }
                    }
                    catch (Exception e)
                    {
                        if (!checkConnection (mgr))
                            noConnectionWindow();
                        else
                            connFailedToast();// (e);
                    }
                    finally
                    {
                        try {
                            if (is != null)
                                is.close();
                        } catch (IOException e){}
                    }
                }
            }).start();
        }

        public void setShouldStop (boolean shouldStop)
        {
            this.shouldStop = shouldStop;
        }

        public void setShouldInterrupt (boolean shouldInterrupt)
        {
            this.shouldInterrupt = shouldInterrupt;
        }

        public boolean getShouldInterrupt()
        {
            return this.shouldInterrupt;
        }
        /*@Override
		protected void onPostExecute (Boolean data)
		{
			Switch s = (Switch) findViewById (R.id.switch1);
			if (s.isChecked() != data)
			{
				ignoreEv = true;
				s.setChecked (data);
			}
		}*/
    }

    public static class TwDialog
        extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setView (inflater.inflate (R.layout.dialog_tw, null))
                .setTitle (R.string.tw_data)
                .setPositiveButton (R.string.login_error_btn_ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        TwDialog.this.getDialog().cancel();
                    }
                });
            return builder.create();
        }
    }
}
