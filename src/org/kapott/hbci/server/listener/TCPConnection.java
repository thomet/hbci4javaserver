
/*  $Id: TCPConnection.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.server.DialogMgr;
import org.kapott.hbci.server.ServerData;

public class TCPConnection
{
    private Socket socket;
    private InputStream istream;
    private OutputStream ostream;
    private boolean quit;
    
    public TCPConnection(Socket socket)
    {
        HBCIUtils.log("new incoming connection from "+socket.getRemoteSocketAddress().toString(),HBCIUtils.LOG_INFO);
        
        this.socket=socket;
        this.quit=false;
    }
    
    // handler fr eingehende verbindung starten
    public void start()
    {
        final String connid=Long.toString(new Random().nextLong()).substring(1);
        HBCIUtils.log("new connection has id '"+connid+"'",HBCIUtils.LOG_DEBUG);
        
        // neuen thread fr die bearbeitung dieser connection starten
        new Thread(new ThreadGroup(socket.getRemoteSocketAddress().toString()),"main") {
            public void run() {
                try {
                    // hbciutils fr diesen thread initialisieren
                    HBCIUtils.initThread(null,null,null);
                    HBCIUtils.setParam("connection.id",connid);
                    HBCIUtils.setParam("log.loglevel.default",Integer.toString(ServerData.getInstance().getLogLevel()));
                    
                    // datenstreams holen
                    istream=socket.getInputStream();
                    ostream=socket.getOutputStream();
                    
                    while (!quit) {
                        // nachricht entgegennehmen
                        StringBuffer msg=receiveMessage();
                        
                        // wenn da eine nachricht ankam
                        if (msg!=null) {
                            String response=DialogMgr.getInstance().handleMessage(msg,"RDH");
                            sendResponse(response);
                        } else { // sonst offensichtlich verbindungsabbruch
                            HBCIUtils.log("client has closed connection",HBCIUtils.LOG_DEBUG);
                            quit=true;
                        }
                    }
                    
                    // socket schlieen
                    HBCIUtils.log("closing connection",HBCIUtils.LOG_INFO);
                    socket.close();
                } catch (Exception e) {
                    throw new HBCI_Exception(e);
                }
            }
        }
        .start();
    }
    
    private StringBuffer receiveMessage()
    {
        try {
            StringBuffer ret=new StringBuffer();
            byte[] buffer=new byte[1024];
            int num=-1;
            int msgsize=-1;
            
            // solange noch daten verfgbar sind und solange noch
            // nicht die erforderliche anzahl bytes gelesen wurde
            while (msgsize!=0 && (num=istream.read(buffer))!=-1) {
                // datenpaket lesen
                ret.append(new String(buffer,0,num,"ISO-8859-1"));
                
                // wenn nachrichtengre noch nicht bekannt ist
                if (msgsize<0) {
                    // nachrichtengre aus msghead extrahieren
                    int firstPlus=ret.indexOf("+");
                    if (firstPlus!=-1) {
                        int secondPlus=ret.indexOf("+",firstPlus+1);
                        if (secondPlus!=-1) {
                            msgsize=Integer.parseInt(ret.substring(firstPlus+1,secondPlus));
                            HBCIUtils.log("detected message size "+msgsize+" bytes",HBCIUtils.LOG_DEBUG);
                            // restgre ist msgsize minus anzahl schon gelesener bytes 
                            msgsize-=ret.length();
                        }
                    }
                } else {
                    // restgre um anzahl der gelesenen bytes verringern
                    msgsize-=num;
                }
            }
        
            HBCIUtils.log("client message received",HBCIUtils.LOG_INFO);
            return (num!=-1)?ret:null;
        } catch (Exception e) {
            throw new HBCI_Exception("error while receiving data from client",e);
        }
    }
    
    private void sendResponse(String msg)
    {
        try {
            HBCIUtils.log("sending response message",HBCIUtils.LOG_DEBUG);
            ostream.write(msg.getBytes("ISO-8859-1"));
            ostream.flush();
            HBCIUtils.log("response sent",HBCIUtils.LOG_INFO);
        } catch (Exception e) {
            throw new HBCI_Exception("error while sending response",e);
        }
    }
}
