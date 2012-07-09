
/*  $Id: ListenerTCP.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.server.ServerData;

public class ListenerTCP 
    implements ConnectionListener
{
    private boolean stopListener;
    
    public String getName()
    {
        return "TCP";
    }
        
    public void start()
    {
        HBCIUtils.log("starting tcp listener",HBCIUtils.LOG_DEBUG);
        stopListener=false;
        
        // ipadresse holen, auf der der server lauschen soll
        String host=ServerData.getInstance().getHost(2);
        if (host==null)
            throw new HBCI_Exception("no host definition for tcp/ip found");
        
        // serversocket erzeugen
        try {
            InetAddress addr=InetAddress.getByName(host);
            HBCIUtils.log("creating server socket (TCP) for "+addr,HBCIUtils.LOG_DEBUG);
            ServerSocket sock=new ServerSocket(3000,0,addr);
            
            while (!stopListener) {
                HBCIUtils.log("waiting for incoming tcp connections",HBCIUtils.LOG_DEBUG);
                Socket clientSock=null;
                
                while (!stopListener && clientSock==null) {
                    try {
                        // auf eingehende verbindungen warten
                        sock.setSoTimeout(3000);
                        clientSock=sock.accept();
                    } catch (SocketTimeoutException ex) {
                    }
                }

                if (!stopListener) {
                    // verbindung handeln (erzeugt neuen thread) und gleich weiter
                    new TCPConnection(clientSock).start();
                }
            } 
        } catch (Exception ex) {
            throw new HBCI_Exception(ex);
        }
    }

    public void stop()
    {
        HBCIUtils.log("stopping tcp listener",HBCIUtils.LOG_DEBUG);
        stopListener=true;
    }
}
