
/*  $Id: HBCIServer.java,v 1.3 2005/06/10 18:03:02 kleiner Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2005  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.server;

import java.util.Iterator;
import java.util.List;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.server.datastore.DataStore;
import org.kapott.hbci.server.listener.ConnectionListener;

/** Diese Klasse reprsentiert einen HBCI-Server. Eine HBCI-Server-Anwendung muss
    eine Instanz dieser Klasse erzeugen. Anschlieend kann der HBCI-Server mit
    {@link #start()} gestartet werden. */
public class HBCIServer 
{
    private CleanupThread cleanup;
    
    /** Erzeugen einer HBCI-Server-Instanz. Intern wird eine Reihe von Datenstrukturen
        fr den Betrieb eines HBCI-Servers initialisiert. Die beiden bergebenen 
        Argumente mssen Objekte sein, die die jeweiligen Interfaces implementieren
        (und drfen nicht <code>null</code> sein!). Die Methoden dieser Interfaces
        werden vom Server-Code aus aufgerufen, um mit der HBCI-Server-Anwendung
        (und damit indirekt wahrscheinlich mit dem Backend-System der Bank) zu
        kommunizieren.
        @param dataStore Objekt, welches intern benutzt wird, um Zugriff auf Laufzeitdaten
        des Servers zu erhalten (Nutzerkennungen, untersttzte Geschftsvorflle usw.)
        @param callback Objekt, dessen Methoden aufgerufen werden, wenn whrend eines
        HBCI-Dialoges mit einem HBCI-Client bestimmte Ereignisse auftreten (z.B. das
        Eintreffen eines Auftrages). */
    public HBCIServer(DataStore dataStore,ServerCallback callback)
    {
        if (dataStore==null)
            throw new NullPointerException("dataStore must not be zero");
        if (callback==null)
            throw new NullPointerException("callback must not be zero");
        
        // Serverdaten initialisieren
        ServerData.getInstance().setCallbackObject(callback);
        HBCIUtils.init(null,null,new HBCICallbackInternal());
        HBCIUtils.setParam("log.loglevel.default",Integer.toString(dataStore.getLogLevel()));
        ServerData.getInstance().init(dataStore);
    }
    
    /** Starten des HBCI-Servers. Diese Methode blockiert, solange der Server
        luft. Soll die HBCI-Server-Anwendung also noch andere Aufgaben erledigen
        (z.B. die externe Steuerung des HBCI-Servers ermglichen [siehe
        {@link #stop()} oder {@link #reInitializeServerData()}]), so muss ein
        zustzlicher Thread erzeugt werden. Die <code>start()</code>-Methode muss
        aus der gleichen <code>ThreadGroup</code> aufgerufen werden die der
        <code>HBCIServer()</code>-Constructor. */
    public void start()
    {
        try {
            HBCIUtils.log("starting HBCI server",HBCIUtils.LOG_INFO);
            
            List listeners=ServerData.getInstance().getListeners();
            for (Iterator i=listeners.iterator();i.hasNext();) {
                final ConnectionListener listener=(ConnectionListener)i.next();
                String                   name=listener.getName();
                
                HBCIUtils.log("starting connection listener '"+name+"'",
                        HBCIUtils.LOG_INFO);

                new Thread(new ThreadGroup("listener-"+name),"listener-"+name) {
                    public void run() {
                        HBCIUtils.initThread(null,null);
                        HBCIUtils.setParam("log.loglevel.default",
                                Integer.toString(ServerData.getInstance().getLogLevel()));
                        listener.start();
                    }
                }.start();
            }
            
            (cleanup=new CleanupThread()).start();

            HBCIUtils.log("falling asleep",HBCIUtils.LOG_DEBUG);
            synchronized (this) {
                wait();
            }
            HBCIUtils.log("server is exiting",HBCIUtils.LOG_DEBUG);
        } catch (Exception e) {
            throw new HBCI_Exception("error while creating server socket",e);
        }
    }
    
    /** <p>Aktualisieren der vom HBCI-Server verwendeten Laufzeitdaten. Beim Starten des
        HBCI-Servers (siehe {@link #start()}) wird das <code>dataStore</code>-Objekt
        (siehe {@link #HBCIServer(DataStore,ServerCallback)}) benutzt, um einige Daten
        der Laufzeitumgebung zu initialisieren. Whrend der Laufzeit des Servers werden
        diese Daten nicht immer wieder ber dieses Interface abgefragt, sondern es wird
        ein Groteil dieser Informationen innerhalb des HBCI4Java-Server-Codes
        gecacht. Eine nachtrgliche Vernderung der Daten, die von den
        <code>dataStore</code>-Methoden zurckgegeben werden, hat also u.U. keine
        Auswirkung auf den HBCI-Server.</p>
        <p>Durch den Aufruf dieser Methode wird der HBCI-Server gezwungen, smtliche
        Caches zu lschen und alle bentigten Daten erneut ber das <code>dataStore</code>-Objekt
        abzufragen.</p>
        <p>Diese Methode kann natrlich nur aufgerufen werden, wenn der Server luft
        (siehe {@link #start()}). Da ein laufender Server aber die <code>start()</code>-Methode
        blockiert, muss die Server-Anwendung mehrere Threads realisieren, wobei in einem
        Thread die <code>start()</code>-Methode (und damit der Server) luft, aus einem
        andern Thread heraus kann <code>reInitializeServerData()</code> aufgerufen werden.</p>*/
    public void reInitializeServerData()
    {
        ServerData.getInstance().reInitializeServerData();
    }
    
    /** Aktualisieren der server-internen Daten fr einen bestimmten Nutzer.
        Es werden die serverseitig gecachten nutzerbezogenen Daten fr die
        angegebene <code>userid</code> als ungltig markiert, so dass diese 
        vor der nchsten Verwendung neu vom 
        {@link org.kapott.hbci.server.datastore.DataStore} abgeholt werden.
        Es gelten die gleichen Anmerkungen wie fr die Methode
        {@link #reInitializeServerData()}. 
        @param userid Nutzer-ID, fr die die Daten vor der nchsten Verwendung
        neu geladen werden sollen. */
    public void reInitializeUserData(String userid)
    {
        ServerData.getInstance().reInitializeUserData(userid);
    }
    
    /** Damit kann ein laufender Server wieder gestoppt werden. Es gelten die gleichen
        Anmerkungen zum MultiThreading wie bei der Methode {@link #reInitializeServerData()}.*/
    public void stop()
    {
        cleanup.quit();
        
        List listeners=ServerData.getInstance().getListeners();
        for (Iterator i=listeners.iterator();i.hasNext();) {
            final ConnectionListener listener=(ConnectionListener)i.next();
            String name=listener.getName();
            
            HBCIUtils.log("stopping connection listener '"+name+"'",
                    HBCIUtils.LOG_INFO);
            listener.stop();
        }
        
        HBCIUtils.log("awaking server main thread",HBCIUtils.LOG_DEBUG);
        synchronized (this) {
            notify();
        }
    }
}
