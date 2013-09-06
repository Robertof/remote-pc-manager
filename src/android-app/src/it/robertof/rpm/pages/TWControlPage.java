package it.robertof.rpm.pages;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.utils.ServiceStrings.Service;
import android.app.Activity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TWControlPage extends GeneralTogglePage
{
    public TWControlPage()
    {
        super.configure (Service.TEAMVIEWER);
    }
    
    @Override
    public int getLayoutId()
    {
        return R.layout.teamviewer_control;
    }
    
    @Override
    public boolean canTriggerEvent (Activity activity, Service service, View masterView, boolean isChecked)
    {
        if (!isChecked || (ControlActivity.sStatusChecker.lastMap != null && ControlActivity.sStatusChecker.lastMap.containsKey (Service.PC) && ControlActivity.sStatusChecker.lastMap.get (Service.PC).booleanValue()))
            return true;
        MethodUtils.displayToast (activity, R.string.err_pc_turned_off, Toast.LENGTH_LONG);
        return false;
    }
    
    @Override
    public void registerOtherEvents (final Activity activity, Service service, View masterView, ToggleButton btn)
    {
        btn.setLongClickable (true);
        btn.setOnLongClickListener (new OnLongClickListener() {
            @Override
            public boolean onLongClick (View v)
            {
                MethodUtils.startTw (activity);
                return true;
            }
        });
    }
}
