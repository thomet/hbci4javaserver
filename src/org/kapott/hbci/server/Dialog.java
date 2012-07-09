
/*  $Id: Dialog.java,v 1.3 2005/06/10 18:03:02 kleiner Exp $

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
import java.security.Security;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.protocol.SEG;
import org.kapott.hbci.protocol.factory.MSGFactory;
import org.kapott.hbci.security.Crypt;
import org.kapott.hbci.security.HBCIProvider;
import org.kapott.hbci.security.factory.CryptFactory;
import org.kapott.hbci.server.msg.MsgHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Dialog
{
    private MsgGen     msggen;  // nachrichtengenerator fr aktuellen dialog
    private Properties bpd;     // bpd, die fr aktuellen dialog gltig sind

    // anonymes passport fr signatur und entschlsselund auf server-seite
    private String               passportType;
    private HBCIPassportInternal localPassport;
    
    private boolean   isInitialized; // dialog-init oder quivalent schon ausgefhrt?  
    private boolean   isAnonymous;   // anonymer dialog
    private Hashtable flags;        // flags fr sign/encrypt response usw.
    
    private String    hbciversion; // hbci-version, in der dieser dialog gefhrt wird
    private String    userid;      // userid fr aktuellen dialog
    private String    customerid;  // kunden-id fr aktuellen dialog
    private String    sysid;       // system-id fr aktuellen dialog
    private String    dialogid;    // dialog-id fr aktuellen dialog
    private int       msgnum;      // zaehler fr nachrichtennummer
    
    private Date lastactive;
    
    public Dialog(String passportType)
    {
        HBCIUtils.log("creating new dialog object",HBCIUtils.LOG_DEBUG);

        this.flags=new Hashtable();
        this.hbciversion=null;
        this.msggen=null;
        this.bpd=null;
        this.userid=null;
        this.customerid=null;
        this.sysid="0";
        this.dialogid="0";
        this.msgnum=1;
        this.passportType=passportType;
        
        setInitialized(false);
        setAnonymous(true);
        setFlag("forceEnd",false);
        touch();
    }
    
    // eingehende Nachricht handeln
    public String handleMessage(StringBuffer msg)
    {
        touch();
        MSG ret=null;
        
        try {
            initDialog(msg);

            // ist nachricht verschlsselt?
            StringBuffer cryptedMsgData=new StringBuffer(msg.toString());
            MSG cryptedMsg=null;
            try {
                HBCIUtils.log("checking if message is encrypted",HBCIUtils.LOG_DEBUG);
                cryptedMsg=MSGFactory.getInstance().createMSG("Crypted",cryptedMsgData.toString(),cryptedMsgData.length(),msggen,false);
                HBCIUtils.log("message is encrypted",HBCIUtils.LOG_INFO);
            } catch (Exception e) {
                HBCIUtils.log("message does not seem to be encrypted",HBCIUtils.LOG_INFO);
            }
            
            try {
                StringBuffer decryptedMsgData=new StringBuffer(cryptedMsgData.toString());
                
                if (cryptedMsg!=null) {
                    HBCIUtils.log("analyzing crypted message",HBCIUtils.LOG_DEBUG);
                    
                    // schlsselkopf extrahieren
                    SEG cryptHead=(SEG)cryptedMsg.getElement("Crypted.CryptHead");
                    
                    // daten aus schlsselkopf holen
                    Hashtable cryptValues=new Hashtable();
                    cryptHead.extractValues(cryptValues);
                    
                    // schlsselkopf checken
                    checkCryptHead(cryptValues);
                    
                    try {
                        // anonymes passport erzeugen (oder holen)
                        HBCIUtils.log("trying to decrypt encrypted message",HBCIUtils.LOG_DEBUG);
                        Crypt crypt=CryptFactory.getInstance().createCrypt(cryptedMsg,msggen,getLocalPassport());
                        try {
                            decryptedMsgData=new StringBuffer(crypt.decryptIt());
                            HBCIUtils.log("decrypted message: "+decryptedMsgData,HBCIUtils.LOG_DEBUG);
                        } finally {
                            CryptFactory.getInstance().unuseObject(crypt);
                        }
                    } catch (Exception e) {
                        throw new HBCI_Exception("error while decrypting message",e);
                    }
                }
                
                // nachrichtentyp ermitteln (alle msgdefs durchgehen und versuchen zu parsen)
                // *** hier evtl. nicht *alle* msgs checken, sondern nur die, die an dieser
                // stelle sinn machen
                Document syntax=msggen.getSyntax();
                NodeList msgdefs=syntax.getElementsByTagName("MSGdef");
                int len=msgdefs.getLength();
                
                // alle msgdef-elemente durchlaufen
                MSG decryptedMsg=null;
                for (int i=0;i<len;i++) {
                    Element msgdef=(Element)msgdefs.item(i);
                    String msgName=msgdef.getAttribute("id");
                    
                    // wenn das eine client-nachrichtenspez. ist
                    if (!msgName.endsWith("Res")) {
                        try {
                            HBCIUtils.log("trying to parse msg as "+msgName,HBCIUtils.LOG_DEBUG);
                            
                            // crypted / nicht crypted checken
                            String dontCrypt=msgdef.getAttribute("dontcrypt");
                            if (dontCrypt==null || dontCrypt.length()==0) { // nachricht muss verschlsselt werden
                                if (cryptedMsg==null) { // war aber nicht verschlsselt
                                    throw new HBCI_Exception("was not encrypted");
                                }
                            } else { // nachricht muss nicht verschlsselt werden
                                if (cryptedMsg!=null) { // war sie aber doch
                                    throw new HBCI_Exception("was encrypted");
                                }
                            }
                            
                            // versuch, msg zu parsen 
                            decryptedMsg=MSGFactory.getInstance().createMSG(msgName,
                                    decryptedMsgData.toString(),
                                    decryptedMsgData.length(),
                                    msggen,true);
                        } catch (Exception e) {
                            HBCIUtils.log("failed",HBCIUtils.LOG_DEBUG);
                        }
                        
                        // wenn message parsen geklappt hat
                        if (decryptedMsg!=null)
                            break;
                    }
                }
                
                // wenn nachricht gar nicht geparst werden konnte, fehler 
                if (decryptedMsg==null)
                    throw new HBCI_Exception("can not parse this message");
                
                HBCIUtils.log("message has been parsed as "+decryptedMsg.getName()+" message",HBCIUtils.LOG_INFO);
                
                try {
                    // messagehandler fr den jeweiligen nachrichtentyp erzeugen
                    Class cl=Class.forName("org.kapott.hbci.server.msg.MsgHandler"+decryptedMsg.getName());
                    Constructor cons=cl.getConstructor(new Class[] {MSG.class,MSG.class,String.class,Dialog.class});
                    
                    // ret=messagehandler aufrufen (originalnachricht, entschlsselte nachricht
                    // und <this> bergeben)
                    MsgHandler handler=(MsgHandler)cons.newInstance(new Object[]
                                                                               {cryptedMsg,decryptedMsg,decryptedMsgData.toString(),this});
                    ret=handler.handleMessage();
                    msgnum=handler.updateMsgNum(msgnum);
                } finally {
                    if (decryptedMsg!=null)
                        MSGFactory.getInstance().unuseObject(decryptedMsg);
                }
            } finally {
                if (cryptedMsg!=null) 
                    MSGFactory.getInstance().unuseObject(cryptedMsg);
            }
        } catch (Exception e) {
            HBCIUtils.log(e);
            if (ret!=null)
                MSGFactory.getInstance().unuseObject(ret);
            ret=createAbortMessage(e.getClass().getName()+": "+e.getMessage());
        }
        
        String retvalue=ret.toString(0);
        MSGFactory.getInstance().unuseObject(ret);
        
        return retvalue;
    }
    
    // erzeugen einer Dialog-Abbruch-Nachricht
    private MSG createAbortMessage(String msg)
    {
        HBCIUtils.log("creating dialog abort message",HBCIUtils.LOG_INFO);
        
        if (msggen==null) {
            createMsgGen("210"); // TODO: hier die hchste der untersttzten hbci-versionen verwenden
        }
        msggen.reset();
        
        String msgNumber=getMsgNum();
        String dialogId=getDialogId();
        
        msggen.set("DialogInitAnonRes.MsgHead.dialogid",dialogId);
        msggen.set("DialogInitAnonRes.MsgHead.msgnum",msgNumber);
        msggen.set("DialogInitAnonRes.MsgHead.MsgRef.dialogid",dialogId);
        msggen.set("DialogInitAnonRes.MsgHead.MsgRef.msgnum",msgNumber);
        msggen.set("DialogInitAnonRes.MsgTail.msgnum",msgNumber);
        
        if (msg.length()>80)
            msg=msg.substring(0,80);
        
        msggen.set("DialogInitAnonRes.RetGlob.RetVal.code","9800");
        msggen.set("DialogInitAnonRes.RetGlob.RetVal.text",msg);
        
        return msggen.generate("DialogInitAnonRes");
    }
    
    private void initDialog(StringBuffer msg)
    {
        // hbciversion steht noch nicht fest
        if (msggen==null) {
            HBCIUtils.log("have to determine used hbci version",HBCIUtils.LOG_DEBUG);
            // hbciversion aus msghead extrahieren
            int[] dotPos=new int[3];
            for (int i=0;i<3;i++) {
                dotPos[i]=msg.indexOf("+",(i==0)?0:(dotPos[i-1]+1));
                if (dotPos[i]==-1) 
                    throw new HBCI_Exception("can not extract hbci version from message");
            }
            
            // kernel-objekt fr diese hbciversion erzeugen
            hbciversion=msg.substring(dotPos[1]+1,dotPos[2]);

            if (hbciversion.equals("220") && passportType.equals("PinTan")) {
                hbciversion="plus";
            }
            
            createMsgGen(hbciversion);
            bpd=ServerData.getInstance().getBPD(hbciversion);
        }
    }
    
    // nachrichtengenerator fr eine bestimmte hbci-version erzeugen
    private void createMsgGen(String hbciversion)
    {
        HBCIUtils.log("searching for already initialized msggen for version "+hbciversion,HBCIUtils.LOG_DEBUG);
        
        // schon mal einen msggen fr diese hbciversion initialisiert? 
        MsgGen oldgen=ServerData.getInstance().getMsgGen(hbciversion);
        if (oldgen==null) { // nein, also neuen msggen erzeugen
            HBCIUtils.log("initializing new message generator for version "+hbciversion,HBCIUtils.LOG_INFO);
            
            // pfad zur xml-spec ermitteln
            String xmlpath=HBCIUtils.getParam("kernel.kernel.xmlpath");
            InputStream syntaxStream=null;
            if (xmlpath==null) {
                xmlpath="";
            }
            
            // inputstream fr xml-spec erzeugen
            ClassLoader cl=HBCIUtils.class.getClassLoader();
            String filename=xmlpath+"hbci-"+hbciversion+".xml";
            syntaxStream=cl.getResourceAsStream(filename);
            if (syntaxStream==null) 
                throw new HBCI_Exception("could not find syntax specification for hbciversion "+hbciversion);
            
            // message-generator erzeugen
            try {
                msggen=new MsgGen(syntaxStream);
                ServerData.getInstance().storeMsgGen(hbciversion,msggen);
                
                if (Security.getProvider("HBCIProvider")==null) {
                    Security.addProvider(new HBCIProvider());
                }
            } catch (Exception e) {
                throw new HBCI_Exception("could not initialize message generator for hbciversion "+hbciversion,e);
            }
            
            HBCIUtils.log("message generator for hbciversion "+hbciversion+" initialized",HBCIUtils.LOG_DEBUG);
        } else { // es gab schon mal einen msggen fr diese hbciversion
            // msggen muss nicht komplett neu erzeugt werden (syntax parsen dauert lange),
            // sondern es kann die syntax eines alten msggen verwendet werden
            
            HBCIUtils.log("found already initialized msggen for version "+hbciversion+" - reusing it",HBCIUtils.LOG_DEBUG);
            msggen=new MsgGen(oldgen.getSyntax());
        }
    }
    
    private void checkCryptHead(Hashtable values)
    {
        HBCIUtils.log("checking cryptHead data",HBCIUtils.LOG_DEBUG);
        
        String st;
        if (!(st=((String)values.get("Crypted.CryptHead.SegHead.seq"))).equals("998"))
            throw new HBCI_Exception("segment number of crypthead is invalid: "+st);
        
        /* *** disabled for pintan 
        if (!(st=((String)values.get("Crypted.CryptHead.secfunc"))).equals("4"))
            throw new HBCI_Exception("secfunc of crypthead has invalid value: "+st); */
        
        if (!(st=((String)values.get("Crypted.CryptHead.role"))).equals("1"))
            throw new HBCI_Exception("role of crypthead has invalid value: "+st);
        
        // *** usw.
        // *** die sysid muss nachtrglich geprft werden, wenn userpassport klar
        // ist und sysid feststeht
        
        HBCIUtils.log("cryptHead data seems to be ok",HBCIUtils.LOG_DEBUG);
    }
    
    public void createNewDialogId()
    {
        dialogid=Long.toString(ServerData.getInstance().nextRandom()).substring(1);
        if (dialogid.length()>30)
            dialogid=dialogid.substring(0,30);
        DialogMgr.getInstance().addDialog(dialogid,this);
        HBCIUtils.log("created new dialogid "+dialogid,HBCIUtils.LOG_INFO);
    }
    
    public String getDialogId()
    {
        return dialogid;
    }
    
    public String getMsgNum()
    {
        return Integer.toString(msgnum);
    }
    
    public MsgGen getMsgGen()
    {
        return msggen;
    }
    
    public boolean isInitialized()
    {
        return isInitialized;
    }
    
    public void setInitialized(boolean init)
    {
        isInitialized=init;
    }
    
    public boolean isAnonymous()
    {
        return isAnonymous;
    }
    
    public void setAnonymous(boolean anon)
    {
        isAnonymous=anon;
    }
    
    public Properties getBPD()
    {
        return bpd;
    }
    
    public String getHBCIVersion()
    {
        return hbciversion;
    }
    
    public boolean getFlag(String name)
    {
        return ((Boolean)flags.get(name)).booleanValue();
    }
    
    public void setFlag(String name,boolean value)
    {
        flags.put(name,new Boolean(value));
    }
    
    public HBCIPassportInternal getLocalPassport()
    {
        if (localPassport==null) {
            try {
                Class       cl=Class.forName("org.kapott.hbci.server.passport.LocalPassport"+getPassportType());
                Constructor cons=cl.getConstructor(null);
                localPassport=(HBCIPassportInternal)cons.newInstance(null);
            } catch (Exception e) {
                throw new HBCI_Exception(e);
            }
        }
        
        return localPassport;
    }
    
    public String getUserId()
    {
        return userid;
    }
    
    public void setUserId(String userid)
    {
        this.userid=userid;
    }
    
    public String getCustomerId()
    {
        return customerid;
    }
    
    public void setCustomerId(String customerid)
    {
        this.customerid=customerid;
    }
    
    public String getSysId()
    {
        return sysid;
    }
    
    public void setSysId(String sysid)
    {
        this.sysid=sysid;
    }
    
    public String getPassportType()
    {
        return passportType;
    }
    
    private void touch()
    {
        this.lastactive=new Date();
    }
    
    public Date getLastActive()
    {
        return lastactive;
    }
}
