
/*  $Id: ListenerPinTan.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.kapott.hbci.manager.HBCIUtils;

public class ListenerPinTan 
    implements ConnectionListener
{
    public String getName()
    {
        return "PinTan";
    }
    
    public void start()
    {
        HBCIUtils.log("starting rmi server as pintan listener",HBCIUtils.LOG_DEBUG);
        
        try {
            Registry reg=null;

            try {
                reg=LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                HBCIUtils.log("starting new registry",HBCIUtils.LOG_DEBUG);
            } catch (Exception e) {
                reg=LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
                HBCIUtils.log("using already running registry",HBCIUtils.LOG_DEBUG);
            }
            
            reg.rebind("pintanListener",new RMIListenerImpl());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    public void stop()
    {
        HBCIUtils.log("removing pintan object from rmi registry",HBCIUtils.LOG_DEBUG);
        
        try {
            Registry reg=LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            reg.unbind("pintanListener");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
}
