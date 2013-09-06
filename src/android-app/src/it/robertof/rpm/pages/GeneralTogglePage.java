package it.robertof.rpm.pages;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.pages.PagesHandler.Page;
import it.robertof.rpm.utils.DialogBuilder;
import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.utils.ServiceStrings;
import it.robertof.rpm.utils.ServiceStrings.Service;
import it.robertof.rpm.utils.UndoableToggleButton;
import it.robertof.rpm.utils.UndoableToggleButton.UndoableClickEvent;
import it.robertof.rpm.utils.YesNoDialogBuilder;
import it.robertof.rpm.workers.AsyncWriter;
import it.robertof.rpm.workers.WorkerConfig;
import java.net.URL;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public abstract class GeneralTogglePage implements Page
{
    public View view;
    private ProgressDialog workDialog;
    private Service service;
    private boolean configured = false;
    
    /**
     * Configures this GeneralTogglePage.
     * @param service Desidered service
     */
    public void configure (Service service)
    {
        this.service = service;
        this.configured = true;
        this.service.setPage (this);
    }
    
    @Override
    public void onInflate (FragmentManager manager, final View masterView, final Activity activity)
    {
        if (!configured)
            throw new RuntimeException ("GeneralTogglePage not configured");
        if (!MethodUtils.checkConnection (ControlActivity.sConnManager))
        {
            MethodUtils.fatalErrorDialog (activity, R.string.err_no_conn);
            return;
        }
        this.view = masterView;
        final UndoableToggleButton btn = (UndoableToggleButton) masterView.findViewById (this.service.toggleBtnId);
        btn.setUndoableClickEvent (new UndoableClickEvent() {
            @Override
            public boolean onClick (ToggleButton _btn)
            {
                final boolean isChecked = !btn.isChecked();
                if (!canTriggerEvent (activity, service, masterView, isChecked))
                    return false;
                final Resources resources = activity.getResources();
                // are you sure that you want to $1 the $2?
                String message = resources.getString (R.string.confirm, 
                        (isChecked ? resources.getString (R.string.turnon) : resources.getString (R.string.turnoff)),
                        resources.getString (GeneralTogglePage.this.service.toggleStr));
                DialogFragment mFragment = YesNoDialogBuilder.buildDialog (message,
                        R.string.btn_yes, R.string.btn_undo,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick (DialogInterface dialog, int which)
                            {
                                // prevent race conditions. Check connection status again
                                if (!MethodUtils.checkConnection (ControlActivity.sConnManager))
                                {
                                    MethodUtils.fatalErrorDialog (activity, R.string.err_no_conn);
                                    return;
                                }
                                if (btn.isChecked() == isChecked)
                                {
                                    DialogBuilder.buildDialog (R.string.err_race_status_changed)
                                        .show (activity.getFragmentManager(), Double.toString (Math.random()));
                                    return;
                                }
                                ControlActivity.sStatusChecker.pause();
                                btn.toggle();
                                workDialog = ProgressDialog.show (activity,
                                        resources.getString (R.string.loading),
                                        resources.getString (R.string.wait_please),
                                        true, false);
                                final URL url = MethodUtils.generateTriggerUrl (isChecked ?
                                        ServiceStrings.getTurnOnString (service) :
                                        ServiceStrings.getTurnOffString (service));
                                final WorkerConfig config = new WorkerConfig (url, activity) {
                                    @Override
                                    public void onPostExecute (boolean result)
                                    {
                                        workDialog.dismiss();
                                        ControlActivity.sStatusChecker.resume();
                                        if (!result)
                                            return;
                                        btn.setEnabled (false);
                                        ((TextView) masterView.findViewById (service.progressLayoutWatDoingLabel))
                                            .setText ((btn.isChecked() ? service.waitingOn : service.waitingOff));
                                        masterView.findViewById (service.progressLayoutId).setVisibility (View.VISIBLE);
                                    }
                                };
                                new AsyncWriter().execute (config);
                            }
                        });
                //mFragment.setCancelable (false);
                mFragment.show (activity.getFragmentManager(), Double.toString (Math.random()));
                return false;
            }
        });
                /*
                       */         
        registerOtherEvents (activity, service, masterView, btn);
    }
    
    /**
     * If you want to register other events on the {@link android.widget.ToggleButton},
     * you may want to override this function. By default, it does nothing.
     * @param activity An activity.
     * @param service The service associated with this Page
     * @param masterView The view of the inflated layout
     * @param btn The ToggleButton associated with this Page
     */
    public void registerOtherEvents (Activity activity, Service service, View masterView, ToggleButton btn)
    {}
    
    /**
     * If you need to cancel the activation of the ToggleButton,
     * you may want to override this and return a negative value.
     * The default implementation returns true.
     * @param activity An activity.
     * @param service The service associated with this Page
     * @param masterView The view of the inflated layout
     * @param isChecked True if the ToggleButton will be checked or not
     * @return True if the event can be executed, false otherwise
     */
    public boolean canTriggerEvent (Activity activity, Service service, View masterView, boolean isChecked)
    {
        return true;
    }
}
