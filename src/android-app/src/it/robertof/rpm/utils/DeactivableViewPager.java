package it.robertof.rpm.utils;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DeactivableViewPager extends ViewPager
{
    private boolean mEnabled = true;
    public DeactivableViewPager (Context context)
    {
        super (context);
    }
    
    public DeactivableViewPager (Context context, AttributeSet attrs)
    {
        super (context, attrs);
    }
    
    public void setSwipable (boolean flag)
    {
        //System.out.println ("setSwipable called (was: " + mEnabled + ", is: " + flag + ")");
        this.mEnabled = flag;
    }

    @Override
    public boolean onTouchEvent (MotionEvent event)
    {
        if (this.mEnabled)
            return super.onTouchEvent (event);
        return false;
    }
    
    @Override
    public boolean onInterceptTouchEvent (MotionEvent event)
    {
        if (this.mEnabled)
            return super.onInterceptTouchEvent (event);
        return false;
    }
}
