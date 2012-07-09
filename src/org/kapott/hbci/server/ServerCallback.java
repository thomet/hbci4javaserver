
/*  $Id: ServerCallback.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import java.util.Date;

/** <p>Schnittstelle fr die Reaktion auf Ereignisse. Dieses Interface muss von
    der HBCI-Server-Anwendung implementiert werden, um auf Ereignisse zu reagieren,
    die vom HBCI4Java-Server-Code erzeugt werden. Zur Zeit gibt es nur zwei
    "Ereignisse": die Ausgabe einer Log-Information und das Eintreffen eines
    Auftragssegmentes innerhalb einer Kundennachricht. Spter werden hier noch
    mehr Callbacks zu finden sein, so zum Beispiel fr das Ereignis "Nutzer
    hat neue Schlssel eingereicht" (wird zur Zeit vllig transparent vom
    HBCI4Java-Server-Code behandelt)</p>
    <p>In der Regel ist beim Auftreten eines solchen Callbacks der Kernel-Parameter
    <code>connection.id</code> (kann mit <code>HBCIUtils.getParam("connection.id")</code>
    ermittelt werden) auf einen Wert gesetzt, der fr jede Client-Verbindung
    eindeutig ist. Damit knnen also alle Callbacks, die zu einer Client-Verbindung
    gehren, auch als solche erkannt werden (z.B. um die Logausgaben pro HBCI-Session
    in separate Dateien zu schreiben, kann der Wert dieses Parameters als Teil
    des Dateinamens fr die jeweilige Log-Datei verwendet werden). Ausnahme sind 
    einige <code>log</code>-Callbacks, die von einem "allgemeineren" Teil des 
    HBCI-Server-Frameworks generiert werden (Initialiserung, Warten auf Verbindungen, 
    usw.) und die keiner bestimmten Client-Connection zugeordnet sind. In diesem Fall 
    liefert die Abfrage von <code>connection.id</code> <code>null</code>.</p>
    <p>Es ist zu beachten, dass alle hier aufgefhrten Callbacks aus mehreren 
    Threads gleichzeitig aufgerufen werden knnen, d.h. diese Methoden mssen auf
    jeden Fall thread-safe implementiert werden. Es wird je Client-Connection ein
    separater Thread gestartet, jeder dieser Threads kann unabhngig von den anderen
    (und *nicht* vom HBCI-Server-Framework synchronisiert) diese Callback-Methoden
    aufrufen.</p> */
public interface ServerCallback 
{
    /** Wird beim Eintreffen eines Auftragssegmentes aufgerufen. Dieser Callback
        wird genau einmal fr jedes Auftragssegment einer eingegangenen Kunden-Nachricht
        aufgerufen. Als "Auftragssegmente" werden alle die Segmente verstanden,
        die Auftragsdaten fr einen HBCI-Geschftsvorfall enthalten (Einreichen
        einer neuen berweisung, Abfrage von Saldoinformationen, usw.). 
        <code>context</code> enthlt Informationen ber den aktuellen Dialog
        (Kunden-ID usw.) sowie die eigentlichen Auftragsdaten. Die Rckgabedaten
        fr diesen Auftrag (Auftragsdaten, Fehlermeldungen) werden von der 
        HBCI-Server-Anwendung ebenfalls ber dieses <code>context</code>-Objekt
        an den HBCI4Java-Server-Code bergeben. Das <code>context</code>-Objekt
        stellt also die eigentliche Schnittstelle zwischen der HBCI-Anwendung und
        dem HBCI4Java-Sever-Code dar.
        @param context enthlt Informationen zum eingegangenen Auftrag und dient zur
                       Speicherung der Antwortdaten */
    public void handleGV(JobContext context);
    
    /** <p>HBCI-Server-Code hat Log-Ausgabe erzeugt. Die Argumente dieser Methode
        sind quivalent zu denen aus {@link org.kapott.hbci.callback.HBCICallback#log(String,int,Date,StackTraceElement)
        HBCICallback.log()}. Tatschlich stammen sie auch aus genau dieser Methode. Die
        hier auflaufenden Meldungen betreffen Server-interne Details und sind
        praktisch nur fr Debugging-Zwecke nutzbar.</p>
        <p>Soll das Backend-System der Bank
        auf bestimmte Ausgaben reagieren, so knnte dieses Verhalten zwar als
        quick-and-dirty hack erst mal implementiert werden, besser ist es aber,
        das jeweilige Ereignis als "richtigen" Callback zu behandeln (dann muss
        natrlich eine entsprechende Methode in diesem Interface definiert sein).
        Grund ist, dass die Log-Ausgaben nicht als offizielles API zu verstehen
        sind und sich jederzeit ndern knnen, deshalb ist eine Reaktion auf
        bestimmte Log-Ausgaben nicht zu empfehlen.</p>*/
    public void log(String msg,int level,Date date,StackTraceElement trace);
}
