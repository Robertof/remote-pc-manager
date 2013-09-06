package it.robertof.rpm.pages.actions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GenericActionPagerAdapter extends FragmentPagerAdapter
{
    private ActionDB action;
    public GenericActionPagerAdapter (FragmentManager fm, ActionDB object)
    {
        super (fm);
        this.action = object;
    }

    @Override
    public Fragment getItem (int position)
    {
        GenericActionFragment frag = new GenericActionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt ("RPM_POS", position);
        bundle.putString ("ACTION_KEY", this.action.toString());
        frag.setArguments (bundle);
        return frag;
    }

    @Override
    public int getCount ()
    {
        return this.action.getInstance().getPageCount();
    }
    
    public static class GenericActionFragment extends Fragment
    {
        @Override
        public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle state)
        {
            Action action = ActionDB.valueOf (getArguments().getString ("ACTION_KEY")).getInstance();
            View inflated = inflater.inflate (action.getLayoutId(), container, false);
            action.onInflate (inflated, getActivity(), getArguments().getInt ("RPM_POS"));
            return inflated;
        }
    }
}
