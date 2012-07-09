
/*  $Id: MyDataStore.java,v 1.5 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.demo.hbci.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.server.StatusProtEntry;
import org.kapott.hbci.server.datastore.DataStore;
import org.kapott.hbci.status.HBCIRetVal;
import org.kapott.hbci.structures.Konto;

/* eigene implementation eines DataStores, welche alle dynamischen
   Serverdaten in Dateien im Filesystem ablegt */
public class MyDataStore 
    implements DataStore 
{
    private String directory;
    
    // mit Verzeichnis fr die Speicherung der einzelnen Dateien initialisieren
    public MyDataStore(String directory)
    {
        this.directory=directory;
    }
    
    // einzeiligen Wert aus einer Datei einlesen
    public synchronized String getSingleLine(String filename)
    {
        try {
            // System.out.println("reading "+what);
            BufferedReader in=new BufferedReader(new FileReader(directory+File.separator+filename));
            String st=in.readLine();
            in.close();
            return st;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // einzeiligen Wert in einer Datei speichern
    public synchronized void storeSingleLine(String data,String filename)
    {
        try {
            // System.out.println("writing "+what);
            PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(directory+File.separator+filename)));
            out.println(data);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // mehrzeiligen Dateiinhalt als Array zurckgeben
    public synchronized String[] getMultipleLines(String filename)
    {
        try {
            // System.out.println("reading "+what);
            BufferedReader in=new BufferedReader(new FileReader(directory+File.separator+filename));
            ArrayList data=new ArrayList();
            String st;
            while ((st=in.readLine())!=null) {
                if (st.trim().length()!=0)
                    data.add(st.trim());
            }
            in.close();
            return (String[])data.toArray(new String[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public synchronized void storeMultipleLines(String[] data,String filename)
    {
        try {
            // System.out.println("writing "+what);
            PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(directory+File.separator+filename)));
            for (int i=0;i<data.length;i++) {
                out.println(data[i]);
            }
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // Schlsseldaten aus Datei einlesen (wird ber Objektserialisierung realisiert)
    private synchronized HBCIKey[] getXKeys(String prefix)
    {
        // System.out.println("reading "+prefix+" keys");
        HBCIKey[] ret=null;
        
        try {
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(directory+File.separator+prefix+"_keys"));
            ret=(HBCIKey[])in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println("  no "+prefix+"_keys found");
        }
        
        return ret;
    }

    // Schlsseldaten in Datei schreiben (via Objektserialisierung)
    private synchronized void storeXKeys(HBCIKey[] keys,String prefix)
    {
        // System.out.println("writing "+prefix+" keys");
        
        try {
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(directory+File.separator+prefix+"_keys"));
            out.writeObject(keys);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Loglevel aus Datei "loglevel" zurckgeben
    public int getLogLevel()
    {
        return Integer.parseInt(getSingleLine("loglevel"));
    }
    
    // Verbindungsarbten zurckgeben
    public String[] getListeners()
    {
        return getMultipleLines("listeners");
    }
    
    // Lndercode ist fest "DE"
    public String getCountry()
    {
        return "DE";
    }
    
    // Bankleitzahl aus Datei "blz" zurckmelden
    public String getBLZ()
    {
        return getSingleLine("blz");
    }
    
    // Name der "Testbank" aus Datei "kiname" auslesen und zurckmelden
    public String getKIName()
    {
        return getSingleLine("kiname");
    }
    
    // Kommunikationsdaten zurckmelden, IP-Adresse dafr aus Datei "address" einlesen
    public String[][] getCommData(String hbciversion)
    {
        return new String[][] {{"2",getSingleLine("address"),null,null,null},
                               {"3",getSingleLine("pintanurl"),null,"MIM","1"}};
    }

    // liste der zu untersttzenden Sprachen aus datei "languages" einlesen
    public String[] getSuppLangs(String hbciversion)
    {
        return getMultipleLines("languages");
    }
    
    // standardsprache ist immer erste sprache, die via getSuppLangs() zurckgemeldet wird
    public String getDefaultLang(String hbciversion)
    {
        return getSuppLangs(hbciversion)[0];
    }
    
    // liste der zu untersttzenden HBCI-versionsn aus datei "hbciversions" einlesen
    public String[] getSuppHBCIVersions()
    {
        return getMultipleLines("hbciversions");
    }
    
    // noch keine kompression untersttzt
    public String[][] getSuppCompMethods(String hbciversion)
    {
        return new String[0][];
    }
    
    // untersttzte sicherheitsmechanismen hardcoded (nur RDH)
    public String[][] getSuppSecMethods(String hbciversion)
    {
        return new String[][] {{"RDH","1"}};
    }
    
    // anonyme anfragen erlaubt
    public boolean isAnonAllowed()
    {
        return true;
    }
    
    // liste der zu untersttzenden geschftsvorflle aus datei "jobs_X" einlesen,
    // wobei X die hbciversion ist, fr die diese liste gelten soll
    public String[] getSupportedGVs(String hbciversion)
    {
        return getMultipleLines("jobs_"+hbciversion);
    }
    
    // liste der jobs, die via pin/tan untersttzt werden
    public Properties getPinTanGVs()
    {
        Properties ret=new Properties();
        String[]   jobs=getMultipleLines("jobs_pintan");
        
        for (int i=0;i<jobs.length;i++) {
            StringTokenizer tok=new StringTokenizer(jobs[i],":");
            String jobname=tok.nextToken();
            String needsTan=tok.nextToken();
            ret.setProperty(jobname,needsTan);
        }
        
        return ret;
    }
    
    // liste der zu untersttzenden GV-versionen fr einen GV aus der datei "JOB_versions_VERSION"
    // einlesen, wobei JOB der name des GVs und VERSION die hbciversion ist, fr die diese
    // liste gelten soll
    public int[] getGVVersions(String gvname,String hbciversion)
    {
        String[] st=getMultipleLines(gvname+"_versions_"+hbciversion);
        int[] ret=new int[st.length];
        for (int i=0;i<st.length;i++)
            ret[i]=Integer.parseInt(st[i]);
        return ret;
    }
    
    // mindestanzahl der bentigten signaturen fr einen bestimmten GV aus der datei
    // "GVNAME_minsigs" auslesen (die version des GVs wird dabei also ignoriert) 
    public int getGVMinSigs(String gvname,int gvversion)
    {
        return Integer.parseInt(getSingleLine(gvname+"_minsigs"));
    } 

    // mindestanzahl der bentigten signaturen fr einen bestimmten GV aus der datei
    // "GVNAME_maxnum" auslesen (die version des GVs wird dabei also ignoriert) 
    public int getGVMaxNum(String gvname,int gvversion)
    {
        return Integer.parseInt(getSingleLine(gvname+"_maxnum"));
    } 
    
    // parameterdaten fr einen GV aus der datei "GVNAME_params" auslesen
    public Properties getGVParams(String gvname,int gvversion)
    {
        try {
            // System.out.println("reading params of job "+gvname+gvversion);
            Properties ret=new Properties();
            InputStream in=new FileInputStream(directory+File.separator+gvname+"_params");
            ret.load(in);
            in.close();
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // max. anzahl der GVs pro hbci-nachricht aus datei "gvspermsg" auslesen
    public int getNumOfGVsPerMsg(String hbciVersion)
    {
        return Integer.parseInt(getSingleLine("gvspermsg"));
    }
    
    // max. erlaubte nachrichtengre aus datei "maxmsgsize" auslesen
    public int getMaxMsgSize(String hbciVersion)
    {
        return Integer.parseInt(getSingleLine("maxmsgsize"));
    }

    // usw. usf.

    public int getBPDVersion(String hbciVersion)
    {
        return Integer.parseInt(getSingleLine("bpdversion_"+hbciVersion));
    }

    // signaturschlssel der bank aus datei "sig_keys" auslesen
    public HBCIKey[] getSigKeys()
    {
        return getXKeys("sig");
    }
    
    // neue bankensignaturschlssel in datei "sig_keys" speichern
    public void setSigKeys(HBCIKey[] keys)
    {
        storeXKeys(keys,"sig");
    }

    public HBCIKey[] getCryptKeys()
    {
        return getXKeys("enc");
    }
    
    public void setCryptKeys(HBCIKey[] keys)
    {
        storeXKeys(keys,"enc");
    }

    public Long getSigId()
    {
        return new Long(getSingleLine("sigid"));
    }
    
    public void setSigId(Long sigid)
    {
        storeSingleLine(sigid.toString(),"sigid");
    }
    
    // gltige nutzerkennungen aus datei "userids" auslesen
    public String[] getUserIds()
    {
        return getMultipleLines("userids");
    }
    
    // kunden-ids fr einen nutzer aus datei "USERID_customerids" auslesen
    public String[] getCustomerIds(String userid)
    {
        return getMultipleLines(userid+"_customerids");
    }
    
    // kontodaten fr einen nutzer aus datei "USERID_accounts" einlesen
    // format je zeile: "KONTONUMMER|KONTOBEZEICHNUNG|NAME_DES_INHABERS|KUNDEN-ID"
    // als Lnderkennung und BLZ fr die Kontoverbindung werden die gleichen daten
    // wie fr den Server benutzt (getCountry()/getBLZ()); als Whrung wird "EUR" benutzt
    public Konto[] getAccounts(String userid)
    {
        String[] accountData=getMultipleLines(userid+"_accounts");
        ArrayList ret=new ArrayList();
        
        for (int i=0;i<accountData.length;i++) {
            StringTokenizer tok=new StringTokenizer(accountData[i],"|");
            Konto acc=new Konto();
            acc.number=tok.nextToken();
            acc.type=tok.nextToken();
            acc.name=tok.nextToken();
            acc.customerid=tok.nextToken();
            acc.country=getCountry();
            acc.blz=getBLZ();
            acc.curr="EUR";
            ret.add(acc);
        }
        
        return (Konto[])ret.toArray(new Konto[0]);
    }

    public int getAccountInfoVersion(String userid)
    {
        return Integer.parseInt(getSingleLine(userid+"_accinfoversion"));
    }

    public String[] getSysIds(String userid)
    {
        return getMultipleLines(userid+"_sysids");
    }
    
    public void addSysId(String userid,String sysid)
    {
        try {
            PrintWriter out=new PrintWriter(new FileWriter(directory+File.separator+userid+"_sysids",true));
            out.println(sysid);
            out.close();
            
            storeMultipleLines(new String[0],userid+"_sigids_"+sysid);
        } catch (Exception e) {
            throw new HBCI_Exception(e.getMessage());
        }
    }
    
    // ffentliche schlssel eines nutzers aus dateien "USERID_sig_keys" und
    // "USERID_enc_keys" einlesen
    public HBCIKey[] getUserKeys(String userid)
    {
        HBCIKey[] sig=getXKeys(userid+"_sig");
        HBCIKey[] enc=getXKeys(userid+"_enc");
        return (sig!=null)?(new HBCIKey[] {sig[0],enc[0]}):null;
    }
    
    public String getUserPIN(String userid)
    {
        return getSingleLine(userid+"_pt_pin");
    }
    
    public void setUserPIN(String userid,String pin)
    {
        storeSingleLine(pin,userid+"_pt_pin");
    }
    
    public String[] getUserTANList(String userid)
    {
        return getMultipleLines(userid+"_pt_tans");
    }
    
    public void setUserTANList(String userid,String[] tans)
    {
        storeMultipleLines(tans,userid+"_pt_tans");
    }
    
    public void removeUserTAN(String userid,String tan)
    {
        String[] tans=getMultipleLines(userid+"_pt_tans");
        List     tanlist=new ArrayList(Arrays.asList(tans));
        
        tanlist.remove(tan+":1");
        tanlist.add(tan+":0");
        
        storeMultipleLines((String[])tanlist.toArray(new String[0]),
                userid+"_pt_tans");
    }

    // signierschlssel eines nutzers in "USERID_sig_keys" speichern
    public void storeUserSigKey(String userid,HBCIKey key)
    {
        storeXKeys((key!=null)?(new HBCIKey[] {key}):null,userid+"_sig");
    }

    public void storeUserEncKey(String userid,HBCIKey key)
    {
        storeXKeys((key!=null)?(new HBCIKey[] {key}):null,userid+"_enc");
    }
    
    // schon eingereichte sigids zurckmelden
    public long[] getSigIds(String userid,String sysid)
    {
        String[] sigids_s=getMultipleLines(userid+"_sigids_"+sysid);
        int      len=sigids_s.length;
        long[]   sigids_l=new long[len];
        
        for (int i=0;i<len;i++) {
            sigids_l[i]=Long.parseLong(sigids_s[i]);
        }
        
        return sigids_l;
    }
   
    // liste der schon eingereichten sigids resetten
    public void clearSigIds(String userid,String sysid)
    {
        storeMultipleLines(new String[0],userid+"_sigids_"+sysid);
    }
    
    // signatur-ids zur liste der schon eingereichten ids hinzufgen
    public void addSigId(String userid,String sysid,long sigid)
    {
        try {
            PrintWriter out=new PrintWriter(new FileWriter(directory+File.separator+userid+"_sigids_"+sysid,true));
            out.println(sigid);
            out.close();
        } catch (Exception e) {
            throw new HBCI_Exception(e.getMessage());
        }
    }
    
    public synchronized void addToStatusProt(String userid,StatusProtEntry entry)
    {
        try {
            PrintWriter out=new PrintWriter(new FileWriter(directory+File.separator+userid+"_statusprot",true));
            
            // TODO auch parameter ins status-protokoll schreiben
            out.println(
                    new SimpleDateFormat("yyyyMMddHHmmss").format(entry.timestamp)+"|"+
                    entry.dialogid+"|"+
                    entry.msgnum+"|"+
                    ((entry.segref!=null)?entry.segref:"<>")+"|"+
                    entry.retval.code+"|"+
                    ((entry.retval.deref!=null)?entry.retval.deref:"<>")+"|"+
                    entry.retval.text
                    );
            out.close();
        } catch (Exception e) {
            throw new HBCI_Exception(e.getMessage());
        }
    }
    
    public StatusProtEntry[] getStatusProt(String userid,Date start,Date end)
    {
        try {
            ArrayList ret=new ArrayList();
            String[]  statusdata=getMultipleLines(userid+"_statusprot");
            
            for (int i=0;i<statusdata.length;i++) {
                String          line=statusdata[i];
                StringTokenizer tok=new StringTokenizer(line,"|");
                
                String timestamp_s=tok.nextToken();
                Date   timestamp=new SimpleDateFormat("yyyyMMddHHmmss").parse(timestamp_s);
                Date   date=new SimpleDateFormat("yyyyMMdd").parse(timestamp_s.substring(0,8));
                
                if ((start==null || date.compareTo(start)>=0) &&
                    (end==null   || date.compareTo(end)<=0)) {
                    
                    StatusProtEntry entry=new StatusProtEntry();
                    entry.timestamp=timestamp;
                    entry.dialogid=tok.nextToken();
                    entry.msgnum=tok.nextToken();
                    entry.segref=tok.nextToken();
                    if (entry.segref.equals("<>")) {
                        entry.segref=null;
                    }

                    String  code=tok.nextToken();
                    String  deref=tok.nextToken();
                    String  text=tok.nextToken();
                    // TODO param auch zurckgeben
                    String[] params=null;
                    
                    if (deref.equals("<>")) {
                        deref=null;
                    }

                    entry.retval=new HBCIRetVal(
                            entry.segref,deref,null,
                            code,text,params);
                    
                    ret.add(entry);
                }
            }
            
            return (StatusProtEntry[])ret.toArray(new StatusProtEntry[0]);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
