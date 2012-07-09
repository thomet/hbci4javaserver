
/*  $Id: MyBackend.java,v 1.4 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.demo.hbci.server.backend;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Saldo;
import org.kapott.hbci.structures.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/* Diese Klasse simuliert ein Bank-Hintergrundsystem */
public class MyBackend 
{
    private String   directory;    // Verzeichnis fr Daten-Dateien
    private XMLTools xml;
    private Random   random;
    
    public MyBackend(String directory)
    {
        this.directory=directory;
        this.xml=new XMLTools();
        this.random=new Random();
    }
    
    // eingegangene kundennachricht speichern
    public void storeCustomMsg(Konto acc,
                               String recpt,String subject,String msgtxt)
    {
        try {
            File dataFile=new File(directory+File.separator+acc.customerid+"_messagesIncoming");
            Document doc=xml.getFileContent(dataFile,"messages");
            Element content=doc.getDocumentElement();
            
            String id=Long.toString(random.nextLong()).substring(1);
            
            Element msg=doc.createElement("msg");
            content.appendChild(msg);
            
            msg.appendChild(xml.createTimestampElement(doc));
            msg.appendChild(xml.createElement(doc,"id",id));
            msg.appendChild(xml.createElement(doc,"account",acc.number));
            msg.appendChild(xml.createElement(doc,"recpt",recpt));
            msg.appendChild(xml.createElement(doc,"subject",subject));
            msg.appendChild(xml.createElement(doc,"message",msgtxt));

            xml.transform(doc,dataFile);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // kontobewegung speichern
    // my ist immer eigenes konto; vorzeichen von btg.value gibt an, ob etwas
    // dazukommt oder abgeht
    public void addTransfer(Konto my,Konto other,Value btg,String[] usage,String key,String addkey)
    {
        try {
            File dataFile=new File(directory+File.separator+my.customerid+"_transfers_"+my.number);
            Document doc=xml.getFileContent(dataFile,"transfers");
            Element content=doc.getDocumentElement();
            
            GVRKUms.UmsLine entry=new GVRKUms.UmsLine();
            double saldoOld=getSaldo(my);
            
            entry.bdate=new Date();
            entry.valuta=entry.bdate;
            entry.cd=btg.value<0?"D":"C";
            entry.other=other;
            entry.value=new Value(Math.abs(btg.value),btg.curr);
            
            double v=(Math.round(saldoOld*100.0)+Math.round(btg.value*100.0))/100.0;
            entry.saldo=new Saldo();
            entry.saldo.value=new Value(Math.abs(v),btg.curr);
            entry.saldo.cd=(v<0)?"D":"C";
            entry.saldo.timestamp=entry.valuta;
            
            entry.usage=usage;
            entry.gvcode="0"+key;
            entry.addkey=addkey;

            String id=Long.toString(random.nextLong()).substring(1);
            content.appendChild(xml.createUmsLineElement(doc,"transfer",entry,id));
            xml.transform(doc,dataFile);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // saldo fr bestimmtes konto zurckgeben
    public double getSaldo(Konto acc)
    {
        try {
            File dataFile=new File(directory+File.separator+acc.customerid+"_transfers_"+acc.number);
            Document doc=xml.getFileContent(dataFile,"transfers");
            Element content=doc.getDocumentElement();
            
            double saldo=0;
            NodeList transfer_entries=content.getElementsByTagName("transfer");
            int len;
            if (transfer_entries!=null && (len=transfer_entries.getLength())!=0) {
                Element lastEntry=(Element)transfer_entries.item(len-1);
                saldo=xml.readValueElement(lastEntry,"saldo",null).value;
            }
            
            return saldo;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // MT940-Kontoauszug erzeugen
    public String getStatementOfAccount(Konto my,Date from,Date to,boolean onlyNew)
    {
        StringBuffer ret=new StringBuffer();
        
        String lastid=null;
        if (onlyNew) {
            // datei mit zeiger auf letzten bermittelten eintrag ffnen uns auslesen
            File dataFile=new File(directory+File.separator+my.customerid+"_transfers_"+my.number+"_lastentry");
            Document doc=xml.getFileContent(dataFile,"lastentry");
            Element content=doc.getDocumentElement();
            lastid=xml.readElement(content,"id",null);
        }
        
        // datendatei ffnen bzw. anlegen
        File dataFile=new File(directory+File.separator+my.customerid+"_transfers_"+my.number);
        Document doc=xml.getFileContent(dataFile,"transfers");
        Element content=doc.getDocumentElement();
        
        // alle transfer-eintrge herausfischen
        NodeList transferList=content.getElementsByTagName("transfer");
        if (transferList.getLength()!=0) {
            // aus nodelist ein arraylist erzeugen
            ArrayList transferArray=new ArrayList();
            int len=transferList.getLength();
            for (int i=0;i<len;i++)
                transferArray.add(transferList.item(i));
            
            // arraylist evtl. um schon gesehene eintrge krzen
            if (lastid!=null)
                optionallyRemoveSeenEntries(transferArray,lastid);
            
            if (transferArray.size()!=0) {
                SimpleDateFormat dateFormat=new SimpleDateFormat("yyMMdd");
                
                DecimalFormat valueFormat=new DecimalFormat("0.##");
                DecimalFormatSymbols symbols=valueFormat.getDecimalFormatSymbols();
                symbols.setDecimalSeparator(',');
                valueFormat.setDecimalFormatSymbols(symbols);
                valueFormat.setDecimalSeparatorAlwaysShown(true);
                
                Iterator transfers=transferArray.iterator();
                Element  transfer=(Element)transfers.next();
                boolean isFirstOfDay=true;
                
                while (true) { // schleife ber alle transfer-eintrge
                    // Daten aus Eintrag extrahieren
                    Date valuta=xml.readDateTimeElement(transfer,"valuta",null);
                    String valuta_st=dateFormat.format(valuta);
                    Date bdate=xml.readDateTimeElement(transfer,"bdate",valuta);
                    String bdate_st=dateFormat.format(bdate);
                    String id=xml.readElement(transfer,"id",null);
                    
                    // liegt dieser Eintrag nicht im gewnschten Zeitraum
                    if (from!=null && !from.before(bdate) ||
                            to!=null && !bdate.before(to)) {
                        // entweder abbrechen oder nchster schleifendurchlauf
                        if (!transfers.hasNext()) {
                            break;
                        }
                        
                        transfer=(Element)transfers.next();
                        continue;
                    }
                    
                    // werte aus eintrag extrahieren
                    double saldo=xml.readValueElement(transfer,"saldo",null).value;
                    double value=xml.readValueElement(transfer,"value",null).value;
                    
                    if (isFirstOfDay) { // startsaldo schreiben
                        ret.append("\r\n");
                        ret.append(":20:0"+"\r\n");
                        ret.append(":25:"+my.blz+"/"+my.number+my.curr+"\r\n");
                        ret.append(":28C:0\r\n");
                        
                        double saldoBefore=(Math.round(saldo*100.0)-Math.round(value*100.0))/100.0;
                        ret.append(":60F:"+
                                (saldoBefore<0?"D":"C")+bdate_st+my.curr+
                                valueFormat.format(Math.abs(saldoBefore))+"\r\n");
                        
                        isFirstOfDay=false;
                    }
                    
                    // daten fr diese berweisung
                    // *** TRF evtl. anpassen
                    String instref;
                    
                    ret.append(":61:"+valuta_st+bdate_st.substring(2)+   // daten
                            (value<0?"D":"C")+my.curr.charAt(2)+    // value
                            valueFormat.format(Math.abs(value))+ 
                            "NTRF"+
                            xml.readElement(transfer,"customerref","NONREF")+   // customerref
                            ((instref=xml.readElement(transfer,"instref",null))!=null?"//"+instref:""));  // instref
                    
                    // wert in originalwhrung
                    Value   orig_value=xml.readValueElement(transfer,"orig",null);
                    boolean additionalAppended=false;
                    if (orig_value!=null) {
                        ret.append("\r\n//OCMT/"+orig_value.curr+valueFormat.format(orig_value.value)+"/");
                        additionalAppended=true;
                    }
                    
                    // gebhren
                    Value charge_value=xml.readValueElement(transfer,"charge",null);
                    if (charge_value!=null) {
                        if (!additionalAppended)
                            ret.append("\r\n/");
                        ret.append("/CHGS/"+charge_value.curr+valueFormat.format(charge_value.value)+"/");
                    }
                    
                    ret.append("\r\n");
                    
                    // start new tag
                    ret.append(":86:");
                    int posOfTagStart=ret.length();
                    
                    // gvcode
                    String gvcode=xml.readElement(transfer,"gvcode","000");
                    ret.append(gvcode);
                    ret.append("?00"+xml.key2Text(gvcode));
                    
                    // primanota
                    String primanota=xml.readElement(transfer,"primanota",null);
                    if (primanota!=null)
                        appendWithOptionalCRLF(ret,"?10"+primanota,posOfTagStart,65);
                    
                    // verwendungszweck
                    String[] usage=xml.readStringArrayElement(transfer,"usage",new String[0]);
                    int usageIdx=0;
                    for (usageIdx=0;usageIdx<Math.min(10,usage.length);usageIdx++) {
                        appendWithOptionalCRLF(ret,"?"+(20+usageIdx)+usage[usageIdx],posOfTagStart,65);
                    }
                    
                    // gegenkonto
                    Konto other=xml.readAccountElement(transfer,"other",new Konto());
                    appendWithOptionalCRLF(ret,"?30"+other.blz,posOfTagStart,65);
                    appendWithOptionalCRLF(ret,"?31"+other.number,posOfTagStart,65);
                    if (other.name!=null && other.name.length()!=0)
                        appendWithOptionalCRLF(ret,"?32"+other.name,posOfTagStart,65);
                    if (other.name2!=null && other.name2.length()!=0)
                        appendWithOptionalCRLF(ret,"?33"+other.name2,posOfTagStart,65);
                    
                    // zusatz-schlssel
                    String addkey=xml.readElement(transfer,"addkey",null);
                    if (addkey!=null)
                        appendWithOptionalCRLF(ret,"?34"+addkey,posOfTagStart,65);
                    
                    // zustzliche verwendungszweckzeilen
                    for (;usageIdx<Math.min(14,usage.length);usageIdx++) {
                        appendWithOptionalCRLF(ret,"?"+(50+usageIdx)+usage[usageIdx],posOfTagStart,65);
                    }
                    
                    ret.append("\r\n");
                    
                    // nchsten eintrag holen
                    String nextDate=null;
                    if (transfers.hasNext()) {
                        // wenn es einen nchsten eintrag gibt
                        transfer=(Element)transfers.next();
                        // buchungsdatum aus nchstem eintrag extrahieren
                        nextDate=dateFormat.format(xml.readDateTimeElement(transfer,"bdate",
                                xml.readDateTimeElement(transfer,"valuta",null)));
                    }
                    
                    // wenn das nchste buchungsdatum ungleich dem des gerade erzeugten
                    // eintrages ist,
                    if (!bdate_st.equals(nextDate)) {
                        // anschlusssaldo mit den *alten* daten erzeugen
                        ret.append(":62F:"+
                                (saldo<0?"D":"C")+bdate_st+"EUR"+
                                valueFormat.format(Math.abs(saldo))+"\r\n");
                        ret.append("-");
                        
                        if (nextDate==null) {
                            // wenn es kein nchstes bdate gibt, abbrechen
                            
                            // vorher aber noch last-seen-id schreiben
                            if (onlyNew) {
                                // datei mit zeiger auf letzten bermittelten eintrag neu erzeugen
                                doc=xml.newDoc();
                                content=xml.createElement(doc,"lastentry","");
                                doc.appendChild(content);
                                content.appendChild(xml.createElement(doc,"id",id));
                                dataFile=new File(directory+File.separator+my.customerid+"_transfers_"+my.number+"_lastentry");
                                xml.transform(doc,dataFile);
                            }
                            // erzeugung des kontoauszugs beenden
                            break;
                        } 
                        
                        // sonst erzeugen eines startsaldos erzwingen
                        isFirstOfDay=true;
                    }
                }
            }
        }
        
        return ret.toString();
    }
    
    private void optionallyRemoveSeenEntries(ArrayList transfers,String lastid)
    {
        Iterator i=transfers.iterator();
        
        // alle transfer-eintrge durchlaufen
        int currentIdx=0;
        while (i.hasNext()) {
            Element transfer=(Element)i.next();
            currentIdx++;
            String id=xml.readElement(transfer,"id",null);
            
            // wenn dessen id mit der last-id bereinstimmt
            if (lastid.equals(id)) {
                // alle eintrge bis hierhin aus array lschen
                HBCIUtils.log("removing all seen entries ("+currentIdx+") before and including id '"+lastid+"'",HBCIUtils.LOG_DEBUG);
                for (int j=0;j<currentIdx;j++)
                    transfers.remove(0);
                break;
            }
        }
    }
    
    private void appendWithOptionalCRLF(StringBuffer ret,String data,int posOfTagStart,int maxlen)
    {
        int lastCRLF=ret.lastIndexOf("\r\n");
        if (lastCRLF<posOfTagStart)
            lastCRLF=posOfTagStart;
        else
            lastCRLF+=2;
        
        if (ret.length()-lastCRLF+data.length()>(maxlen-2))
            ret.append("\r\n");
        ret.append(data);
    }
}
