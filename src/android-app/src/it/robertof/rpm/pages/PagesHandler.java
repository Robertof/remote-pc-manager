package it.robertof.rpm.pages;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.view.View;

public class PagesHandler
{
    private static PagesHandler instance;
    private List<Page> pages = new ArrayList<Page>();
    public static interface Page
    {
        public int getLayoutId();
        public void onInflate (FragmentManager manager, View masterView, Activity activity);
    }
    
    private PagesHandler()
    {
        pages.add (new PCControlPage());
        pages.add (new TWControlPage());
        pages.add (new ActionsPage());
    }
    
    public static PagesHandler getHandler()
    {
        if (instance == null)
            instance = new PagesHandler();
        return instance;
    }
    
    public Page getPage (int pos)
    {
        if (pages.size() > pos)
            return pages.get (pos);
        return null;
    }
    
    public int getCount()
    {
        return pages.size();
    }
}