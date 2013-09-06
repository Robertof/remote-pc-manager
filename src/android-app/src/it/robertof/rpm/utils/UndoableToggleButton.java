package it.robertof.rpm.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public class UndoableToggleButton extends ToggleButton
{
    private UndoableClickEvent mClickEvt;
    
    public UndoableToggleButton (Context context)
    {
        super (context);
    }

    public UndoableToggleButton (Context context, AttributeSet attrs,
            int defStyle)
    {
        super (context, attrs, defStyle);
    }

    public UndoableToggleButton (Context context, AttributeSet attrs)
    {
        super (context, attrs);
    }
    
    public void setUndoableClickEvent (UndoableClickEvent evt)
    {
        this.mClickEvt = evt;
    }
    
    @Override
    public boolean performClick()
    {
        if (mClickEvt != null)
        {
            if (mClickEvt.onClick (this))
                toggle();
            return true;
        }
        return super.performClick();
    }
    
    public static interface UndoableClickEvent
    {
        public boolean onClick (ToggleButton btn);
    }
}
