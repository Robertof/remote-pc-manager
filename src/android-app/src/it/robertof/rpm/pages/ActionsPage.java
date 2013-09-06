package it.robertof.rpm.pages;

import it.robertof.rpm.ControlActivity;
import it.robertof.rpm.R;
import it.robertof.rpm.pages.PagesHandler.Page;
import it.robertof.rpm.pages.actions.ActionDB;
import it.robertof.rpm.pages.actions.GenericActionPagerAdapter;
import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class ActionsPage implements Page
{
    //public static ScreenshotAction scAction = new ScreenshotAction();
    //public static DeactivableViewPager scPager;
    
    @Override
    public int getLayoutId()
    {
        return R.layout.actions;
    }

    @Override
    public void onInflate (FragmentManager childManager, View masterView, Activity activity)
    {
        //System.out.println ("onInflate");
        for (ActionDB action : ActionDB.values())
        {
            ViewPager pager = (ViewPager) masterView.findViewById (action.getPagerId());
            action.setPager (pager);
            pager.setAdapter (new GenericActionPagerAdapter (childManager, action));
            SingleTapGestureDetector provider = new SingleTapGestureDetector (activity, pager, action);
            pager.setOnTouchListener (new InnerPagerTouchListener (provider, new GestureDetector (activity, provider)));
        }
        /*scPager = (DeactivableViewPager) masterView.findViewById (R.id.button1pager);
        scPager.setAdapter (new GenericActionPagerAdapter (childManager));
        final SingleTapGestureDetector handler = new SingleTapGestureDetector (activity, scPager);
        final GestureDetector detector = new GestureDetector (activity, handler);
        scPager.setOnTouchListener (new OnTouchListener() {

        });*/
    }
    
    private static class InnerPagerTouchListener implements OnTouchListener
    {
        private SingleTapGestureDetector handler;
        private GestureDetector detector;
        private float downX;
        
        public InnerPagerTouchListener (SingleTapGestureDetector h, GestureDetector detector)
        {
            this.handler = h;
            this.detector = detector;
        }
        
        public boolean onTouch (View v, MotionEvent event)
        {
            handler.setView (v);
            detector.onTouchEvent (event);
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (Math.abs (downX - event.getX()) > 200 && downX - event.getX() < 0 && ((ViewPager) v).getCurrentItem() == 0)
                {
                    ControlActivity.pager.setCurrentItem (ControlActivity.pager.getCurrentItem() - 1, true);
                    //System.out.println ("on swipe return");
                    return false;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                downX = event.getX();
            v.getParent().requestDisallowInterceptTouchEvent (true);
            return false;
        }
    }
    
    private class SingleTapGestureDetector extends GestureDetector.SimpleOnGestureListener
    {
        private Activity activity;
        private View view;
        private ViewPager pager;
        private ActionDB abstrinst;
        
        public SingleTapGestureDetector (Activity ac, ViewPager pager, ActionDB act)
        {
            this.activity = ac;
            this.pager = pager;
            this.abstrinst = act;
        }
        
        public void setView (View v)
        {
            this.view = v;
        }
        
        @Override
        public boolean onSingleTapConfirmed (MotionEvent evt)
        {
            //System.out.println ("onSingleTapConfirmed");
            this.abstrinst.getInstance().handleClickEvent (this.view, this.activity, this.pager.getCurrentItem());
            return true;
        }
    }
}
