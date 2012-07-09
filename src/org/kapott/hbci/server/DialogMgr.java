
/*  $Id: DialogMgr.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

    This file is part of hbci4java-server
    Copyright (C) 2001-2005 Stefan Palme

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

import java.util.Hashtable;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;

public class DialogMgr
{
    private static DialogMgr _instance;
    
    private Hashtable dialogs;
    
    public synchronized static DialogMgr getInstance()
    {
        if (_instance==null) {
            _instance=new DialogMgr();
        }
        return _instance;
    }
    
    private DialogMgr()
    {
        this.dialogs=new Hashtable();
    }
    
    public String handleMessage(StringBuffer msg,String passportType)
    {
        HBCIUtils.log("have to handle incoming msg: "+msg,HBCIUtils.LOG_DEBUG2);
        
        // extract dialogid
        int dotPos=0;
        int prevDotPos=-1;
        int counter=0;
        
        while (counter<4 && dotPos!=-1) {
            prevDotPos=dotPos;
            dotPos=msg.indexOf("+",dotPos+1);
            counter++;
        }
        
        if (dotPos==-1)
            throw new HBCI_Exception("invalid message - can not extract dialogid");
        
        String dialogid=msg.substring(prevDotPos+1,dotPos);
        HBCIUtils.log("found dialog with dialogid "+dialogid,HBCIUtils.LOG_DEBUG);
        
        // get dialog for dialogid
        Dialog dialog=(Dialog)dialogs.get(dialogid);
        if (dialog==null) {
            dialog=new Dialog(passportType);
        }
        
        // use dialog for handling message
        return dialog.handleMessage(msg);
    }
    
    public void addDialog(String dialogid,Dialog dialog)
    {
        dialogs.put(dialogid,dialog);
    }
    
    public void removeDialog(String dialogid)
    {
        dialogs.remove(dialogid);
    }
    
    public Hashtable getDialogs()
    {
        return dialogs;
    }
}
