package it.robertof.rpm.pages.actions;

import android.app.Activity;
import android.view.View;


public interface Action
{
    public int getPageCount();
    public int getLayoutId();
    public void onInflate (View inflated, Activity activity, int page);
    public void handleClickEvent (View view, Activity activity, int page);
}
