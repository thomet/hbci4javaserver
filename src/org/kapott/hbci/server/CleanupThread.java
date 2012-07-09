
/*  $Id: CleanupThread.java,v 1.3 2005/06/10 18:03:03 kleiner Exp $

    This file is part of hbci4java-server
    Copyright (C) 2001-2005  Stefan Palme

    hbci4java-server is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    hbci4java-server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.server;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.kapott.hbci.manager.HBCIUtils;

/* TODO cleanup-zeiten konfigurierbar machen */
public class CleanupThread 
    extends Thread
{
    private boolean quit;
    
    public CleanupThread()
    {
        this.quit=false;
    }
    
    public void run()
    {
        HBCIUtils.log("starting HBCIServer cleanup thread",HBCIUtils.LOG_DEBUG);
        
        while (!quit) {
            try {
                synchronized (this) {
                    wait(120*1000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
            doJob();
        }
        
        HBCIUtils.log("cleanup thread stopped",HBCIUtils.LOG_DEBUG);
    }
    
    private void doJob()
    {
        // HBCIUtils.log("checking hashtable with user entries",HBCIUtils.LOG_DEBUG);
        
        Hashtable userdata=ServerData.getInstance().getUserData();
        synchronized (userdata) {
            long      now=new Date().getTime();

            for (Enumeration e=userdata.keys();e.hasMoreElements();) {
                String    userid=(String)e.nextElement();
                Hashtable entry=(Hashtable)userdata.get(userid);
                
                synchronized (entry) {
                    if (entry.size()!=0) {
                        long timestamp=((Date)entry.get("timestamp")).getTime();
                        
                        if (now-timestamp>=300*1000) {
                            HBCIUtils.log("removing userdata entry for user "+userid,HBCIUtils.LOG_DEBUG);
                            entry.clear();
                        }
                    }
                }
            }
        }
        
        // -----------
        
        // HBCIUtils.log("checking hashtable with dialogs",HBCIUtils.LOG_DEBUG);
        Hashtable dialogs=DialogMgr.getInstance().getDialogs();
        
        synchronized (dialogs) {
            long now=new Date().getTime();
            
            for (Enumeration e=dialogs.keys();e.hasMoreElements();) {
                String dialogid=(String)e.nextElement();
                Dialog dialog=(Dialog)dialogs.get(dialogid);
                
                if (now-dialog.getLastActive().getTime()>=5*60*1000) {
                    HBCIUtils.log("removing dialog "+dialogid+" from list of dialogs",
                            HBCIUtils.LOG_DEBUG);
                    DialogMgr.getInstance().removeDialog(dialogid);
                }
            }
        }
    }
    
    public void quit()
    {
        HBCIUtils.log("stopping cleanup thread",HBCIUtils.LOG_DEBUG);
        quit=true;
        synchronized (this) {
            notify();
        }
    }
}
