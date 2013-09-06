package it.robertof.rpm;

import it.robertof.rpm.R;
import it.robertof.rpm.utils.DialogBuilder;
import it.robertof.rpm.utils.MethodUtils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity
{
    public final static String PWD_MSG_INTENT = "unusefulstuff.pwd"; // do not edit this
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        getActionBar().setTitle (R.string.login_btn);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate (R.menu.aboutmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.about)
        {
            MethodUtils.showAboutDialog (this);
            return true;
        }
        return super.onOptionsItemSelected (item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        ((EditText) this.findViewById (R.id.pwd)).setText("");
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_FIRST_USER)
            finish();
    }

    public void handleLogin (View view)
    {
        String pwd = ((EditText) this.findViewById (R.id.pwd)).getText().toString();
        if (!ControlActivity.CFG_PASSWORD_HASH.equals (MethodUtils.getSha1 (pwd)))
        {
            DialogBuilder.buildDialog (R.string.login_error).show (this.getFragmentManager(), "RPCCFMgr");
            return;
        }
        Intent intent = new Intent (this, ControlActivity.class);
        intent.putExtra (PWD_MSG_INTENT, pwd);
        this.startActivityForResult (intent, 0);
    }
}
