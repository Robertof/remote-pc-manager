package it.robertof.rpm.pages.actions;

import it.robertof.rpm.R;
import android.support.v4.view.ViewPager;

public enum ActionDB
{
    SCREENSHOT (R.id.button1pager, new ScreenshotAction()),
    WEBCAM (R.id.button2pager, new WebcamAction());
    private Action instance;
    private ViewPager pager;
    private int pagerid;
    
    ActionDB (int pagerId, Action instance)
    {
        this.instance = instance;
        this.pagerid = pagerId;
    }
    
    public void setPager (ViewPager pager)
    {
        this.pager = pager;
    }
    
    public ViewPager getPager()
    {
        return this.pager;
    }
    
    public Action getInstance()
    {
        return this.instance;
    }
    
    public int getPagerId()
    {
        return this.pagerid;
    }
}
