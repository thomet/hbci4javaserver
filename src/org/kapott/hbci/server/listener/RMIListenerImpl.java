
/*  $Id: RMIListenerImpl.java,v 1.3 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.listener;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.server.DialogMgr;
import org.kapott.hbci.server.HBCICallbackInternal;
import org.kapott.hbci.server.ServerData;

public class RMIListenerImpl 
    extends UnicastRemoteObject 
    implements RMIListener
{
    private boolean initialized;
    
    public RMIListenerImpl()
        throws RemoteException
    {
        initialized=false;
    }
    
    public String handleMessage(StringBuffer msg)
    {
        try {
            if (!initialized) {
                HBCIUtils.init(null,null,new HBCICallbackInternal());
                initialized=true;
            } else {
                HBCIUtils.initThread(null,null);
            }
            HBCIUtils.setParam("connection.id","PinTan");
            HBCIUtils.setParam("log.loglevel.default",Integer.toString(ServerData.getInstance().getLogLevel()));
            
            StringBuffer plainMsg=new StringBuffer(new String(HBCIUtils.decodeBase64(msg.toString()),"ISO-8859-1"));
            String ret=HBCIUtils.encodeBase64(DialogMgr.getInstance().handleMessage(plainMsg,"PinTan").getBytes("ISO-8859-1"));
            return ret;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
