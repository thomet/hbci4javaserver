
/*  $Id: TestServer.java,v 1.20 2005/06/10 18:03:03 kleiner Exp $

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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.kapott.demo.hbci.server.backend.MyBackend;
import org.kapott.hbci.datatypes.factory.SyntaxDEFactory;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.protocol.factory.DEFactory;
import org.kapott.hbci.protocol.factory.DEGFactory;
import org.kapott.hbci.protocol.factory.MSGFactory;
import org.kapott.hbci.protocol.factory.MultipleDEGsFactory;
import org.kapott.hbci.protocol.factory.MultipleDEsFactory;
import org.kapott.hbci.protocol.factory.MultipleSEGsFactory;
import org.kapott.hbci.protocol.factory.MultipleSFsFactory;
import org.kapott.hbci.protocol.factory.SEGFactory;
import org.kapott.hbci.protocol.factory.SFFactory;
import org.kapott.hbci.security.factory.CryptFactory;
import org.kapott.hbci.security.factory.SigFactory;
import org.kapott.hbci.server.DialogMgr;
import org.kapott.hbci.server.HBCIServer;
import org.kapott.hbci.server.JobContext;
import org.kapott.hbci.server.ServerCallback;
import org.kapott.hbci.server.ServerData;
import org.kapott.hbci.server.StatusProtEntry;
import org.kapott.hbci.server.datastore.DataStore;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

public class TestServer 
    implements ServerCallback
{
    public DataStore    dataStore;
    private MyBackend   backend;
    private HBCIServer  server;
    private ServerAdmin serverAdmin;
    
    private static final boolean INVERSE=true;
    private static final boolean TERMINATED=true;
    
    public static void main(String[] args) 
        throws Exception
    {
        // erstes argument ist verzeichnisname fr server-daten
        boolean noConsole=args.length>=2 && args[1].equals("noconsole");
        new TestServer().start(args[0],!noConsole);
    }
    
    public void start(String directory,boolean useConsole)
    {
        // hbci-server mit neuem datastore initialisieren
        dataStore=new MyDataStore(directory);
        backend=new MyBackend(directory+"-backend");
        server=new HBCIServer(dataStore,this);
        initRMI();
        
        // zustzlichen thread fr "admin-console" starten
        if (useConsole) {
            new Thread() { public void run() {
                System.out.println("starting admin console");
                String command=null;
                
                while (true) {
                    // "kommando" einlesen
                    try {
                        command=new BufferedReader(new InputStreamReader(System.in)).readLine();
                    } catch (Exception e) {
                        System.out.println("problem with STDIN");
                        e.printStackTrace();
                        return;
                    }
                    
                    try {
                        if (command!=null) {
                            if (command.equals("halt")) { // server anhalten
                                server.stop();
                                closeRMI();
                                break;
                            } else if (command.equals("reload")) {  // datastore neu einlesen
                                server.reInitializeServerData();
                            /* *** } else if (command.startsWith("iniletter")) { // iniletter fr benutzer ausgeben
                                showINILetter(command.substring("iniletter".length())); */
                            } else if (command.equals("factorystats")) { // iniletter fr benutzer ausgeben
                                showFactoryStats();
                            } else if (command.equals("cacheinfo")) {
                                showCacheInfo();
                            } else if (command.length()!=0) {
                                System.out.println();
                                System.out.println("unknown command!");
                                System.out.println("halt     iniletter <user>");
                                System.out.println("reload   factorystats");
                                System.out.println("cacheinfo");
                                System.out.println();
                            }
                        }  
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }}.start();
        }

        // server starten
        server.start();
    }
    
    // RMI server und objekte initialisieren
    private void initRMI()
    {
        try {
            Registry reg=null;

            try {
                reg=LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                HBCIUtils.log("starting new registry",HBCIUtils.LOG_DEBUG);
            } catch (Exception e) {
                reg=LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
                HBCIUtils.log("using already running registry",HBCIUtils.LOG_DEBUG);
            }
            
            serverAdmin=new ServerAdminImpl(server,(MyDataStore)dataStore,backend);
            reg.rebind("serverAdmin",serverAdmin);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // RMI objekte wieder aufrumen
    private void closeRMI()
    {
        try {
            Registry reg=LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            reg.unbind("serverAdmin");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // anzeigen eines INI briefes
    /* *** private void showINILetter(String who)
    {
        HBCIPassport passport=server.getPassport(who.trim());
        INILetter iniletter=new INILetter(passport,INILetter.TYPE_INST);
        System.out.println(iniletter.toString());
    } */
    
    private void showFactoryStats()
    {
        System.out.println();
        System.out.println("msg: "+MSGFactory.getInstance());
        System.out.println("multisf: "+MultipleSFsFactory.getInstance());
        System.out.println("sf: "+SFFactory.getInstance());
        System.out.println("multiseg: "+MultipleSEGsFactory.getInstance());
        System.out.println("seg: "+SEGFactory.getInstance());
        System.out.println("multideg: "+MultipleDEGsFactory.getInstance());
        System.out.println("deg: "+DEGFactory.getInstance());
        System.out.println("multide: "+MultipleDEsFactory.getInstance());
        System.out.println("de: "+DEFactory.getInstance());
        System.out.println("sig: "+SigFactory.getInstance());
        System.out.println("crypt: "+CryptFactory.getInstance());
        System.out.println("syntax: "+SyntaxDEFactory.getInstance());
        System.out.println();
    }
    
    public void showCacheInfo()
    {
        System.out.println();
        System.out.println("current dialogids:");
        for (Enumeration e=DialogMgr.getInstance().getDialogs().keys();e.hasMoreElements();) {
            System.out.println(e.nextElement());
        }
        
        System.out.println();
        System.out.println("currently user entries:");
        Hashtable userdata=ServerData.getInstance().getUserData();
        for (Enumeration e=userdata.keys();e.hasMoreElements();) {
            String userid=(String)e.nextElement();
            System.out.println(userid+":"+
                    ((((Hashtable)userdata.get(userid)).size()==0)?"not loaded":"loaded"));
        }
        
        System.out.println();
    }

    // log-ausgaben des servers entgegennehmen
    public void log(String msg,int level,Date date,StackTraceElement trace)
    {
        String[] levels={"NON","ERR","WRN","INF","DBG","DB2"};
        StringBuffer ret=new StringBuffer(128);
        ret.append("<").append(levels[level]).append("> ");
        
        SimpleDateFormat df=new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        ret.append("[").append(df.format(date)).append("] ");
        
        Thread thread=Thread.currentThread();
        ret.append("[").append(thread.getThreadGroup().getName()+"/"+thread.getName()+"] ");
        
        String classname=trace.getClassName();
        String hbciname="org.kapott.hbci.";
        if (classname!=null && classname.startsWith(hbciname))
            ret.append(classname.substring((hbciname).length())).append(": ");
        
        if (msg!=null)
            ret.append(msg);
        
        // ausgabestream fr server-logs erzeugen
        PrintWriter logStream=null;
        try {
            logStream=new PrintWriter(new BufferedWriter(new FileWriter("server_"+HBCIUtils.getParam("connection.id")+".log",true)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logStream.println(ret.toString());
        logStream.close();
    }

    // wird aufgerufen, wenn ein job eingegangen ist
    public void handleGV(JobContext context)
    {
        String jobname=context.getJobName();
        System.out.println("have to handle job "+jobname+
                " for userid/customer "+
                context.getUserId()+"/"+context.getCustomerId());
        
        if (jobname.equals("Saldo"))
            handleSaldoRequest(context);
        else if (jobname.equals("CustomMsg"))
            handleCustomMsg(context);
        else if (jobname.equals("Ueb"))
            handleUeb(context,!TERMINATED);
        else if (jobname.equals("TermUeb"))
            handleUeb(context,TERMINATED);
        else if (jobname.equals("Umb"))
            handleUmb(context);
        else if (jobname.equals("Last"))
            handleUeb(context,INVERSE,"71",!TERMINATED);
        else if (jobname.equals("KUmsZeit"))
            handleKUmsZeit(context);
        else if (jobname.equals("KUmsNew"))
            handleKUmsNew(context);
        else if (jobname.equals("Status"))
            handleStatusProt(context);
        else if (jobname.equals("SammelUeb"))
            handleMultiTransfer(context,!INVERSE);
        else if (jobname.equals("SammelLast"))
            handleMultiTransfer(context,INVERSE);
        else
            throw new HBCI_Exception("'"+jobname+"' not yet supported");
    }
    
    // saldoabfrage handeln
    private void handleSaldoRequest(JobContext context)
    {
        // daten aus saldo-request extrahieren
        boolean allAccounts=context.getJobData("allaccounts").equals("J");
        
        // abgefrages konto berprfen
        Konto acc=context.checkKTV("KTV");
        if (acc==null) {
            return;
        }
        
        // *** maxentries/offset prfen
        
        // kontosammlung fr antwortdaten zusammenstellen
        Konto[] accounts;
        if (!allAccounts)
            accounts=new Konto[] {acc};
        else
            accounts=context.getAllMyAccounts();
        
        int counter=0;
        // alle kundenkonten durchlaufen
        for (int i=0;i<accounts.length;i++) {
            acc=accounts[i];
            System.out.println("adding saldo for account "+acc.toString());
            
            // kontosaldo in antwort einstellen (antwortdaten erzeugen)
            double saldo=backend.getSaldo(acc);
            
            context.setData(counter,"KTV.KIK.country",acc.country);
            context.setData(counter,"KTV.KIK.blz",acc.blz);
            context.setData(counter,"KTV.number",acc.number);
            context.setData(counter,"kontobez",acc.type);
            context.setData(counter,"curr",acc.curr);
            context.setData(counter,"booked.CreditDebit",saldo>=0?"C":"D");
            context.setData(counter,"booked.value",HBCIUtils.value2String(Math.abs(saldo)));
            context.setData(counter,"booked.curr","EUR");
            
            Date now=new Date();
            context.setData(counter,"booked.date",HBCIUtils.date2String(now));
            context.setData(counter,"booked.time",HBCIUtils.time2String(now));
            context.setData(counter,"Timestamp.date",HBCIUtils.date2String(now));
            context.setData(counter,"Timestamp.time",HBCIUtils.time2String(now));
            
            context.addStatus(null,"0020","Daten zurckgemeldet",null);
            counter++;
        }
    }
    
    // eingehende kundenmeldung handeln
    public void handleCustomMsg(JobContext context)
    {
        // bergebene kontoverbindung berprfen
        Konto acc=context.checkKTV("KTV");
        if (acc==null) {
            return;
        }
        
        System.out.println("Nachricht von Kunde "+context.getCustomerId()+" (Nutzerkennung "+context.getUserId()+"):");
        System.out.println("  Konto: "+acc);
        System.out.println("  An: "+context.getJobData("recpt"));
        System.out.println("  Betreff: "+context.getJobData("betreff"));
        System.out.println("  Nachricht: "+context.getJobData("msg"));
        
        backend.storeCustomMsg(acc,
                context.getJobData("recpt"),
                context.getJobData("betreff"),
                context.getJobData("msg"));
        
        context.addStatus(null,"0010","Nachricht entgegengenommen",null);
    }
    
    // umbuchung mit zustzlichem test, ansonsten wie ueberweisung
    private void handleUmb(JobContext context)
    {
        Konto other=context.checkKTV("Other");
        if (other==null) {
            return;
        }
        handleUeb(context,!TERMINATED);
    }
    
    private void handleUeb(JobContext context,boolean terminated)
    {
        handleUeb(context,false,"20",terminated);
    }
    
    // eingehenden berweisungsauftrag handeln
    private void handleUeb(JobContext context,boolean inverse,String myKey,boolean terminated)
    {
        Konto    my=context.checkKTV("My");
        Konto    other=context.extractOtherAccount("Other");
        String   name=context.getJobData("name");
        String   name2=context.getJobData("name2");
        Value    btg=context.extractBTG("BTG");
        String   key=context.getJobData("key");
        String   addkey=context.getJobData("addkey");
        String[] usage=context.extractStringArray("usage.usage");
        Date     date=context.extractDate("date");
        String   id=context.getJobData("id");
        
        if (my==null) {
            return;
        }
        
        if (!terminated && date!=null) {
            context.addStatus("date","9150","Belegung nicht erlaubt",null);
            return;
        }
        
        if (terminated && date==null) {
            context.addStatus("date","9160","Ausfhrungsdatum fehlt",null);
            return;
        }
        // TODO minpretime und maxpretime testen
        
        if (id!=null) {
            context.addStatus("id","9150","Belegung nicht erlaubt",null);
            return;
        }
        
        // *** hoehe von btg ueberpruefen
        if (btg.value<=0) {
            context.addStatus("BTG.value","9215","Inhalt zu klein",null);
            return;
        }
        
        // waehrung von btg ueberprfen
        if (!btg.curr.equals("EUR")) {
            context.addStatus("BTG.curr","9210","Ungltige Whrung",null);
            return;
        }
        
        // *** key auf erlaubte werte berprfen
        // *** usage auf maxanzahl ueberpruefen
        
        other.name=name;
        other.name2=name2;
        
        if (terminated)
            System.out.print("terminated ("+HBCIUtils.date2String(date)+") ");
        System.out.println("request for "+(inverse?"debit note":"transfer"));
        System.out.println("  my account:    "+my);
        System.out.println("  other account: "+other);
        System.out.println("  value: "+btg);
        
        if (!terminated) {
            /* btg wird immer zum my-konto addiert
             bei berweisungen muss dieser wert also negativ sein (weil das
             eigene konto ja belastet wird), bei lastschriften (inverse=true)
             muss der wert positiv bleiben, weil das eigene konto erhht wird */
            if (!inverse)
                btg.value=-btg.value;
            
            backend.addTransfer(my,other,btg,usage,myKey,null);
            
            // hier alle konten bei dieser bank checken
            if ((other=context.extractOtherAccount("Other")).customerid!=null) {
                btg.value=-btg.value;
                backend.addTransfer(other,my,btg,usage,key,addkey);
            }
            
            context.addStatus(null,"0020","Auftrag ausgefhrt",null);
        } else {
            // TODO Auftrag muss in scheduled queue aufgenommen werden
            context.addStatus(null,"0010","Auftrag entgegengenommen",null);
        }
    }
    
    private void handleKUmsNew(JobContext context)
    {
        handleKUms(context,true);
    }

    // abfrage von kontoauszgen behandeln
    private void handleKUmsZeit(JobContext context)
    {
        handleKUms(context,false);
    }
    
    // abfrage von kontoauszgen behandeln
    private void handleKUms(JobContext context,boolean onlyNew)
    {
        Konto my=context.checkKTV("KTV");
        Date from=null;
        Date to=null;

        if (!onlyNew) {
            String st;
            from=(st=context.getJobData("startdate"))!=null?HBCIUtils.string2Date(st):null;
            to=(st=context.getJobData("enddate"))!=null?HBCIUtils.string2Date(st):null;
            if (to!=null) {
                Calendar cal=Calendar.getInstance();
                cal.setTime(to);
                cal.set(Calendar.HOUR,23);
                cal.set(Calendar.MINUTE,59);
                cal.set(Calendar.SECOND,59);
                to=cal.getTime();
            }
        }
        
        String st=context.getJobData("allaccounts");
        boolean allAccounts=(st!=null && st.equals("J"));
        
        if (my==null) {
            return;
        }
        
        // *** canallaccounts prfen
        // *** zeitangaben prfen
        // *** timerange prfen?
        // *** maxentries prfen
        // *** offset prfen
        
        // kontosammlung fr antwortdaten zusammenstellen
        Konto[] accounts;
        if (!allAccounts)
            accounts=new Konto[] {my};
        else
            accounts=context.getAllMyAccounts();
        
        // alle kundenkonten durchlaufen
        int counter=0;
        for (int i=0;i<accounts.length;i++) {
            Konto acc=accounts[i];
            System.out.println("adding statement of account for "+acc.toString());
            
            String data=backend.getStatementOfAccount(acc,from,to,onlyNew);
            
            if (data.length()!=0) {
                // daten in antwort setzen
                context.setData(counter,"booked","B"+data);
                context.addStatus(null,"0020","Daten zurckgemeldet",null);
                counter++;
            } else {
                context.addStatus(null,"3010","Keine Eintrge verfgbar",null);
            }
        }
    }
    
    private void handleStatusProt(JobContext context)
    {
        // TODO maxentries und offset auswerten
        
        Date startdate=context.extractDate("startdate");
        Date enddate=context.extractDate("enddate");
        
        if (startdate!=null && enddate!=null && startdate.compareTo(enddate)>0) {
            context.addStatus("enddate","9210","Endedatum liegt vor Startdatum",null);
        } else {
            System.out.println("returning status protocol for timerange '"+
                    ((startdate!=null)?HBCIUtils.date2String(startdate):"any")+
                    "' to '"+
                    ((enddate!=null)?HBCIUtils.date2String(enddate):"any")+
                    "'");
            
            StatusProtEntry[] entries=((MyDataStore)dataStore).getStatusProt(
                    context.getUserId(),
                    startdate,enddate);
            
            for (int i=0;i<entries.length;i++) {
                StatusProtEntry entry=entries[i];
                context.setData(i,"MsgRef.dialogid",entry.dialogid);
                context.setData(i,"MsgRef.msgnum",entry.msgnum);
                if (entry.segref!=null) {
                    context.setData(i,"segref",entry.segref);
                }
                context.setData(i,"date",HBCIUtils.date2String(entry.timestamp));
                context.setData(i,"time",HBCIUtils.time2String(entry.timestamp));
                context.setData(i,"RetVal.code",entry.retval.code);
                if (entry.retval.deref!=null) {
                    context.setData(i,"RetVal.ref",entry.retval.deref);
                }
                context.setData(i,"RetVal.text",entry.retval.text);
                // TODO param auch zurckgeben
            }
            
            if (entries.length!=0) {
                context.addStatus(null,"0020","Daten zurckgemeldet",null);
            } else {
                context.addStatus(null,"3010","Keine Eintrge verfgbar",null);    
            }
        }
    }
    
    private void handleMultiTransfer(JobContext context,boolean inverse)
    {
        // my-KTV checken, wenn vorhanden
        
        System.out.println("processing mass "+(inverse?"debit":"transfer"));
        String data=context.getJobData("data");
        
        // TODO dtaus-data auswerten
        
        context.addStatus(null,"0010","Auftrag entgegengenommen",null);
    }
}
