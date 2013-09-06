package it.robertof.rpm.utils;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.utils.ServiceStrings.Service;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class MethodUtils
{
    /**
     * Returns the SHA1 of text in hexadecimal.
     * @param text string to hash
     * @return sha1 hash of text in hexadecimal
     */
    public static String getSha1 (String text)
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
    
    /**
     * Checks if the connection is enabled.
     * @param manager ConnectionManager
     * @return true if the connection is enabled
     */
    public static boolean checkConnection (ConnectivityManager manager)
    {
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null || (info != null && !info.isConnected()))
            return false;
        return true;
    }
    
    /**
     * Merges translated strings (from {@link R.string}) and normal ones (CharSequence)
     * @param res resources of your activity (use getResources())
     * @param strings array of string or integers (or both)
     * @return every string in the array in one string
     */
    public static String mergeStrings (Resources res, Object... strings)
    {
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
    
    /**
     * Shows the about dialog.
     * @param delegate activity instance
     */
    public static void showAboutDialog (Activity delegate)
    {
        DialogBuilder d = DialogBuilder.buildDialog (R.string.about_txt);
        d.setTitle (delegate.getResources().getText (R.string.about));
        d.show (delegate.getFragmentManager(), "rAboutDlg");
    }
    
    /**
     * Creates a dialog which causes the application to close
     * when okay-clicked.
     * @param activity The activity which will be closed
     * @param msg The message to show in the dialog. (mergeStrings can be used)
     */
    public static void fatalErrorDialog (final Activity activity, final Object msg)
    {
        activity.runOnUiThread (new Runnable() {
            public void run()
            {
                DialogBuilder db = DialogBuilder.buildDialog (msg, new DialogInterface.OnClickListener()
                {               
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        activity.setResult (Activity.RESULT_FIRST_USER);
                        activity.finish();
                    }
                });
                db.setCancelable (false);
                db.show (activity.getFragmentManager(), Double.toString (Math.random()));
            }
        });
    }
    
    /**
     * Starts the TeamViewer official application if found.
     * @param activity Your activity.
     */
    public static void startTw (Activity activity)
    {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage ("com.teamviewer.teamviewer.market.mobile");
        if (intent != null)
            activity.startActivity (intent);
    }
    
    /**
     * Shows a Toast with the duration of LENGTH_SHORT.
     * @param activity An activity.
     * @param msg The desidered message.
     * @param duration The desidered duration ({@link Toast#LENGTH_LONG} or {@link Toast#LENGTH_SHORT})
     */
    public static void displayToast (final Activity activity, final Object msg, final int duration)
    {
        activity.runOnUiThread (new Runnable() {
            @Override
            public void run ()
            {
                if (msg instanceof Integer)
                    Toast.makeText (activity, (Integer) msg, duration).show();
                else
                    Toast.makeText (activity, (String) msg, duration).show();
            }
        });
    }
    
    /**
     * Alias for displayToast (..., mergeStrings (data)).
     * @param activity An activity.
     * @param data Data to pass to mergeStrings
     */
    public static void displayToast (Activity activity, int duration, Object... data)
    {
        displayToast (activity, mergeStrings (activity.getResources(), data), duration);
    }
    
    /**
     * Merges an URL with a querystring and generates an URL object.
     * If the URL constructor throws an exception, a RuntimeException
     * with the same parameters will be thrown.
     * @param baseURL Basic URL (http://example.com/test)
     * @param queryString The querystring.
     * @return an URL object with the path baseURL + ? + queryString
     */
    public static URL generateURL (String baseURL, String queryString)
    {
        try {
            return new URL (baseURL + "?" + queryString);
        } catch (Exception e) {
            throw new RuntimeException (e);
        }
    }
    
    /**
     * Generates a trigger_action.php URL, given an action name and
     * optional other parameters.
     * @param actionName Action name to trigger.
     * @param otherParams Other parameters. Use null if you don't need them,
     * or use generateTriggerUrl (String).
     * @return The correct URL object for the specified actionName
     */
    public static URL generateTriggerUrl (String actionName, String otherParams)
    {
        return generateURL (ControlActivity.CFG_TRIGGER_URL,
                "action=" + actionName +
                "&pwd=" + ControlActivity.passwordUsed +
                (otherParams != null ? "&" + otherParams : ""));
    }
    
    /**
     * Generates a trigger_action.php URL, given just the action name.
     * @param actionName Action name to trigger.
     * @return The correct URL object for the specified actionName
     */
    public static URL generateTriggerUrl (String actionName)
    {
        return generateTriggerUrl (actionName, null);
    }
    
    /**
     * Performs a bad and manual parsing on the JSON
     * returned by the action 'alive'.
     * @param json JSON returned by action 'alive'
     * @return A Map<Service, Boolean>.
     */
    public static Map<Service, Boolean> parseStatusJson (String json)
    {
        Map<Service, Boolean> returnValue = new HashMap<Service, Boolean>();
        for (Service service : Service.values())
        {
            Matcher mtc = Pattern.compile ("\"" + service.toString() + "\":(false|true)", Pattern.CASE_INSENSITIVE).matcher (json);
            if (!mtc.find())
                return null;
            returnValue.put (service, Boolean.valueOf (mtc.group (1)));
        }
        return returnValue;
    }
}
