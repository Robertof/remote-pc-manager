package it.robertof.rpm.pages;

import it.robertof.rpm.R;
import it.robertof.rpm.utils.ServiceStrings.Service;

public class PCControlPage extends GeneralTogglePage
{
    public PCControlPage()
    {
        super.configure (Service.PC);
    }
    
    @Override
    public int getLayoutId()
    {
        return R.layout.activity_control;
    }
}
