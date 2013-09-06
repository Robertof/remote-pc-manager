package it.robertof.rpm.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class YesNoDialogBuilder extends DialogFragment
{
    public static final String MSGTXT_KEY    = "yesnodialog.data.msg";
    public static final String ISSTRING_KEY  = "yesnodialog.data.msg_str";
    public static final String YESTXT_KEY    = "yesnodialog.data.yes";
    public static final String NOTXT_KEY     = "yesnodialog.data.no";
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
