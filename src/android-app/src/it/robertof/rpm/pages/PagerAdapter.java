package it.robertof.rpm.pages;

import it.robertof.rpm.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PagerAdapter extends FragmentPagerAdapter
{
    private static final String POS_KEY = "R_POS";
    private Activity activity;
    public PagerAdapter (Activity activity, FragmentManager fm)
    {
        super (fm);
        this.activity = activity;
    }
    
    @Override
    public Fragment getItem (int i)
    {
        Fragment fragment = new FragmentPageHandler();
        Bundle args = new Bundle();
        args.putInt (POS_KEY, i);
        fragment.setArguments (args);
        return fragment;
    }
    
    @Override
    public int getCount()
    {
        return PagesHandler.getHandler().getCount();
    }
    
    @Override
    public CharSequence getPageTitle (int pos)
    {
        return activity.getResources().getString (R.string.tab1 + pos);
    }
    
    public static class FragmentPageHandler extends Fragment
    {
        @Override
        public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle state)
        {
            int pos = getArguments().getInt (POS_KEY);
            PagesHandler.Page page = PagesHandler.getHandler().getPage (pos);
            View masterView = inflater.inflate (page.getLayoutId(), container, false);
            page.onInflate (this.getChildFragmentManager(), masterView, getActivity());
            return masterView;
        }
    }
}
