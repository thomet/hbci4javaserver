
/*  $Id: JobContext.java,v 1.8 2005/06/10 18:03:02 kleiner Exp $

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

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.status.HBCIRetVal;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

/** <p>Liefert Informationen ber einen eingegangenen Auftrag und nimmt Rckgabedaten
    auf. Ein Objekt dieser Klasse wird fr jedes eingegangene Auftragssegment
    einer Kundennachricht vom HBCI4Java-Server-Code erzeugt. Mit diesem 
    <code>JobContext</code>-Objekt wird dann der Callback-Handler
    {@link ServerCallback#handleGV(JobContext)} aufgerufen, der von der 
    HBCI-Server-Anwendung implementiert werden muss.</p>
    <p>Die <code>JobContext</code>-Klasse stellt auerdem einige Hilfsmethoden
    fr die leichtere Auswertung der Auftragsdaten zur Verfgung.</p>*/
public class JobContext 
{
    private Dialog conn;
    private String jobname;
    private int    jobversion;
    private Properties jobData;
    
    private ArrayList rets;
    private Properties resultData;
    
    public JobContext(Dialog conn,String jobname,int jobversion,Properties jobData)
    {
        this.conn=conn;
        this.jobname=jobname;
        this.jobversion=jobversion;
        this.jobData=jobData;
        
        this.rets=new ArrayList();
        this.resultData=new Properties();
    }
    
    /** Gibt den Namen des eingegangenen Auftragssegmentes zurck. Dabei handelt
        es sich um die gleichen Namen, die in einer HBCI4Java-Client-Anwendung mit
        {@link org.kapott.hbci.manager.HBCIHandler#getSupportedLowlevelJobs()}
        ermittelt werden knnen. Eine Liste aller mglichen Jobnamen erhlt man 
        mit dem Tool {@link org.kapott.hbci.tools.ShowLowlevelGVs}. Dieser 
        Jobname ist immer einer der Namen, die von dem implementierten 
        {@link org.kapott.hbci.server.datastore.DataStore}-Objekt mit der 
        Methode {@link org.kapott.hbci.server.datastore.DataStore#getSupportedGVs(String)}
        zurckgegeben werden (das stimmt noch nicht, es werden zur Zeit auch Auftrge
        akzeptiert, die gar nicht in den BPD aufgefhrt sind ***).
        @return interner Name des eingegangenen Auftrages */
    public String getJobName()
    {
        return jobname;
    }
    
    /** Gibt die Versionsnummer des eingegangenen Geschftsvorfalles zurck. Es handelt
        sich dabei um eine der Versionsnummern, die via
        {@link org.kapott.hbci.server.datastore.DataStore#getGVVersions(String,String)} fr
        den aktuellen {@link #getJobName() Job-Namen} ermittelt werden knnen
        (das stimmt noch nicht, es werden zur Zeit auch Auftrge
        akzeptiert, die gar nicht in den BPD aufgefhrt sind ***).
        @return Versionsnummer des Geschftsvorfall-Auftragssegmentes */
    public int getJobVersion()
    {
        return jobversion;
    }
    
    /** Gibt ein Objekt mit den Auftragsdaten des eingegangenen Auftrages zurck.
        Die fr jeden Geschftsvorfall jeweils mglichen Daten lassen sich mit dem
        Tool {@link org.kapott.hbci.tools.ShowLowlevelGVs} ermitteln.
        @return <code>Properties</code>-Objekt mit Auftragsdaten */
    public Properties getJobData()
    {
        return jobData;
    }
    
    /** Ermitteln eines bestimmten Eintrages aus den eingegangen Auftragsdaten.
        Ein Aufruf von <code>getJobData(key)</code> entspricht dem Aufruf von
        {@link #getJobData() getJobData().getProperty(key)}.
        @param key Name fr das Auftragsdatenelement, dessen Wert ermittelt werden soll
        @return Wert des entsprechenden Auftragsdatenelementes */
    public String getJobData(String key)
    {
        return jobData.getProperty(key);
    }
    
    private String getDERef(String path)
    {
        return getJobData("_deref."+path);
    }
    
    /** Gibt die Kunden-ID zurck, mit der der aktuelle HBCI-Dialog gefhrt wird
        und fr die also der aktuelle Auftrag bearbeitet werden soll
        @return aktuelle Kunden-ID */
    public String getCustomerId()
    {
        return conn.getCustomerId();
    }
    
    /** Gibt die Nutzerkennung zurck, mit der der aktuelle HBCI-Dialog gefhrt wird
        und fr die also der aktuelle Auftrag bearbeitet werden soll
        @return aktuelle Benutzerkennung */
    public String getUserId()
    {
        return conn.getUserId();
    }
    
    /** Hinzufgen eines auftragsspezifischen Rckgabewertes zur Antwortnachricht.
        Diese Methode erzeugt eine HBCI-Rckmeldung, die sich automatisch auf das aktuelle
        Auftragssegment bezieht. Diese Methode hat eine gewisse "Intelligenz" der
        folgenden Art eingebaut: Eine Erfolgsmeldung (<code>code</code> beginnt mit
        '0') wird nur dann tatschlich als Rckmeldung hinzugefgt, wenn vorher nicht
        bereits eine Fehlermeldung hinzugefgt wurde. Eine hinzugefgte Fehlermeldung
        (<code>code</code> beginnt mit '9') bewirkt, dass alle evtl. vorher 
        hinzugefgten Erfolgsmeldungen aus der Menge der Rckmeldungen entfernt
        werden und dass keine weiteren Erfolgsmeldungen hinzugefgt werden knnen.
        @param dename Name des Datenelements, auf das sich diese Meldung bezieht 
        (kann <code>null</code> sein) - fr eine sinnvolle Belegung ist die Kenntnis 
        der intern von <em>HBCI4Java</em> verwendeten HBCI-Spezifikation ntig
        @param code Codenummer der Rckmeldung (siehe HBCI-Spezifikation)
        @param text Textmeldung, die zurckgegeben werden soll
        @param params Feld mit optionalen Parametern, die die Fehlermeldung nher spezifizieren */
    public void addStatus(String dename,String code,String text,String[] params)
    {
        String deref=(dename!=null)?getDERef(dename):null;
        rets.add(new HBCIRetVal(null,deref,null,code,text,params));
    }
    
    public ArrayList getStatusData()
    {
        return rets;
    }
    
    /** Setzen eines Datenelementes fr die zu generierende Antwortnachricht. Die Namen
        (<code>key</code>) der mglichen Datenelemente je Auftragstyp knnen mit dem
        Tool {@link org.kapott.hbci.tools.ShowLowlevelGVRs} ermittelt werden. Da ein
        Auftrag u.U. mehrere Antwortdatenstze erzeugen kann (z.B. eine Saldenabfrage
        fr alle Konten gleichzeitig), kann mit dem Parameter <code>segCounter</code>
        zustzlich angegeben werden, fr den wievielten Antwortdatensatz das Datenelement
        benutzt werden soll (Zhlung beginnt bei 0!).
        @param segCounter Nummer des Antwortdatensatzes (beginnend bei 0), fr den dieses
        Datenelement gesetzt werden soll
        @param key Name des zu setzenden Antwortdatenelementes
        @param value Wert, auf das Datenelement gesetzt werden soll */
    public void setData(int segCounter,String key,String value)
    {
        resultData.setProperty(HBCIUtilsInternal.withCounter("result",segCounter)+"."+key,value);
    }
    
    public Properties getResultData()
    {
        return resultData;
    }
    
    /** Hilfmethode zum berprfen der Gltigkeit der Kreditinstitutskennung in einem
        Auftragssegment. Diese Methode berprft, ob eine Kreditinstitutskennung
        (<code><em>KIK</em></code>), welche in einem Auftragssegment bermittelt wurde,
        gltig ist (d.h. <code>KIK.country</code> und <code>KIK.blz</code> entsprechen
        den Daten des HBCI-Servers). Ist das nicht der Fall, so wird automatisch
        eine entsprechende Fehlermeldung zur Antwortnachricht hinzugefgt.
        @param header Name der KIK-Datenelementgruppe (<code>[*.]KIK</code>)
        @return <code>true</code>, wenn Daten in <code>KIK</code>-Element mit den
        Serverdaten bereinstimmen, sonst <code>false</code> */
    public boolean checkKIK(String header)
    {
        String country=getJobData(header+".country");
        String blz=getJobData(header+".blz");

        if (!country.equals(ServerData.getInstance().getCountry()) ||
                !blz.equals(ServerData.getInstance().getBLZ())) {
            addStatus(header,"9210","Ungltige Kreditinstitutskennung",null);
            return false;
        }
        
        return true;
    }
    
    /** Alle fr die aktuelle UserId/CustomerId verfgbaren Kontoverbindungen zurckmelden.
        Diese Methode ermittelt alle Konten, auf die der Nutzer, der den aktuellen Dialog
        fhrt, zugreifen darf. Dazu werden intern zunchst die fr die aktuelle
        Nutzer-ID ({@link #getUserId()}) gltigen Kontoverbindungen ermittelt
        ({@link org.kapott.hbci.server.datastore.DataStore#getAccounts(String)}). Daraus werden
        dann die Eintrge extrahiert, deren <code>.customerid</code>-Feld mit der aktuellen
        Kunden-ID ({@link #getCustomerId()}) bereinstimmt. Die so ermittelte Menge von
        Kontoverbindungen wird zurckgegeben.
        @return Liste der Konten, auf die der aktuelle Nutzer/Kunde zugreifen darf */
    public Konto[] getAllMyAccounts()
    {
        // alle konten fr user holen
        return ServerData.getInstance().getAccounts(getUserId(),getCustomerId());
    }
    
    /** Gibt alle Konten aller Kunden zurck, die an dieser Bank gefhrt werden.
        @return Feld mit allen Konten bei dieser Bank */
    public Konto[] getAllAccounts()
    {
        // alle konten fr user holen
        return ServerData.getInstance().getAllAccounts();
    }
    
    /** Hilfsmethode zum berprfen der Gltigkeit einer Kontoverbindung aus den
        Auftragsdaten des aktuellen Auftrages. Diese Methode berprft, ob die
        jeweilige Kontoverbindung (<code>header</code>) ein gltiges Konto fr den
        aktuellen Nutzer/Kunden darstellt (siehe {@link #extractMyAccount(String)}). Ist das
        der Fall, so wird das entsprechende {@link org.kapott.hbci.structures.Konto}-Objekt
        zurckgegeben, sonst <code>null</code>. Falls kein passendes Nutzerkonto
        gefunden werden konnte, so wird automatisch eine entsprechende Fehlermeldung
        zur Antwortnachricht hinzugefgt.
        @param header Name der KTV-Datenelementgruppe (<code>[*.]KTV</code>) innerhalb der
        Auftragsdaten
        @return <code>null</code>, wenn die Kontodaten kein gltiges Konto des aktuellen
        Nutzers/Kunden darstellen, sonst das entsprechende {@link org.kapott.hbci.structures.Konto}-Objekt*/
    public Konto checkKTV(String header)
    {
        Konto acc=extractMyAccount(header);
        if (acc==null)
            addStatus(header,"9210","ungltige Kontoverbindung fr aktuelle Kunden-ID",null);
        return acc;
    }
    
    private Konto findAccountInList(String header,Konto[] accounts)
    {
        boolean ok=false;
        
        // kontodaten aus job extrahieren
        String country=getJobData(header+".KIK.country");
        String blz=getJobData(header+".KIK.blz");
        String number=getJobData(header+".number");
        
        // berprfen, ob kontodaten aus request mit einem der gltigen user-konten bereinstimmt
        Konto acc=null;
        for (int i=0;i<accounts.length;i++) {
            acc=accounts[i];
            if (acc.blz.equals(blz) && 
                    acc.country.equals(country) &&
                    acc.number.equals(number)) {
                
                // konto gefunden
                ok=true;
                break;
            }
        }
        
        return ok?acc:null;
    }
    
    /** Extrahieren einer Kontoverbindung und Zurckgeben des entsprechenden 
        <code>Konto</code>-Objektes. Diese Methode gibt <code>null</code> 
        zurck, wenn es fr den aktuellen Nutzer/Kunden kein Konto mit der
        extrahierten Kontoverbindung gibt. Ansonsten wird das <code>Konto</code>-Objekt
        zurckgegeben, welches der Kontoverbindung entspricht. Es muss sich dabei
        also immer um ein Konto handeln, welches dem aktuellen Nutzer/Kunden
        gehrt. Zustzlich zu den Verbindungsdaten aus dem Auftragssegment 
        werden in dem <code>Konto</code>-Objekt weitere Informationen (wie
        Kontowhrung, Name des Inhabers) usw. aus den server-internen Datenbestnden
        eingetragen.
        @see #getAllMyAccounts()
        @param header Name der KTV-Datenelementgruppe (<code>[*.]KTV</code>) innerhalb der
        Auftragsdaten
        @return <code>null</code>, wenn die Kontodaten kein gltiges Konto des aktuellen
        Nutzers/Kunden darstellen, sonst das entsprechende {@link org.kapott.hbci.structures.Konto}-Objekt*/
    public Konto extractMyAccount(String header)
    {
        return findAccountInList(header,getAllMyAccounts());
    }
    
    /** Extrahieren einer Kontoverbindung und Zurckgeben eines entsprechenden
        <code>Konto</code>-Objektes. Diese Methode extrahiert die blichen
        Verbindungsdaten (Lndercode, Bankleitzahl, Kontonummer) und stellt ein
        <code>Konto</code>-Objekt bereit, welches mit diesen Werten initialisiert
        ist. Im Gegensatz zu {@link #extractMyAccount(String)} wird hier keine
        berprfung vorgenommen, ob das Konto dem aktuellen Nutzer gehrt. Das
        hat aber auch den Effekt, dass keine zustzlichen Kontoinformationen in
        das <code>Konto</code>-Objekt eingestellt werden (wie z.B. Whrung,
        Name des Inhabers usw.). Falls das referenzierte Konto jedoch bei dieser
        Bank gefhrt wird (siehe {@link #getAllAccounts()}), dann werden auch
        die entsprechenden zustzlichen Daten (Kunden-ID, Whrung) gesetzt.
        @param header Name der KTV-Datenelementgruppe (<code>[*.]KTV</code>) innerhalb der
        Auftragsdaten
        @return {@link org.kapott.hbci.structures.Konto}-Objekt, welches mit den
        jeweiligen Daten aus dem Auftragssegment initialisiert ist */
    public Konto extractOtherAccount(String header)
    {
        Konto acc=findAccountInList(header,getAllAccounts());
        
        if (acc==null) {
            acc=new Konto(getJobData(header+".KIK.country"),
                    getJobData(header+".KIK.blz"),
                    getJobData(header+".number"));
        }
            
        return (acc!=null && acc.number!=null)?acc:null;
    }
    
    /** Geldbetrag aus Auftragsdaten extrahieren.
        @param header Name des BTG-Datenelementes, welches extrahiert werden soll
        @return <code>Value</code>-Objekt, welches den extrahierten Geldbetrag
        reprsentiert */
    public Value extractBTG(String header)
    {
        Value value=new Value(getJobData(header+".value"),getJobData(header+".curr"));
        return (value.curr!=null)?value:null;
    }
    
    /** Extrahieren von evtl. mehrfach auftretenden Datenelemente. Das Datenelement
        mit dem Pfad <code>header</code> wird als Datenelement betrachtet, welches
        0..n mal auftreten kann. Diese Methode gibt ein <code>String[]</code>
        zurck, welches alle Vorkommen dieses DE enthlt (kann zum Beispiel
        fr die Extraktion von Verwendungszweckzeilen benutzt werden --
        <code>header=usage.usage</code>).
        @param header Pfadname des zu extrahierenden Datenelementes
        @return <code>String[]</code> (evtl. mit Lnge 0) mit den entsprechenden
        Daten */
    public String[] extractStringArray(String header)
    {
        ArrayList ret=new ArrayList();
        for (int i=0;;i++) {
            String st=getJobData(HBCIUtilsInternal.withCounter(header,i));
            if (st==null)
                break;
            ret.add(st);
        }
        return (String[])ret.toArray(new String[0]);
    }
    
    /** Extrahiert eine Datumsangabe.
        @param header Pfadname des zu extrahierenden Datenelementes
        @return extrahiertes Datum als Java-Objekt */
    public Date extractDate(String header)
    {
        String st=getJobData(header);
        return st!=null?HBCIUtils.string2Date(st):null;
    }
}
