package it.robertof.rpm.pages.actions;

import it.robertof.rpm.R;
import it.robertof.rpm.utils.DeactivableViewPager;
import it.robertof.rpm.utils.DialogBuilder;
import it.robertof.rpm.utils.HoloColorSequenceGenerator;
import it.robertof.rpm.utils.MethodUtils;
import it.robertof.rpm.utils.YesNoDialogBuilder;
import it.robertof.rpm.workers.AsyncWriter;
import it.robertof.rpm.workers.ScheduledImageChecker;
import it.robertof.rpm.workers.WorkerConfig;
import java.net.URL;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class WebcamAction implements Action
{
    protected final SparseIntArray tabColors = new SparseIntArray();
    private final HoloColorSequenceGenerator generator = new HoloColorSequenceGenerator();
    private boolean waitingForImg;
    
    @Override
    public int getPageCount ()
    {
        return 2;
    }

    @Override
    public int getLayoutId ()
    {
        return R.layout.action_webcam;
    }

    @Override
    public void onInflate (View inflated, Activity activity, int page)
    {
        int res = tabColors.get (page, -1);
        if (res == -1) {
            if (page == 0)
            {
                ScreenshotAction instance = (ScreenshotAction) ActionDB.SCREENSHOT.getInstance();
                if (page == 0 && instance.tabColors.indexOfKey (0) >= 0)
                    generator.addToAlreadyGenerated (instance.tabColors.get (0));
            }
            res = generator.randomColor();
            tabColors.put (page, res);
        }
        inflated.setBackgroundResource (res);
        final TextView tv = (TextView) inflated.findViewById (R.id.webcamText);
        switch (page)
        {
            case 0:
                if (this.waitingForImg)
                {
                    tv.setVisibility (View.GONE);
                    inflated.findViewById (R.id.webcamProgressBar).setVisibility (View.VISIBLE);
                    return;
                }
                inflated.findViewById (R.id.webcamProgressBar).setVisibility (View.GONE);
                tv.setVisibility (View.VISIBLE);
                tv.setText (R.string.webcam);
            break;
            case 1:
                tv.setVisibility (View.VISIBLE);
                tv.setText (R.string.take_webcam);
            break;
        }
    }

    @Override
    public void handleClickEvent (View view, final Activity activity, int page)
    {
        if (page == 0)
            MethodUtils.displayToast (activity, (this.waitingForImg ? R.string.wait_please : R.string.wrong_page), Toast.LENGTH_SHORT);
        else
        {
            if (ScheduledImageChecker.currentInstance != null)
            {
                MethodUtils.displayToast (activity, R.string.download_already_queued, Toast.LENGTH_LONG);
                return;
            }
            // TODO: completely refactor the Async* stuff, because it can
            // be implemented in a better way (without redundant code).
            YesNoDialogBuilder.buildDialog (R.string.webcam_confirmation, R.string.btn_yes, R.string.btn_undo, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick (DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    final ProgressDialog pdiag = ProgressDialog.show (activity,
                            activity.getString (R.string.loading),
                            activity.getString (R.string.wait_please), true, false);
                    final WorkerConfig basicConfig = new WorkerConfig (MethodUtils.generateTriggerUrl ("do_action", 
                            "param=webcam"), activity) {
                        @Override
                        public void onPostExecute (boolean result)
                        {
                            pdiag.dismiss();
                            if (!result) return;
                            MethodUtils.displayToast (activity, R.string.waiting_for_webcam, Toast.LENGTH_LONG);
                            waitingForImg = true;
                            ActionDB.WEBCAM.getPager().setCurrentItem (0, true);
                            ActionDB.WEBCAM.getPager().setAdapter (ActionDB.WEBCAM.getPager().getAdapter());
                            ((DeactivableViewPager) ActionDB.WEBCAM.getPager()).setSwipable (false);
                            final WorkerConfig imgCheckerConf = new WorkerConfig (new URL[] {
                                    MethodUtils.generateTriggerUrl ("uploaded", "context=webcam"),
                                    MethodUtils.generateTriggerUrl ("get_uploaded_image", "context=webcam")
                            }, activity) {
                                @Override
                                public void onPostExecute (boolean result)
                                {
                                    if (!result)
                                    {
                                        MethodUtils.displayToast (activity, R.string.download_in_progress, Toast.LENGTH_LONG);
                                        return;
                                    }
                                    waitingForImg = false;
                                    ActionDB.WEBCAM.getPager().setAdapter (ActionDB.WEBCAM.getPager().getAdapter());
                                    ((DeactivableViewPager) ActionDB.WEBCAM.getPager()).setSwipable (true);
                                    MethodUtils.displayToast (activity, R.string.download_completed, Toast.LENGTH_LONG);
                                }
                                
                                @Override
                                public void onError (final String msg)
                                {
                                    activity.runOnUiThread (new Runnable() {
                                        public void run()
                                        {
                                            waitingForImg = false;
                                            ActionDB.WEBCAM.getPager().setAdapter (ActionDB.WEBCAM.getPager().getAdapter());
                                            ((DeactivableViewPager) ActionDB.WEBCAM.getPager()).setSwipable (true);
                                            DialogBuilder.buildDialog (MethodUtils.mergeStrings (activity.getResources(), R.string.err_generic, ": ", msg))
                                                .show (activity.getFragmentManager(), Double.toString (Math.random()));
                                        }
                                    });
                                }
                            };
                            imgCheckerConf.setContext ("webcam");
                            new ScheduledImageChecker (imgCheckerConf).begin();
                        }
                    };
                    new AsyncWriter().execute (basicConfig);
                }
            }).show (activity.getFragmentManager(), Double.toString (Math.random()));
        }
    }
}
