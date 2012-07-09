
/*  $Id: ServerData.java,v 1.20 2005/06/10 18:03:03 kleiner Exp $

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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.server.datastore.DataStore;
import org.kapott.hbci.server.listener.ConnectionListener;
import org.kapott.hbci.structures.Konto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ServerData 
{
    private static ServerData _instance;
    
    private DataStore  dataStore;
    private Hashtable  msggens;
    private Random     random;
    
    private ServerCallback callback;
    
    private List      listeners;
    private boolean   anonAllowed;
    private int       loglevel;
    private Hashtable bpds;     // "210" --> BPD
    private Hashtable rdhKeys;     // "S_1_1_PRIV" --> hbcikey
    private Long      sigid;
    private Hashtable userdata; // "userid" --> hashtable
    
    public static synchronized ServerData getInstance()
    {
        if (_instance==null)
            _instance=new ServerData();
        return _instance;
    }
    
    private ServerData()
    {
        this.random=new Random();
    }
    
    public void init(DataStore _dataStore)
    {
        this.dataStore=_dataStore;
        
        initListeners();
        reInitializeServerData();
    }
    
    public void reInitializeServerData()
    {
        this.msggens=new Hashtable();
        initBasicData();
        initKeyData();
        initUserData();
    }
    
    public void reInitializeUserData(String userid)
    {
        synchronized (userdata) {
            Hashtable entry=(Hashtable)userdata.get(userid);
            synchronized (entry) {
                entry.clear();
            }
        }
    }
    
    private void initListeners()
    {
        listeners=new ArrayList();
        
        String[] listenerNames=dataStore.getListeners();
        for (int i=0;i<listenerNames.length;i++) {
            String listenerName=listenerNames[i];
            HBCIUtils.log("initializing connection listener '"+listenerName+"'",
                    HBCIUtils.LOG_INFO);
            
            try {
                Class cl=Class.forName("org.kapott.hbci.server.listener.Listener"+listenerName);
                Constructor cons=cl.getConstructor(null);
                final ConnectionListener listener=(ConnectionListener)cons.newInstance(null);
                listeners.add(listener);
            } catch (Exception e) {
                throw new HBCI_Exception(e);
            }
        }
    }
    
    private void initBasicData()
    {
        loglevel=dataStore.getLogLevel();
        anonAllowed=dataStore.isAnonAllowed();
        
        bpds=new Hashtable();
        String[] suppversions=dataStore.getSuppHBCIVersions();
        
        // eventually add "plus" to list of supported hbci versions
        List versionList=new ArrayList(Arrays.asList(suppversions));
        List listenerList=new ArrayList(Arrays.asList(dataStore.getListeners()));
        if (versionList.contains("220") && listenerList.contains("PinTan")) {
            versionList.add("plus");
            suppversions=(String[])versionList.toArray(new String[0]);
        }
        
        for (int vi=0;vi<suppversions.length;vi++) {
            String hversion=suppversions[vi];
            HBCIUtils.log("initializing BPD for version "+hversion,HBCIUtils.LOG_DEBUG);
            
            Properties bpd=new Properties();
            // BPA
            bpd.setProperty("BPA.version",Integer.toString(dataStore.getBPDVersion(hversion)));
            bpd.setProperty("BPA.KIK.country",dataStore.getCountry());
            bpd.setProperty("BPA.KIK.blz",dataStore.getBLZ());
            bpd.setProperty("BPA.kiname",dataStore.getKIName());
            bpd.setProperty("BPA.numgva",Integer.toString(dataStore.getNumOfGVsPerMsg(hversion)));
            bpd.setProperty("BPA.maxmsgsize",Integer.toString(dataStore.getMaxMsgSize(hversion)));
            
            // supported languages
            String[] supplangs=dataStore.getSuppLangs(hversion);
            int len=supplangs.length;
            for (int i=0;i<len;i++) {
                bpd.setProperty(
                        HBCIUtilsInternal.withCounter("BPA.SuppLangs.lang",i),
                        HBCISpec.mapISO2HBCILang(supplangs[i].toUpperCase()));
            }
            
            // supported hbci versions
            len=suppversions.length;
            for (int i=0;i<len;i++) {
                if (!suppversions[i].equals("plus")) {
                    bpd.setProperty(HBCIUtilsInternal.withCounter("BPA.SuppVersions.version",i),suppversions[i]);
                }
            }
            
            // comm-data (basic)
            String header="CommListRes";
            bpd.setProperty(header+".KIK.country",dataStore.getCountry());
            bpd.setProperty(header+".KIK.blz",dataStore.getBLZ());
            bpd.setProperty(header+".deflang",HBCISpec.mapISO2HBCILang(dataStore.getDefaultLang(hversion)));
            
            // comm-data (access data)
            String[][] commData=dataStore.getCommData(hversion);
            String currentHeader;
            for (int i=0;i<commData.length;i++) {
                currentHeader=HBCIUtilsInternal.withCounter(header+".CommParam",i);
                String[] entry=commData[i];
                
                bpd.setProperty(currentHeader+".dienst",entry[0]);
                bpd.setProperty(currentHeader+".addr",entry[1]);
                if (entry[2]!=null && entry[2].length()!=0){ 
                    bpd.setProperty(currentHeader+".addr2",entry[2]);
                }
                if (entry[3]!=null && entry[3].length()!=0) { 
                    bpd.setProperty(currentHeader+".filter",entry[3]);
                    bpd.setProperty(currentHeader+".filterversion",entry[4]);
                }
            }
            
            // security methods
            bpd.setProperty("SecMethod.mixing","N");
            String[][] suppsecs=dataStore.getSuppSecMethods(hversion);
            for (int i=0;i<suppsecs.length;i++) {
                header=HBCIUtilsInternal.withCounter("SecMethod.SuppSecMethods",i);
                bpd.setProperty(header+".method",suppsecs[i][0]);
                bpd.setProperty(header+".version",suppsecs[i][1]);
            }
            
            // compression methods
            String[][] suppcomps=dataStore.getSuppCompMethods(hversion);
            if (suppcomps!=null) {
                for (int i=0;i<suppcomps.length;i++) {
                    header=HBCIUtilsInternal.withCounter("CompMethod.SuppCompMethods",i);
                    bpd.setProperty(header+".func",suppcomps[i][0]);
                    bpd.setProperty(header+".version",suppcomps[i][1]);
                }
            }
            
            // store GV-params in BPD
            String[]  gvnames=dataStore.getSupportedGVs(hversion);
            int       counter=0;
            
            for (int i=0;i<gvnames.length;i++) {
                String gvname=gvnames[i];
                HBCIUtils.log("storing BPD data for job "+gvname,HBCIUtils.LOG_DEBUG);
                
                // alle supported versions dieses gv holen
                int[] versions=dataStore.getGVVersions(gvname,hversion);
                for (int j=0;j<versions.length;j++) {
                    int version=versions[j];
                    String segheader=HBCIUtilsInternal.withCounter("Params",counter++)+"."+gvname+"Par"+Integer.toString(version);
                    
                    // basic params fr gvname+version setzen
                    bpd.setProperty(segheader+".maxnum",Integer.toString(dataStore.getGVMaxNum(gvname,version)));
                    bpd.setProperty(segheader+".minsigs",Integer.toString(dataStore.getGVMinSigs(gvname,version)));
                    
                    // parameter fr diesen gv holen
                    String paramheader=segheader+".Par"+gvname;
                    Properties params=dataStore.getGVParams(gvname,version);
                    if (params!=null) {
                        for (Enumeration e=params.keys();e.hasMoreElements();) {
                            // gv-parameter in bpd speichern
                            String parname=(String)e.nextElement();
                            String parvalue=params.getProperty(parname);
                            bpd.setProperty(paramheader+"."+parname,parvalue);
                        }
                    }
                }
            }
            
            if (hversion.equals("plus")) {
                // informationen hinzufgen, welche jobs via pin/tan untersttzt
                // werden und welche eine tan bentigen
                HBCIUtils.log("adding BPD data for PinTan support",HBCIUtils.LOG_DEBUG);
                
                String segheader=HBCIUtilsInternal.withCounter("Params",counter++)+".PinTanPar1";
                
                // basic params setzen
                bpd.setProperty(segheader+".maxnum","1");
                bpd.setProperty(segheader+".minsigs","1");
                
                // alle jobs, die via pintan untersttzt werden, eintragen
                Properties pintanJobs=dataStore.getPinTanGVs();
                int        jobcounter=0;
                for (Enumeration e=pintanJobs.propertyNames();e.hasMoreElements();) {
                    String jobname=(String)e.nextElement();
                    String needsTan=pintanJobs.getProperty(jobname);
                    
                    String jobheader=HBCIUtilsInternal.withCounter(segheader+".ParPinTan.PinTanGV",jobcounter++);
                    bpd.setProperty(jobheader+".segcode",getSegCodeForJob(jobname));
                    bpd.setProperty(jobheader+".needtan",needsTan);
                }
            }
            
            bpds.put(hversion,bpd);
        }
    }
    
    // TODO: die methode ist scheisse
    private String getSegCodeForJob(String jobname)
    {
        String      ret=null;
        
        String      xmlpath=HBCIUtils.getParam("kernel.kernel.xmlpath");
        InputStream syntaxStream=null;
        
        if (xmlpath==null) {
            xmlpath="";
        }
        
        // inputstream fr xml-spec erzeugen
        ClassLoader cl=HBCIUtils.class.getClassLoader();
        String      filename=xmlpath+"hbci-plus.xml";
        syntaxStream=cl.getResourceAsStream(filename);
        if (syntaxStream==null) 
            throw new HBCI_Exception("could not find syntax specification for hbciversion plus");
        
        MsgGen   msggen=new MsgGen(syntaxStream);
        Document syntax=msggen.getSyntax();
        
        NodeList segdefs=syntax.getElementsByTagName("SEGdef");
        int      size=segdefs.getLength();
        for (int i=0;i<size;i++) {
            Node segdef=segdefs.item(i);
            if (segdef.getNodeType()==Node.ELEMENT_NODE) {
                String id=((Element)segdef).getAttribute("id");
                if (id.startsWith(jobname)) {
                    char digit=id.charAt(jobname.length());
                    if (digit>='0' && digit<='9') {
                        NodeList valueChilds=((Element)segdef).getElementsByTagName("value");
                        int      size2=valueChilds.getLength();
                        for (int j=0;j<size2;j++) {
                            Node valueChild=valueChilds.item(j);
                            if (valueChild.getNodeType()==Node.ELEMENT_NODE) {
                                if (((Element)valueChild).getAttribute("path").equals("SegHead.code")) {
                                    ret=((Element)valueChild).getFirstChild().getNodeValue();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ret;
    }
    
    private void initKeyData()
    {
        HBCIUtils.log("initializing server keys",HBCIUtils.LOG_DEBUG);
        
        // rdh-schlssel aus keystore einlesen
        rdhKeys=new Hashtable();
        for (int keytype=0;keytype<2;keytype++) {     // S und V keys
            HBCIKey[] loadedKeys=(keytype==0?
                dataStore.getSigKeys():
                    dataStore.getCryptKeys());
            
            // wenn schlssel schon vorhanden
            if (loadedKeys!=null) {
                HBCIUtils.log("server "+(keytype==0?"signature":"encryption")+" key loaded",
                        HBCIUtils.LOG_DEBUG);
                
                // schlssel in keys-hashtable ablegen
                for (int i=0;i<2;i++) {
                    HBCIKey key=loadedKeys[i];
                    rdhKeys.put((keytype==0?"S":"V")+"_"+
                            key.num+"_"+
                            key.version+"_"+
                            (i==0?"PUB":"PRIV"),key);
                }
            } else {
                // schlssel noch nicht vorhanden
                HBCIUtils.log("server "+(keytype==0?"signature":"encryption")+" keys not found - generating new",
                        HBCIUtils.LOG_INFO);
                
                try {
                    // neue schlssel erzeugen
                    KeyPairGenerator keygen=KeyPairGenerator.getInstance("RSA");
                    keygen.initialize(768);
                    KeyPair keypair=keygen.generateKeyPair();
                    
                    HBCIKey[] newKeys=new HBCIKey[2];
                    
                    // hbcikey-objekte mit neuen schlsseldaten erzeugen
                    newKeys[0]=new HBCIKey(getCountry(),getBLZ(),getBLZ(),"1","1",keypair.getPublic());
                    newKeys[1]=new HBCIKey(getCountry(),getBLZ(),getBLZ(),"1","1",keypair.getPrivate());
                    
                    // neue schlssel in datastore ablegen
                    if (keytype==0)
                        dataStore.setSigKeys(newKeys);
                    else
                        dataStore.setCryptKeys(newKeys);
                    
                    // neue schlssel in keys-hashtable ablegen
                    rdhKeys.put((keytype==0?"S":"V")+"_1_1_PUB",newKeys[0]);
                    rdhKeys.put((keytype==0?"S":"V")+"_1_1_PRIV",newKeys[1]);
                } catch (Exception e) {
                    throw new HBCI_Exception("error while generating new server keys",e);
                }
            }
        }
        
        // sigid initialisieren
        sigid=dataStore.getSigId();
    }
    
    private void initUserData()
    {
        userdata=new Hashtable();
        
        // get all userids from datastore
        String[] userids=dataStore.getUserIds();
        for (int i=0;i<userids.length;i++) {
            String userid=userids[i];
            HBCIUtils.log("initializing userdata for userid "+userid,HBCIUtils.LOG_DEBUG);
            
            // creating entry for this userid
            Hashtable entry=new Hashtable();
            userdata.put(userid,entry);
        }
    }
    
    public Hashtable getUserData()
    {
        return userdata;
    }
    
    private Hashtable getUserEntry(String userid)
    {
        Hashtable entry;
        synchronized (userdata) {
            entry=(Hashtable)userdata.get(userid);
        }
        
        if (entry.size()==0) {
            HBCIUtils.log("loading data for user "+userid,HBCIUtils.LOG_DEBUG);
            
            // initializing entry for userid
            entry.put("customerids",dataStore.getCustomerIds(userid));
            
            // alle gltigen system-ids holen
            String[] sysids=dataStore.getSysIds(userid);
            
            // fr jede system-id die schon eingereichten sig-ids holen und speichern
            Hashtable sigids=new Hashtable();
            for (int j=0;j<sysids.length;j++) {
                // sigids are returned as long[] from the datastore
                // and internally stored as arraylist
                long[]    ids_a=dataStore.getSigIds(userid,sysids[j]);
                int       len=ids_a.length;
                ArrayList ids=new ArrayList();
                for (int i=0;i<len;i++) {
                    ids.add(new Long(ids_a[i]));
                }
                sigids.put(sysids[j],ids);
            }
            entry.put("sigids",sigids);
            
            // Nutzerschlssel holen
            // TODO: das besser nach passportTypes differenzieren
            HBCIKey[] userkeys=dataStore.getUserKeys(userid);
            if (userkeys!=null) {
                entry.put("key_sig",userkeys[0]);
                entry.put("key_enc",userkeys[1]);
            }
            
            // PIN holen
            String pin=dataStore.getUserPIN(userid);
            if (pin!=null) {
                entry.put("pt_pin",pin);
            }
            
            // TAN-Liste holen
            String[]  tans=dataStore.getUserTANList(userid);
            Hashtable usertans=new Hashtable();
            for (int i=0;i<tans.length;i++) {
                String tan=tans[i];
                int    tanlen=tan.length();
                
                usertans.put(tan.substring(0,tanlen-2),tan.substring(tanlen-1));
            }
            entry.put("pt_tans",usertans);
            
            // UPD zusammenstellen
            Properties upd=new Properties();
            entry.put("upd",upd);
            upd.setProperty("UPA.userid",userid);
            upd.setProperty("UPA.version",Integer.toString(dataStore.getAccountInfoVersion(userid)));
            upd.setProperty("UPA.usage","1");
            
            // Kontoinformationen sammeln
            Konto[] accounts=dataStore.getAccounts(userid);
            entry.put("accounts",accounts);
            for (int j=0;j<accounts.length;j++) {
                Konto acc=accounts[j];
                String header=HBCIUtilsInternal.withCounter("KInfo",j);
                
                upd.setProperty(header+".KTV.KIK.country",acc.country);
                upd.setProperty(header+".KTV.KIK.blz",acc.blz);
                upd.setProperty(header+".KTV.number",acc.number);
                upd.setProperty(header+".customerid",acc.customerid);
                upd.setProperty(header+".cur",acc.curr);
                upd.setProperty(header+".name1",acc.name);
                if (acc.name2!=null)
                    upd.setProperty(header+".name2",acc.name2);
                upd.setProperty(header+".konto",acc.type);
            }
        }
        
        entry.put("timestamp",new Date());
        return entry;
    }
    
    public List getListeners()
    {
        return listeners;
    }
    
    // get country code for server
    public String getCountry()
    {
        return ((Properties)bpds.elements().nextElement()).getProperty("BPA.KIK.country");
    }

    // get BLZ for server
    public String getBLZ()
    {
        return ((Properties)bpds.elements().nextElement()).getProperty("BPA.KIK.blz");
    }
    
    // server-adresse aus BPD fr typ <type> ermitteln
    public String getHost(int type)
    {
        String ret=null;
        
        // irgendeine BPD benutzen
        String hbciversion=(String)bpds.keys().nextElement();
        Properties bpd=(Properties)bpds.get(hbciversion);
        
        String header="CommListRes";
        
        // alle darin gespeicherten comm-daten durchlaufen
        for (int i=0;;i++) {
            String dataHeader=HBCIUtilsInternal.withCounter(header+".CommParam",i);
            
            // serveradresse extrahieren
            ret=bpd.getProperty(dataHeader+".addr");
            
            if (ret==null)
                break;
            
            // wenn comm-type passt, dann fertig
            if (Integer.parseInt(bpd.getProperty(dataHeader+".dienst"))==type)
                break;
        }
        
        return ret;
    }
    
    public boolean isAnonAllowed()
    {
        return anonAllowed;
    }
    
    public int getLogLevel()
    {
        return loglevel;
    }
    
    public Properties getBPD(String version)
    {
        return (Properties)bpds.get(version);
    }
    
    public long nextRandom()
    {
        return random.nextLong();
    }
    
    // fr msggen-cache
    public MsgGen getMsgGen(String hbciversion)
    {
        synchronized (msggens) {
            return (MsgGen)msggens.get(hbciversion);
        }
    }
    
    // fr msggen-cache
    public synchronized void storeMsgGen(String hbciversion,MsgGen gen)
    {
        synchronized (msggens) {
            msggens.put(hbciversion,gen);
        }
    }
    
    private HBCIKey getInstXKey(String type,String visibility)
    {
        HBCIKey ret=null;
        
        int num=0;
        int version=0;
        
        // alle gespeicherten server-schlssel durchlaufen
        for (Enumeration e=rdhKeys.keys();e.hasMoreElements();) {
            String name=(String)e.nextElement();
            // wenn der aktuelle schlssel von der gesuchten art ist...
            if (name.startsWith(type+"_") &&
                    name.endsWith("_"+visibility)) {
                
                HBCIKey key=(HBCIKey)rdhKeys.get(name);
                // wenn schlsselnummer/-version hher als zuletzt gefundener, dann diesen nehmen
                if (Integer.parseInt(key.num)>num ||
                        (Integer.parseInt(key.num)==num && Integer.parseInt(key.version)>version)) {
                    ret=key;
                }
            }
        }
        
        return ret;
    }
    
    public HBCIKey getInstPublicRDHSigKey()
    {
        return getInstXKey("S","PUB");
    }
    
    public HBCIKey getInstPrivateRDHSigKey()
    {
        return getInstXKey("S","PRIV");
    }
    
    public HBCIKey getInstPublicRDHEncKey()
    {
        return getInstXKey("V","PUB");
    }

    public HBCIKey getInstPrivateRDHEncKey()
    {
        return getInstXKey("V","PRIV");
    }
    
    public Long getSigId()
    {
        synchronized (sigid) {
            return sigid;
        }
    }
    
    public synchronized void setSigId(Long sigid)
    {
        synchronized (sigid) {
            if (sigid.longValue()>this.sigid.longValue()) {
                this.sigid=sigid;
                dataStore.setSigId(sigid);
            }
        }
    }
    
    public HBCIKey getUserRDHSigKey(String userid) 
    {
        return (HBCIKey)getUserEntry(userid).get("key_sig");
    }

    public HBCIKey getUserRDHEncKey(String userid) 
    {
        return (HBCIKey)getUserEntry(userid).get("key_enc");
    }
    
    public String getUserPIN(String userid)
    {
        return (String)getUserEntry(userid).get("pt_pin");
    }
    
    public Hashtable getUserTANList(String userid)
    {
        return (Hashtable)getUserEntry(userid).get("pt_tans");
    }
    
    public void removeUserTAN(String userid,String tan)
    {
        ((Hashtable)getUserEntry(userid).get("pt_tans")).put(tan,"0");
        dataStore.removeUserTAN(userid,tan);
    }
    
    public synchronized void setUserRDHSigKey(String userid,HBCIKey key)
    {
        Hashtable entry=getUserEntry(userid);
        
        if (key!=null)
            entry.put("key_sig",key);
        else
            entry.remove("key_sig");
        
        dataStore.storeUserSigKey(userid,key);
        
        HBCIUtils.log("removing passport data from cache because sig keys changed",HBCIUtils.LOG_DEBUG);
        entry.remove("passports");
    }

    public synchronized void setUserRDHEncKey(String userid,HBCIKey key)
    {
        Hashtable entry=getUserEntry(userid);
        
        if (key!=null)
            entry.put("key_enc",key);
        else
            entry.remove("key_enc");
        
        dataStore.storeUserEncKey(userid,key);
        
        HBCIUtils.log("removing passport data from cache because enc keys changed",HBCIUtils.LOG_DEBUG);
        entry.remove("passports");
    }
    
    public void addSysId(String userid,String sysid)
    {
        Hashtable entry=getUserEntry(userid);
        ((Hashtable)entry.get("sigids")).put(sysid,new ArrayList());
        dataStore.addSysId(userid,sysid);
        
        HBCIUtils.log("removing passport data from cache because there is a new valid sysid ",HBCIUtils.LOG_DEBUG);
        entry.remove("passports");
    }
    
    public String[] getSysIds(String userid)
    {
        ArrayList ret=new ArrayList();
        Hashtable sigids=(Hashtable)getUserEntry(userid).get("sigids");
        
        for (Enumeration e=sigids.keys();e.hasMoreElements();) {
            ret.add(e.nextElement());
        }
        
        return (String[])ret.toArray(new String[0]);
    }
    
    public String[] getCustomerIds(String userid)
    {
        return (String[])getUserEntry(userid).get("customerids"); 
    }
    
    public Properties getUPD(String userid)
    {
        Properties ret=new Properties();

        // anonymous user
        if (userid==null) {
            ret.setProperty("UPA.userid","9999999999");
            ret.setProperty("UPA.usage","1");
            ret.setProperty("UPA.version","1");
        } else {
            // lokal gespeicherte UPD zurckgeben 
            ret=(Properties)getUserEntry(userid).get("upd");
        }

        return ret;
    }
    
    public Konto[] getAccounts(String userid,String customerid)
    {
        Konto[] accounts=(Konto[])getUserEntry(userid).get("accounts");
        ArrayList ret=new ArrayList();
        for (int i=0;i<accounts.length;i++) {
            Konto acc=accounts[i];
            if (acc.customerid.equals(customerid))
                ret.add(acc);
        }
        return (Konto[])ret.toArray(new Konto[0]);
    }
    
    public Konto[] getAllAccounts()
    {
        ArrayList result=new ArrayList();
        for (Enumeration e=userdata.keys();e.hasMoreElements();) {
            String userid=(String)e.nextElement();
            String[] customerids=getCustomerIds(userid);
            for (int i=0;i<customerids.length;i++) {
                result.addAll(Arrays.asList(getAccounts(userid,customerids[i])));
            }
        }
        return (Konto[])result.toArray(new Konto[0]);
    }
    
    public HBCIPassportInternal getPassport(Dialog dialog)
    {
        String userid=dialog.getUserId();
        String passportType=dialog.getPassportType();
        
        Hashtable passports=(Hashtable)getUserEntry(userid).get("passports");
        if (passports==null) {
            passports=new Hashtable();
            getUserEntry(userid).put("passports",passports);
        }
        
        String               passportID=passportType+"_"+dialog.getSysId();
        HBCIPassportInternal ret=(HBCIPassportInternal)passports.get(passportID);
        
        // wenn es noch kein lokal gespeichertes passport gibt
        if (ret==null) {
            try {
                // neues passport fr diese userid erzeugen
                Class       cl=Class.forName("org.kapott.hbci.server.passport.LocalPassport"+passportType);
                Constructor cons=cl.getConstructor(new Class[] {Dialog.class});
                ret=(HBCIPassportInternal)cons.newInstance(new Object[] {dialog});
            } catch (Exception e) {
                throw new HBCI_Exception(e);
            }
            passports.put(passportID,ret);
        }
        
        return ret;
    }
    
    public boolean existsUserId(String userid)
    {
        return userdata.get(userid)!=null;
    }
    
    public boolean existUserSigKeys(Dialog dialog)
    {
        // *** TODO changed this, does it work???
        // return getUserSigKey(userid)!=null;
        return getPassport(dialog).hasInstSigKey();
    }
    
    public boolean existsSigId(String userid,String sysid,long sigId)
    {
        boolean exists;
        
        if (sysid.equals("0")) {
            exists=false;
        } else {
            exists=((ArrayList)((Hashtable)getUserEntry(userid).get("sigids")).get(sysid)).contains(new Long(sigId));
        }
        
        return exists;
    }
    
    public long getLastSigId(String userid,String sysid)
    {
        long   ret;
        
        if (sysid.equals("0")) {
            ret=0;
        } else {
            Long[] ids=(Long[])((ArrayList)((Hashtable)getUserEntry(userid).get("sigids")).get(sysid)).toArray(new Long[0]);
            
            if (ids.length==0) {
                ret=0;
            } else {
                Arrays.sort(ids);
                ret=ids[ids.length-1].longValue();
            }
        }
        
        return ret;
    }
    
    public void clearSigIds(String userid,String sysid)
    {
        if (!sysid.equals("0")) {
            ((ArrayList)((Hashtable)getUserEntry(userid).get("sigids")).get(sysid)).clear();
            dataStore.clearSigIds(userid,sysid);
        }
    }
    
    public void addSigId(String userid,String sysid,long sigId)
    {
        if (!sysid.equals("0")) {
            ((ArrayList)((Hashtable)getUserEntry(userid).get("sigids")).get(sysid)).add(new Long(sigId));
            dataStore.addSigId(userid,sysid,sigId);
        }
    }
    
    public void setCallbackObject(ServerCallback callback)
    {
        this.callback=callback;
    }
    
    public void handleGVCallback(JobContext context)
    {
        callback.handleGV(context);
    }
    
    public void log(String msg,int level,Date date,StackTraceElement trace)
    {
        callback.log(msg,level,date,trace);
    }
    
    public void addToStatusProt(String userid,StatusProtEntry entry)
    {
        if (userid!=null) {
            dataStore.addToStatusProt(userid,entry);
        }
    }
}
