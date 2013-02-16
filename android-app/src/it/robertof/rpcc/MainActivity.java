package it.robertof.rpcc;

import java.security.MessageDigest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity
{
    public final static String PWD_MSG_INTENT = "it.robertof.rpcc.pwd";
    private final String pwdHash = "** INSERT SHA1 HASH HERE **";
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
            showAboutDialog (this);
            return true;
        }
        return super.onOptionsItemSelected (item);
    }

    public static void showAboutDialog (Activity delegate)
    {
        SimpleDialogBuilder d = SimpleDialogBuilder.buildDialog (R.string.about_txt);
        d.setTitle (delegate.getResources().getText (R.string.about));
        d.show (delegate.getFragmentManager(), "rAboutDlg");
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

    private String getSha1 (String text)
    {
        try {
            MessageDigest md = MessageDigest.getInstance ("SHA-1");
            byte[] bytes = text.getBytes ("utf-8");
            md.update (bytes, 0, bytes.length);
            byte[] digest = md.digest();
            String res = "";
            for (byte b : digest)
            {
                res += Integer.toString ( ( b & 0xff ) + 0x100, 16).substring (1);
            }
            return res;
        } catch (Exception e) {}
        return null;
    }

    public void handleLogin (View view)
    {
        String pwd = ((EditText) this.findViewById (R.id.pwd)).getText().toString();
        if (!pwdHash.equals (this.getSha1 (pwd)))
        {
            SimpleDialogBuilder.buildDialog (R.string.login_error)
            .show (this.getFragmentManager(), "RPCCFMgr");
            return;
        }
        Intent intent = new Intent (this, ControlActivity.class);
        intent.putExtra (PWD_MSG_INTENT, pwd);
        this.startActivityForResult (intent, 0);
    }

    public static class YesNoDialogBuilder extends DialogFragment
    {
        public static final String MSGTXT_KEY = "it.robertof.rpcc.yesnodialog.data.msg";
        public static final String ISSTRING_KEY  = "it.robertof.rpcc.yesnodialog.data.msg_is";
        public static final String YESTXT_KEY = "it.robertof.rpcc.yesnodialog.data.yes";
        public static final String NOTXT_KEY  = "it.robertof.rpcc.yesnodialog.data.no";
        private DialogInterface.OnClickListener b1c;
        private DialogInterface.OnClickListener b2c;
        public static YesNoDialogBuilder buildDialog (Object data, int opt1, int opt2,
                DialogInterface.OnClickListener btn1click, DialogInterface.OnClickListener btn2click)
        {
            if (!(data instanceof Integer) && !(data instanceof String))
                return null;
            Bundle args = new Bundle();
            if (data instanceof Integer)
                args.putInt (MSGTXT_KEY, (Integer) data);
            else
                args.putString (MSGTXT_KEY, (String) data);
            args.putBoolean (ISSTRING_KEY, data instanceof String);
            args.putInt (YESTXT_KEY, opt1);
            args.putInt (NOTXT_KEY, opt2);
            YesNoDialogBuilder builder = new YesNoDialogBuilder();
            builder.b1c = btn1click;
            builder.b2c = btn2click;
            builder.setArguments (args);
            return builder;
        }

        public static YesNoDialogBuilder buildDialog (Object data, int opt1, int opt2,
                DialogInterface.OnClickListener btn1click)
        {
            return buildDialog (data, opt1, opt2, btn1click, new DialogInterface.OnClickListener() {
                @Override
                public void onClick (DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        }

        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
            Bundle conf = this.getArguments();
            if (conf.getBoolean (ISSTRING_KEY))
                builder.setMessage (conf.getString (MSGTXT_KEY));
            else
                builder.setMessage (conf.getInt (MSGTXT_KEY));
            builder.setPositiveButton(conf.getInt (YESTXT_KEY), this.b1c)
            .setNegativeButton(conf.getInt (NOTXT_KEY), this.b2c);
            return builder.create();
        }
    }

    public static class SimpleDialogBuilder extends DialogFragment {    
        public static final String BUNDLEKEY = "it.robertof.rpcc.data.msg";
        public static final String ISSTRINGKEY = "it.robertof.rpcc.data.isstring";
        private DialogInterface.OnClickListener listener;
        private CharSequence title;
        public static SimpleDialogBuilder buildDialog (Object message, DialogInterface.OnClickListener listener)
        {
            Bundle data = new Bundle();
            data.putBoolean (ISSTRINGKEY, message instanceof String);
            if (message instanceof String)
                data.putString (BUNDLEKEY, (String) message);
            else if (message instanceof Integer)
                data.putInt (BUNDLEKEY, (Integer) message);
            else
                return null;
            SimpleDialogBuilder builder = new SimpleDialogBuilder();
            builder.listener = listener;
            builder.setArguments (data);
            return builder;
        }

        public static SimpleDialogBuilder buildDialog (Object message)
        {
            return buildDialog (message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        }

        public void setTitle (CharSequence data)
        {
            this.title = data;
        }

        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
            Bundle args = this.getArguments();
            if (args.getBoolean (ISSTRINGKEY))
                builder.setMessage (args.getString (BUNDLEKEY));
            else
                builder.setMessage (args.getInt (BUNDLEKEY));
            builder.setPositiveButton (R.string.login_error_btn_ok, listener);
            if (title != null)
                builder.setTitle (title);
            return builder.create();
        }
    }
}
