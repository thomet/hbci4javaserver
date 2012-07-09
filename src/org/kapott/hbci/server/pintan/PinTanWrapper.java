
/*  $Id: PinTanWrapper.java,v 1.4 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.pintan;

import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.Naming;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kapott.hbci.server.listener.RMIListener;

public class PinTanWrapper 
    extends HttpServlet
{
    private String rmiAddr;
    
    // adresse des rmi-servers aus den web.xml-daten extrahieren
    public void init()
    {
        try {
            super.init();
            
            // initialize rmi server address
            rmiAddr=getServletConfig().getInitParameter("rmiServer");
            System.out.println("rmiServer address set to "+rmiAddr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void doPost(HttpServletRequest request,HttpServletResponse response) 
    {
        try {
            // request einlesen
            // TODO alternatives vorgehen, wenn ContentLength nicht bermittelt wird
            int          size=request.getContentLength();
            byte[]       buffer=new byte[1024];
            StringBuffer msg=new StringBuffer();
            InputStream  in=request.getInputStream();
            
            // TODO HBCIUtils.log("got content-length = "+size+" bytes",HBCIUtils.LOG_DEBUG2);
            while (size>0) {
                int len=in.read(buffer);
                msg.append(new String(buffer,0,len));
                size-=len;
            }
            
            // request an hbci-server senden
            // und response vom hbci-server empfangen
            RMIListener listener=(RMIListener)Naming.lookup("//"+rmiAddr+"/pintanListener");
            String responseMsg=listener.handleMessage(msg);
            
            // response zurcksenden
            byte[]       data=responseMsg.getBytes("ISO-8859-1");
            OutputStream out=response.getOutputStream();
            
            response.setContentLength(data.length);
            out.write(data);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
