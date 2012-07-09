
/*  $Id: DataStore.java,v 1.3 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.datastore;

import java.util.Properties;

import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.server.StatusProtEntry;
import org.kapott.hbci.structures.Konto;

/** <p>Schnittstelle, die vom Server-Code fr den Zugriff auf Laufzeitdaten
    benutzt wird. Beim Erzeugen eines HBCI-Server-Objektes (siehe
    {@link org.kapott.hbci.server.HBCIServer#HBCIServer(DataStore,org.kapott.hbci.server.ServerCallback) 
    HBCIServer()}) muss ein Objekt angegeben werden, welches dieses Interface 
    implementiert. Der HBCI4Java-Server-Code benutzt die Methoden dieser 
    Schnittstelle, um Daten, die zur Konfiguration, Benutzerverwaltung usw. 
    bentigt werden, in Erfahrung zu bringen.</p>
    <p>Somit sind die Laufzeitdaten eines HBCI-Servers von der
    eigentlichen Server-Implementation losgelst. Durch die Verwendung eines
    Interfaces kann ein konkreter Server diese Daten auf beliebige Art und
    Weise bereitstellen. Fr einen allerersten Server-Test knnten die jeweiligen
    Methoden beispielsweise feste Rckgabewerte (also statische Daten)
    zurckliefern. In einer "echteren" Implementation knnten die Daten aus
    einer Datenbank kommen, die evtl. auch zur Laufzeit des Servers gendert
    wird (siehe dazu auch 
    {@link org.kapott.hbci.server.HBCIServer#reInitializeServerData()} !!).</p>
    <p>In der Beschreibung des API wird davon ausgegangen, dass der zu implementierende
    HBCI-Server tatschlich innerhalb einer Bank luft, deshalb auch 
    Beschreibungen wie "<em>Bankleitzahl fr Kreditinstitut zurckgeben</em>".
    Soll der HBCI-Server nur innerhalb einer Testumgebung aufgesetzt werden, so
    ist hier natrlich prinzipiell jede BLZ mglich, diese muss dann aber auch
    clientseitig verwendet werden. Das gilt analog auch fr alle anderen Daten,
    die mit diesem Interface verwaltet werden.</p>*/
public interface DataStore 
{
    /** Zu untersttzende Verbindungstypen zurckgeben. Damit ermittelt das
     *  <em>HBCI4Java</em>-Server-Framework, welche Verbindungsarten aktiviert
     *  werden sollen. Gltige Werte sind "<code>TCP</code>" fr "normale"
     *  TCP-Verbindungen ber Port 3000 sowie "<code>PinTan</code>" fr 
     *  Verbindungen via HBCI-PIN/TAN. Zum Einrichten des PIN/TAN-Listeners siehe
     *  Datei <code>README.PinTan</code> */
    public String[] getListeners();
    
    // BPD data
    /** Lndercode fr Kreditinstitut zurckgeben.
        @return Lndercode (z.B. "<code>DE</code>") */
    public String getCountry();
    /** Bankleitzahl fr Kreditinstitut zurckgeben
        @return BLZ */
    public String getBLZ();
    /** Namen des Kreditinstitutes zurckgeben
        @return Name der Bank */
    public String getKIName();
    /** <p>Zurckgeben der Kommunikationsparameter. Damit ist gemeint, welche
        Kommunikationspfade genutzt werden knnen, um diesen Server zu erreichen.
        Fr jeden zu realisierenden Pfad ist genau ein <code>String[]</code>
        zurckzugeben.</p>
        <p>Ein solches <code>String[]</code> fr einen Kommunikationspfad enthlt
        folgende Daten in dieser Reihenfolge: Typ des Dienstes, Adresse, Adresszusatz,
        Filter, Filterversion. (diese Daten entsprechen den Daten aus dem HBCI-Segment
        "<em>Kommunikationszugang zurckmelden</em>".
        Eine Server-Implementation wird diese Methode also in etwa wiefolgt realisieren:</p>
        <pre>
public String[][] getCommData(String hbciVersion)
{
    return new String[][] {{"2","192.168.1.1",null,null,null},
                           {"3","https://192.168.1.1/pintan/PinTanServlet",null,"MIM","1"}};
}
        </pre>
        <p>Der HBCI4Java-Server-Code fragt fr jede zu untersttzende HBCI-Version separat
        die Kommunikationsdaten ab. Im Parameter <code>hbciVersion</code> wird angegeben,
        fr welche HBCI-Version er die Daten gerade bentigt. Da diese Informationen i.d.R.
        fr jede HBCI-Version identisch sind, wird dieser Parameter bei der Erstellung der
        Rckgabedaten meist ignoriert.</p>
        @param hbciVersion HBCI-Version, fr die diese Daten gelten sollen
        @return Array mit Kommunikationszugangs-Informationen, wobei jedes Array-Element
                wiederum ein String-Array mit den eigentlichen Daten pro Zugang ist*/
    public String[][] getCommData(String hbciVersion);
    
    /** Menge der untersttzen Sprachen zurckgeben. Es muss ein <code>String[]</code>
        zurckgegeben werden, wobei jedes Arrayelement eine Sprache kennzeichnet.
        Gltige Sprachcodes sind "<code>DE</code>", "<code>EN</code>" und "<code>FR</code>".
        @param hbciVersion HBCI-Version, fr die diese Informationen gelten sollen
        @return Array mit untersttzten Sprachen */
    public String[] getSuppLangs(String hbciVersion);
    
    /** Gibt die Standard-Sprache des HBCI-Servers zurck. Der hier zurckgegebene Wert
        muss einer der Werte aus {@link #getSuppLangs(String)} sein.
        @param hbciVersion HBCI-Version, fr die diese Standardsprache gelten soll
        @return Standardsprache des Institutes */
    public String getDefaultLang(String hbciVersion);
    
    /** Welche HBCI-Versionen soll der HBCI-Server untersttzen? Es muss ein Array
        zurckgegeben werden, wobei jedes Element eine untersttzte HBCI-Version
        angibt. Gltige Werte sind "<code>201</code>", "<code>210</code>" und
        "<code>220</code>". Wenn HBCI via PIN/TAN untersttzt werden soll, muss
        hier mindestens die Version "<code>220</code>" angegeben werden.
        @return Array der zu untersttzenden HBCI-Versionen */
    public String[] getSuppHBCIVersions();
    
    /** <p>Welche Sicherheitsmechanismen soll der HBCI-Server untersttzen? Es muss
        ein Array zurckgegeben werden, wobei jeder zu untersttzende 
        Sicherheitsmechanismus einem Element entspricht. Jedes Element ist wiederum
        ein <code>String[]</code>, bei welchem das erste Element den Typ und das
        zweite die Version des jeweiligen Mechanismus' angibt. Als Typen sind
        prinzipiell "<code>RDH</code>" und "<code>DDV</code>" mglich, als Versions
        bis jetzt immer "<code>1</code>". Zur Zeit wird aber nur der Mechanismus
        <code>RDH</code> auch tatschlich vom HBCI4Java-Server-Code implementiert.</p>
        <p>Soll auch HBCI-PIN/TAN-Untersttzung aktiviert werden, so muss an dieser
        Stelle kein spezieller Sicherheitsmechanismus dafr angegeben werden.
        Statt dessen muss ein entsprechender Kommunikationszugang zurckgemeldet
        werden, und es muss der Listener "<code>PinTan</code>" aktiviert werden.</p>
        @param hbciVersion HBCI-Version, fr die diese Informationen gelten sollen
        @return Array mit Informationen ber die zu untersttzenden Sicherheitsmechanismen */
    public String[][] getSuppSecMethods(String hbciVersion);
    
    /** Welche Kompressionsverfahren soll der HBCI-Server untersttzen? Da Kompression
        von <em>HBCI4Java</em> noch nicht untersttzt wird, ist hier vorerst <code>null</code>
        oder ein leeres Array (<code>new String[0][]</code>) zurckzugeben.
        @param hbciVersion HBCI-Version, fr die diese Informationen gelten sollen
        @return zu untersttzende Komressionsverfahren */
    public String[][] getSuppCompMethods(String hbciVersion);
    
    /** Wieviele Auftragssegmente sollen pro HBCI-Nachricht mglich sein?
        @param hbciVersion HBCI-Version, fr die diese Informationen gelten soll
        @return Anzahl der maximalen Auftragssegmente pro HBCI-Auftragsnachricht */
    public int getNumOfGVsPerMsg(String hbciVersion);
    
    /** Maximale Gre einer HBCI-Nachricht in KiloByte (1024 Byte). Diese Angabe gilt
        fr den HBCI-Client, der HBCI4Java-Server-Code generiert zur Zeit u.U.
        grere Nachrichten.
        @param hbciVersion HBCI-Version, fr die diese Informationen gelten soll
        @return Maximalgrer einer HBCI-Nachricht in KB */
    public int getMaxMsgSize(String hbciVersion);
    
    /** Gibt an, ob der HBCI-Server anonyme Zugnge erlauben soll.
        @return anonyme Zugnge erlaubt? */
    public boolean isAnonAllowed();
    
    /** Gibt die Versionsnummer der aktuell gltigen BankParameterDaten zurck. Die
        BPD-Versionsnummer sollte erhht werden, wenn Konfigurationsdaten ndern,
        die ber dieses Interface (<code>DataStore</code>) zurckgegeben werden.
        Spter wird u.U. eine automatische Versionsnummernverwaltung im 
        HBCI4Java-Server-Code implementiert werden, die bei Datenvernderungen 
        automatisch die Versionsnummer aktualisiert.
        <code>hbciVersion</code> kann hier zustzliche auch "<code>plus</code>"
        sein, falls Untersttzung fr HBCI-PIN/TAN aktiviert ist.
        @param hbciVersion HBCI-Version, deren BPD-Version hier gemeint ist
        @return Versionsnummer der aktuellen BPD */
    public int  getBPDVersion(String hbciVersion);
    
    // GV data
    /** Liste aller untersttzten HBCI-Geschftsvorflle zurckgeben. Eine Liste aller
        mglichen Werte erhlt man mit dem Tool 
        {@link org.kapott.hbci.tools.ShowLowlevelGVs}, die Zeilen, die mit
        "<em>jobname:</em>" beginnen, enthalten die jeweils mglichen GV-Bezeichner
        fr die einzelnen Geschftsvorflle.
        <code>hbciversion</code> kann hier zustzliche auch "<code>plus</code>"
        sein, falls Untersttzung fr HBCI-PIN/TAN aktiviert ist.
        @param hbciversion HBCI-Version, fr die die Liste der untersttzten GVs
        zurckgegeben werden soll
        @return Liste der zu untersttzenden HBCI-Geschftsvorflle */
    public String[] getSupportedGVs(String hbciversion);
    
    /* TODO doku fehlt */
    public Properties getPinTanGVs();
    
    /** Welche Versionen eines Geschftsvorfalles sollen fr eine bestimmte HBCI-Version
        untersttzt werden? Die zur Zeit prinzipiell von <em>HBCI4Java</em> untersttzten
        Versionsnummern knnen mit dem Tool {@link org.kapott.hbci.tools.ShowLowlevelGVs}
        (siehe auch {@link #getSupportedGVs(String)}) ermittelt werden.
        <code>hbciVersion</code> kann hier zustzliche auch "<code>plus</code>"
        sein, falls Untersttzung fr HBCI-PIN/TAN aktiviert ist.
        @param gvname Name des Geschftsvorfalles, fr den die untersttzten Versionen
        zurckgemeldet werden sollen
        @param hbciversion HBCI-Version, fr die diese Daten gelten sollen
        @return Array mit Versionsnummern */
    public int[] getGVVersions(String gvname,String hbciversion);
    
    /** Gibt zurck, wie viele Auftragssegmente einer Geschftsvorfallart pro Auftragsnachricht
        erlaubt werden sollen. Dieser Wert wird zwar in die BPD eingestellt, die
        der HBCI-Client erhlt, wird aber bei eingehenden Nachrichten noch nicht
        vom HBCI4Java-Server-Code berprft (***)
        @param gvname Name des Geschftsvorfalles, fr den die Maximalanzahl an Segmenten
        pro Nachricht zurckgegeben werden soll
        @param gvversion Versionsnummer des entsprechenden Geschftsvorfalles
        @return Maximalanzahl an Auftragssegmenten dieses Typs */
    public int getGVMaxNum(String gvname,int gvversion);
    
    /** Wieviele Signaturen sind fr einen bestimmten Geschftsvorfall ntig? Hier wird
        i.d.R. "<code>1</code>" zurckgegeben. Sollen Mehrfachsignaturen fr einen
        GV ntig sein, so kann hier natrlich auch ein hherer Wert zurckgegeben werden.
        Dieser Parameter wird vom HBCI4Java-Server-Code bei eingehenden Nachrichten noch
        nicht berprft.
        @param gvname Name des Geschftsvorfalles, fr die Mindestanzahl Signaturen 
        ermittelt werden soll
        @param gvversion Versionsnummer des entsprechenden Geschftsvorfalles
        @return Mindestanzahl an bentigten Signaturen fr diesen GV */
    public int getGVMinSigs(String gvname,int gvversion);
    
    /** Ermitteln der Parameter fr einen Geschftsvorfall. Hier muss ein 
        <code>Property</code>-Objekt zurckgegeben werden, welches als <code>key</code>
        den Namen eines GV-Parameters und als Wert den entsprechenden Wert
        enthlt. Es gibt leider noch kein externes Tool, mit welchem sich die
        bentigten Parameter je Geschftsvorfall anzeigen lassen (***). In einem
        <em>HBCI4Java</em>-Client-Programm kann das aber mit der Methode
        {@link org.kapott.hbci.GV.HBCIJob#getJobRestrictions()} ermittelt werden.
        Auerdem hilft ein Blick in die <em>HBCI4Java</em>-interne Spezifikation
        der HBCI-Nachrichten, die Segmente "<code><em>gvname</em>Par*</code>" bzw.
        die DEG "<code>Par<em>gvname</em>*</code>" helfen hier weiter.
        @param gvname Name des Geschftsvorfalles, fr den die Parameter ermittelt werden
        sollen
        @param gvversion Versionsnummer des GV, fr den die Parameter bentigt werden
        @return <code>Property</code>-Objekt mit Parameterdaten */
    public Properties getGVParams(String gvname,int gvversion);
    
    // keys
    /** Wird vom HBCI4Java-Server-Code bei dessen Initialisierung aufgerufen, um
        die Signierschlssel des Institutes fr den RDH-Zugang zu
        erhalten. Das Array muss als erstes Element den ffentlichen und als zweites
        den privaten Signierschlssel enthalten.
        Sind noch keine Schlssel vorhanden, so kann hier <code>null</code>
        zurckgegeben werden. In diesem Fall erzeugt der HBCI4Java-Server-Code
        selbststndig neue Signierschlssel und bergibt sie der Methode
        {@link #setSigKeys(HBCIKey[])}, damit die Server-Anwendung diese Schlssel
        speichern und bei der nchsten Schlssel-Initialisierung zurckgeben kann.
        @return Array mit ffentlichem und privaten Signierschlssel der Bank */
    public HBCIKey[] getSigKeys();

    /** Wird vom HBCI4Java-Server-Code bei dessen Initialisierung aufgerufen, um
        die Chiffrierschlssel des Institutes fr den RDH-Zugang zu
        erhalten. Das Array muss als erstes Element den ffentlichen und als zweites
        den privaten Chiffrierschlssel enthalten.
        Sind noch keine Schlssel vorhanden, so kann hier <code>null</code>
        zurckgegeben werden. In diesem Fall erzeugt der HBCI4Java-Server-Code
        selbststndig neue Chiffrierschlssel und bergibt sie der Methode
        {@link #setCryptKeys(HBCIKey[])}, damit die Server-Anwendung diese Schlssel
        speichern und bei der nchsten Schlssel-Initialisierung zurckgeben kann.
        @return Array mit ffentlichem und privaten Chiffrierschlssel der Bank */
    public HBCIKey[] getCryptKeys();
    
    /** Speichern eines neuen Signierschlsselpaares. Nachdem der HBCI4Java-Server-Code
        ein neues Signierschlsselpaar erzeugt hat, ruft er diese Methode auf, damit
        die HBCI-Server-Anwendung diese Schlssel permanent speichern kann.
        @param keys Array mit neuem ffentlichem und privatem Signierschlssel der Bank */
    public void setSigKeys(HBCIKey[] keys);

    /** Speichern eines neuen Chiffrierschlsselpaares. Nachdem der HBCI4Java-Server-Code
        ein neues Chiffrierschlsselpaar erzeugt hat, ruft er diese Methode auf, damit
        die HBCI-Server-Anwendung diese Schlssel permanent speichern kann.
        @param keys Array mit neuem ffentlichem und privatem Chiffrierschlssel der Bank */
    public void setCryptKeys(HBCIKey[] keys);
    
    /** Wird aufgerufen, wenn der HBCI4Java-Server-Code die aktuelle Signatur-ID
        fr servergenerierte Signaturen bentigt. Diese Methode wird nur einmal beim
        Initialisieren des Servers aufgerufen (und beim Aufruf der Methode
        {@link org.kapott.hbci.server.HBCIServer#reInitializeServerData()}), danach
        wird die Signatur-ID intern verwaltet.
        @return aktuelle Signatur-ID */
    public Long getSigId();
    
    /** Speichern des aktuellen Wertes der Signatur-ID. Sobald die Signatur-ID intern
        verndert wurde, wird diese Methode aufgerufen, um der HBCI-Anwendung
        die Mglichkeit zu geben, die neue Signatur-ID permanent zu speichern
        @param sigid die genderte Signatur-ID */
    public void setSigId(Long sigid);
    
    // userdata
    /** Liste der gltigen Benutzerkennungen ermitteln. Wird vom Server bei
        dessen Initialisierung aufgerufen, um die Menge aller gltigen
        Benutzerkennungen zu ermitteln
        @return Array mit Benutzerkennungen fr den HBCI-Server */
    public String[] getUserIds();
    /** Gibt zu einer Benutzerkennung die gltigen Kunden-IDs zurck. Wenn
        "eigentlich" keine Kunden-IDs verwendet werden, so muss in dem zurckgegebenen
        Array trotzdem zumindest ein Element eingestellt werden, welches in 
        diesem Fall identisch mit der Benutzerkennung sein muss.
        @param userid Nutzerkennung, fr die die Kunden-IDs bentigt werden
        @return Array mit Kunden-IDs fr diese Benutzerkennung.*/
    public String[] getCustomerIds(String userid);
    /** Ermitteln der ffentlichen Schlssel eines Nutzers. Diese Methode wird
        vom HBCI4Java-Server-Code aufgerufen, wenn die ffentlichen Schlssel fr
        eine bestimmte Nutzerkennung bentigt werden. Sind noch keine Schlssel fr
        diesen Nutzer eingereicht worden, so kann <code>null</code> zurckgegeben
        werden. Das erste Element dieses Arrays muss den ffentlichen Signier-,
        das zweite den ffentlichen Chiffrierschlssel fr diese Nutzerkennung
        enthalten.
        @param userid Benutzerkennung, fr die die ffentlichen Schlssel bentigt werden
        @return Array mit ffentlichem Signier- und Chiffrierschlssel des Nutzers oder
        <code>null</code>, wenn noch keine Schlssel eingereicht wurden*/
    public HBCIKey[] getUserKeys(String userid);
    
    /** Ermitteln der PIN eines Nutzers fr das HBCI-PIN/TAN-Verfahren
     *  @param userid Nutzerkennung, fr die die PIN zurckgegeben werden soll
     *  @return PIN des jeweiligen Nutzers fr das HBCI-PIN/TAN-Verfahren */
    public String getUserPIN(String userid);
    
    /** Wird aufgerufen, um eine neue PIN fr einen Nutzer zu setzen. Gemeint ist
     *  hier die PIN fr das HBCI-PIN/TAN-Verfahren.
     *  @param userid Nutzerkennung, fr die die PIN gendert werden soll
     *  @param pin neue PIN */
    public void setUserPIN(String userid,String pin);
    
    /** Zurckgeben der TAN-Liste eines Nutzers. Das zurckgegebene String-Array
     *  muss fr jede TAN der aktuellen TAN-Liste einen String im Format
     *  "TAN:0" oder "TAN:1" enthalten. TAN ist dabei durch eine konkrete TAN
     *  zu ersetzen. "TAN:0" bedeutet, diese TAN wurde bereits verbraucht, 
     *  "TAN:1" bedeutet, dass diese TAN noch zur Verfgung steht.
     *  @param userid Nutzerkennung, fr die die TAN-Liste zurckgegeben werden soll
     *  @return String-Array mit Informationen ber TANs und deren Status 
     *  (verbraucht / nicht verbraucht) */
    public String[] getUserTANList(String userid);
    
    /** Wird aufgerufen, wenn fr einen Nutzer eine komplett neue TAN-Liste
     *  verwendet werden soll.
     *  @param userid Nutzerkennung, fr den die TAN-Liste gendert werden soll
     *  @param tans Neue TAN-Liste im Format TAN:0 oder TAN:1 fr jeden Eintrag
     *         (siehe auch {@link #getUserTANList(String)}) */
    public void setUserTANList(String userid,String[] tans);
    
    /** Wird aufgerufen, um eine TAN eines Nutzers als "benutzt" zu markieren.
     *  @param userid Nutzerkennung des Nutzers, dessen TAN-Liste aktualisiert
     *         werden soll
     *  @param tan die TAN aus der Liste, die als "benutzt" markiert werden soll */ 
    public void removeUserTAN(String userid,String tan);
    
    /** Speichern des genderten Signierschlssels eines Nutzers. Wird whrend eines
        HBCI-Dialoges ein neuer Signierschlssel durch den Nutzer eingereicht bzw. der
        aktuelle Signierschlssel gendert (Nachrichten "<em>Erstmalige Einreichung
        der Nutzerschlssel</em>" bzw. "<em>nderung der Nutzerschlssel</em>"), dann
        ruft der HBCI4Java-Server-Code diese Methode auf, damit die HBCI-Anwendung
        die neuen Nutzerschlssel permanent speichern kann.
        @param userid Nutzerkennung, zu der der neue Signierschlssel gehrt
        @param key neuer ffentlicher Signierschlssel des Nutzers*/
    public void storeUserSigKey(String userid,HBCIKey key);

    /** Speichern des genderten Chiffrierschlssels eines Nutzers. Wird whrend eines
        HBCI-Dialoges ein neuer Chiffrierschlssel durch den Nutzer eingereicht bzw. der
        aktuelle Chiffrierschlssel gendert (Nachrichten "<em>Erstmalige Einreichung
        der Nutzerschlssel</em>" bzw. "<em>nderung der Nutzerschlssel</em>"), dann
        ruft der HBCI4Java-Server-Code diese Methode auf, damit die HBCI-Anwendung
        die neuen Nutzerschlssel permanent speichern kann.
        @param userid Nutzerkennung, zu der der neue Chiffrierschlssel gehrt
        @param key neuer ffentlicher Chiffrierschlssel des Nutzers*/
    public void storeUserEncKey(String userid,HBCIKey key);
    
    /** Ermitteln der System-IDs fr einen Nutzer. 
        Existiert fr einen Nutzer noch keine System-ID (weil der Client noch
        keine "<em>Synchronisierung der System-ID</em>" durchgefhrt hat), dann
        muss hier ein leeres Array zurckgegeben werden.
        @param userid Nutzerkennung, fr die die System-IDs bentigt werden
        @return Array mit den gltigen System-IDs fr diesen Nutzer */
    public String[] getSysIds(String userid);
    
    /** Speichern einer neuen System-ID fr einen Nutzer. Fhrt ein Nutzer eine
        "<em>Synchronisation der System-ID</em>" aus, so generiert der 
        HBCI4Java-Server-Code fr diesen Nutzer eine neue System-ID und sendet ihm
        diese zurck. Gleichzeitig wird diese Methode aufgerufen, damit die
        HBCI-Server-Anwendung die neue fr diesen Nutzer gltige System-ID permanent
        abspeichern kann. Existiert die angegebene System-ID bereits, so wird
        die bereits existierende System-ID einfach berschrieben.
        @param userid Nutzerkennung, fr die die neue System-ID gespeichert werden soll
        @param sysid neue System-ID fr diesen Nutzer */
    public void addSysId(String userid,String sysid);
    
    /** Ermitteln der schon eingereichten Signatur-IDs fr eine bestimmte Nutzerkennung 
        und eine bestimmte System-ID. Wenn die System-ID nicht existiert, so wird
        eine Exception geworfen.
        @param userid Nutzerkennung, fr die die Liste der Sig-IDs ermittelt werden soll
        @param sysid System-ID, fr die die schon eingereichten Signatur-IDs ermittelt
               werden sollen.
        @return Array aus <code>long</code>-Werten, wobei jedes Element eine schon 
        eingereichte Signatur-ID darstellt */
    public long[] getSigIds(String userid,String sysid);
    
    /** Lschen der Liste schon eingereichter Signatur-IDs fr eine bestimmte System-ID.
     *  Existiert die angegebene System-ID nicht, so wird eine Exception geworfen.
        @param userid Nutzerkennung, fr die die Liste der Sig-IDs gelscht werden soll
        @param sysid System-ID, fr welche die Signatur-IDs zurckgesetzt werden sollen. */
    public void clearSigIds(String userid,String sysid);
    
    /** Hinzufgen einer Signatur-ID zur Liste der schon eingereichten Signatur-IDs.
        @param userid Nutzerkennung, deren Sig-ID-Liste aktualisiert werden soll. Wenn
        die angegebene System-ID noch nicht existiert, wird eine Exception geworfen.
        @param sysid System-ID, fr die diese Signatur-ID hinzugefgt werden soll
               (Die Doppeleinreichungskontrolle fr Signatur-IDs wird fr jede
               System-ID separat durchgefhrt)
        @param sigid Signatur-ID, die zur Liste hinzugefgt werden soll */
    public void addSigId(String userid,String sysid,long sigid);
    
    /** Ermitteln aller fr eine bestimmte Nutzerkennung gltigen Kontoverbindungen.
        Fr die durch diese Methode zurckgegeben {@link org.kapott.hbci.structures.Konto}-Objekte
        mssen mindestens die Felder <code>country</code>, <code>blz</code>, <code>number</code>,
        <code>curr</code>, <code>name</code>, <code>type</code> und <code>customerid</code> 
        belegt sein. <code>customerid</code> gibt dabei eine der Kunden-IDs aus
        {@link #getCustomerIds(String)} an, fr die Berechtigung zum Zugriff auf dieses
        Konto besteht
        @param userid Benutzerkennung, fr die die verfgbaren Kontoverbindungen zurckgemeldet
        werden sollen
        @return Array mit Kontoinformationen fr diesen Nutzer */
    public Konto[] getAccounts(String userid);
    
    /** Ermitteln der Versionsnummer der Kontoinformationen. Sobald sich Daten in den
        Kontoinformationen fr einen Benutzer (siehe {@link #getAccounts(String)}) ndern,
        muss hier eine hhere Versionsnummer zurckgemeldet werden als zu dem Zeitpunkt,
        zu dem noch die alten Kontodaten galten.
        @param userid Benutzerkennung, fr die Version der Kontoinformationen bentigt wird
        @return Versionsnummer der Kontoinformationen, die via {@link #getAccounts(String)}
        zurckgegeben werden */
    public int getAccountInfoVersion(String userid);
    
    // misc
    /** Ermitteln des Log-Levels, mit welchem der HBCI4Java-Server-Code Logausgaben
        erzeugen soll. Die Logausgaben werden via 
        {@link org.kapott.hbci.server.ServerCallback#log(String,int,java.util.Date,StackTraceElement)} zur
        HBCI-Anwendung gesandt. Gltige Werte fr das Log-Level sind in der Dokumentation
        zu {@link org.kapott.hbci.manager.HBCIUtils#LOG_DEBUG HBCIUtils} aufgefhrt.
        @return Loglevel fr HBCI4Java-Server-Logausgaben */
    public int getLogLevel();
    
    /* TODO doku fehlt */
    public void addToStatusProt(String userid,StatusProtEntry entry);
}
