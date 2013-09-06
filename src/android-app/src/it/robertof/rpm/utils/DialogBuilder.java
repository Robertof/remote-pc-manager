package it.robertof.rpm.utils;

import it.robertof.rpm.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogBuilder extends DialogFragment
{    
    public static final String BUNDLEKEY   = "robsdialog.msg";
    public static final String ISSTRINGKEY = "robsdialog.isstring";
    private DialogInterface.OnClickListener listener;
    private CharSequence title;
    
    public static DialogBuilder buildDialog (Object message, DialogInterface.OnClickListener listener)
    {
        Bundle data = new Bundle();
        data.putBoolean (ISSTRINGKEY, message instanceof String);
        if (message instanceof String)
            data.putString (BUNDLEKEY, (String) message);
        else if (message instanceof Integer)
            data.putInt (BUNDLEKEY, (Integer) message);
        else
            return null;
        DialogBuilder builder = new DialogBuilder();
        builder.listener = listener;
        builder.setArguments (data);
        return builder;
    }

    public static DialogBuilder buildDialog (Object message)
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