package it.robertof.rpm.utils;

import it.robertof.rpm.R;
import it.robertof.rpm.pages.GeneralTogglePage;

/**
 * Contains turn on and off action names for every
 * service.
 * Provides a convenient enum for services.
 * @author Robertof
 *
 */
public class ServiceStrings
{   
    /**
     * Given a service, returns the action name
     * which can be used to turn on the service.
     * @param service value from the Service enum
     * @return the action name which can be used to turn on service
     */
    public static String getTurnOnString (Service service)
    {
        return service.actions[0];
    }
    
    /**
     * Given a service, returns the action name
     * which can be used to turn off the service.
     * @param service value from the Service enum
     * @return the action name which can be used to turn off the service
     */
    public static String getTurnOffString (Service service)
    {
        return service.actions[1];
    }
    
    /**
     * Returns a member from the Service enum given
     * a string.
     * @param str A string representing a member in the enum
     * @return The Service which matches str
     */
    public static Service getServiceByString (String str)
    {
        for (Service service : Service.values())
        {
            if (service.toString().equalsIgnoreCase (str))
                return service;
        }
        return null;
    }
    
    public static enum Service
    {
        PC (new String[] { "turn_on_pc", "turn_off_pc" }, R.string.waiting_pc_on, R.string.waiting_pc_off, R.id.pcBtn, R.string.pc_toggle, R.id.pc_progress_layout, R.id.pcWatDoing),
        TEAMVIEWER (new String[] { "enable_teamviewer", "disable_teamviewer" }, R.string.waiting_tw_on, R.string.waiting_tw_off, R.id.twBtn, R.string.tw_toggle, R.id.teamviewer_progress_layout, R.id.twWatDoing);
        public String[] actions;
        public int waitingOn;
        public int waitingOff;
        public int toggleBtnId;
        public int toggleStr;
        public int progressLayoutId;
        public int progressLayoutWatDoingLabel;
        public GeneralTogglePage associatedPage;
        Service (String[] actions, int onString, int offString, int toggleId, int toggleString, int progressLayout, int progressWatDoing)
        {
            this.actions = actions;
            this.waitingOn = onString;
            this.waitingOff = offString;
            this.toggleBtnId = toggleId;
            this.toggleStr = toggleString;
            this.progressLayoutId = progressLayout;
            this.progressLayoutWatDoingLabel = progressWatDoing;
        }
        
        public void setPage (GeneralTogglePage page)
        {
            this.associatedPage = page;
        }
    }
}
