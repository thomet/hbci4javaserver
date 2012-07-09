
/*  $Id: AbstractMsgHandler.java,v 1.14 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.passport.HBCIPassportList;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.protocol.SEG;
import org.kapott.hbci.protocol.factory.MSGFactory;
import org.kapott.hbci.security.Crypt;
import org.kapott.hbci.security.Sig;
import org.kapott.hbci.security.factory.CryptFactory;
import org.kapott.hbci.security.factory.SigFactory;
import org.kapott.hbci.server.Dialog;
import org.kapott.hbci.server.ServerData;
import org.kapott.hbci.server.StatusProtEntry;
import org.kapott.hbci.status.HBCIRetVal;
import org.w3c.dom.Element;

public abstract class AbstractMsgHandler 
    implements MsgHandler 
{
    protected MSG    cryptedMsg;
    protected MSG    decryptedMsg;
    protected String decryptedMsgData;
    protected Dialog dialog;
    
    private String     msgname;
    private ServerData serverdata;
    private Properties data;
    
    private ArrayList  globRets;
    private ArrayList  segRets;

    protected abstract void initMessageHandler();
    protected abstract boolean isMessageAllowed();
    protected abstract void handleMessageContent();
    protected abstract void postProcess();
    
    // verifySysId() und verifySigId() knnen auch berschrieben werden

    protected AbstractMsgHandler(MSG cryptedMsg,MSG decryptedMsg,
                                 String decryptedMsgData,Dialog conn)
    {
        this.cryptedMsg=cryptedMsg;
        this.decryptedMsg=decryptedMsg;
        this.decryptedMsgData=decryptedMsgData;
        this.dialog=conn;
        
        this.msgname=decryptedMsg.getName();
        this.serverdata=ServerData.getInstance();
        this.data=decryptedMsg.getData();
        
        this.globRets=new ArrayList();
        
        // soviele segrets anlegen wie es segmente gibt
        int size=Integer.parseInt(getData("MsgTail.SegHead.seq"));
        HBCIUtils.log("creating segrets for "+size+" segments",HBCIUtils.LOG_DEBUG);
        this.segRets=new ArrayList();
        for (int i=0;i<size;i++)
            this.segRets.add(null);
        
        // extract element paths
        Properties paths=new Properties();
        decryptedMsg.getElementPaths(paths,null,null,null);
        for (Enumeration e=paths.propertyNames();e.hasMoreElements();) {
            String key=(String)e.nextElement();
            data.setProperty(key,paths.getProperty(key));
        }
    }
    
    public final MSG handleMessage()
    {
        HBCIUtils.log("handling incoming message: "+decryptedMsgData,HBCIUtils.LOG_DEBUG);
        dialog.getMsgGen().reset();
        initMessageHandler();
        
        // first check dialogid/msgnum; if there is something wrong, abort
        checkMessageFrame();
        
        // ist nachricht im moment erlaubt? (dialoginit innerhalb eines dialoges
        // ist es z.b. nicht); wenn nicht, dann abbruch des dialoges
        if (!isMessageAllowed())
            throw new HBCI_Exception("message not allowed in current context");

        // signatur berprfen
        if (checkMessageSignature()) {
            // wenn signatur ok, dann nachrichteninhalt bearbeiten
            handleMessageContent();
        } else {
            HBCIUtils.log("message signature is wrong",HBCIUtils.LOG_ERR);
        }
        
        // wenn nicht abgebrochen wurde (also wenn msgframe und allowed() okay
        // waren und auch keine exceptions beim content-handling aufgetreten
        // sind), dann antwortnachricht erzeugen
        MSG ret=createResponseMessage();
        postProcess();
        
        return ret;
    }
    
    private void checkMessageFrame()
    {
        HBCIUtils.log("checking message frame",HBCIUtils.LOG_DEBUG);
        
        // dialogid
        String dialogId=getData("MsgHead.dialogid");
        if (!dialogId.equals(dialog.getDialogId())) {
            throw new HBCI_Exception("invalid dialog-id");
        }
        
        // msgnum (head und tail)
        if (!getData("MsgHead.msgnum").equals(dialog.getMsgNum()) ||
                !getData("MsgTail.msgnum").equals(dialog.getMsgNum())) {
            throw new HBCI_Exception("invalid message number");
        }
        
        if (dialogId.equals("0"))
            dialog.createNewDialogId();
    }
    
    private boolean checkMessageSignature()
    {
        HBCIUtils.log("checking message signature",HBCIUtils.LOG_DEBUG);
        
        // sighead extrahieren
        // TODO: hier multi-sigs implementieren
        MsgGen msggen=dialog.getMsgGen();
        SEG sighead=(SEG)decryptedMsg.getElement(msgname+".SigHead");
        
        // ist eine signatur vorhanden?
        if (sighead!=null) {
            HBCIUtils.log("found sighead",HBCIUtils.LOG_DEBUG);
            
            // daten aus sighead extrahieren
            String header=sighead.getPath();
            String userid=sighead.getValueOfDE(header+".KeyName.userid");
            long   sigid=Long.parseLong(getData("SigHead.secref"));
            String sysid=sighead.getValueOfDE(header+".SecIdnDetails.sysid");
            if (sysid==null)
                sysid="0";
            
            // TODO verify all sighead data (auer keynum/-version, hashalg, sigalg, sigmode)
            
            HBCIUtils.log("found sighead data userid/sysid/sigid = "+userid+"/"+sysid+"/"+sigid,
                    HBCIUtils.LOG_DEBUG);
            
            // userid berprfen
            // wenn das die erste nachricht im dialog ist
            if (!dialog.isInitialized()) {
                HBCIUtils.log("checking first sighead in dialog",HBCIUtils.LOG_DEBUG);

                // existiert diese userid berhaupt?
                if (!getServerData().existsUserId(userid)) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet("SigHead","11,2","9210","ungltige User-ID",null);
                    return false;
                }
                dialog.setUserId(userid);

                // schlssel vorhanden? (ausnahme sendkeys)
                if (!getServerData().existUserSigKeys(dialog)) {
                    HBCIUtils.log("keys for this user dont exist",HBCIUtils.LOG_WARN);
                    if (this instanceof MsgHandlerSendKeys) {
                        HBCIUtils.log("ignoring this, because this is a sendkeys message",HBCIUtils.LOG_WARN);
                    } else {
                        addGlobRet("9010","Vararbeitung nicht mglich",null);
                        addSegRet("SigHead",null,"9310","Elektronische Signatur noch nicht hinterlegt",null);
                        return false;
                    }
                }

                // systemid berprfen
                if (!verifySysId(sysid))
                    return false;
                dialog.setSysId(sysid);
            } else { // nicht die erste nachricht im dialog
                HBCIUtils.log("checking following sighead in dialog",HBCIUtils.LOG_DEBUG);
                
                // userid muss ber den gesamten dialog gleich bleiben
                if (!userid.equals(dialog.getUserId())) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet("SigHead","11,2","9210","falsche User-ID",null);
                    return false;
                }

                // sysid muss konstant bleiben
                if (!sysid.equals(dialog.getSysId())) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet("SigHead","6,3","9210","falsche System-ID",null);
                    return false;
                }
            }

            // check sigid (ausnahme sendkeys,sync)
            if (!verifySigId(sigid))
                return false;
                
            // signatur berprfen
            // bei sendkeys wird spter verified
            if (!(this instanceof MsgHandlerSendKeys)) {
                if (!checkDigitalSignature(userid,sysid))
                    return false;
            } else {
                HBCIUtils.log("delaying signature check",HBCIUtils.LOG_DEBUG);
            }
            
            return true;
        } 
        
        // keine signatur vorhanden
        HBCIUtils.log("message is not signed",HBCIUtils.LOG_DEBUG);
        
        // nachrichtedefinition aus spez. holen
        Element msgdef=msggen.getSyntax().getElementById(msgname);
        String dontSign=msgdef.getAttribute("dontsign");
        
        // wenn attribut "dontsign" gesetzt, dann ist das fehlen der sig. ok
        if (dontSign!=null && dontSign.equals("1")) {
            HBCIUtils.log("this message does not need to be signed",HBCIUtils.LOG_DEBUG);
        } else if (dialog.getFlag("forceEnd") && !dialog.getFlag("endInSig") &&
                (this instanceof MsgHandlerDialogEndAnon)) 
        {
            HBCIUtils.log("this is a special end message - no signature needed",HBCIUtils.LOG_DEBUG);
        } else { // sonst fehler erzeugen
            HBCIUtils.log("missing signature",HBCIUtils.LOG_ERR);
            addGlobRet("9110","Signatur fehlt",null);
            return false;
        }
        
        return true;
    }

    // berprfen, ob die System-ID im sighead ok ist
    protected boolean verifySysId(String sysid)
    {
        // system-id ist ungltig, wenn sie entweder 0 oder nicht identisch
        // mit einer aus den server-daten ist (das wird von einigen special-
        // messages berschrieben)
        String[]  validSysIds_a=getServerData().getSysIds(dialog.getUserId());
        ArrayList validSysIds=new ArrayList(Arrays.asList(validSysIds_a));
        
        if (sysid.equals("0") || !validSysIds.contains(sysid)) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("SigHead","6,3","9210","ungltige System-ID",null);
            return false;
        }
        
        return true;
    }
    
    // berprfen, on die sig-id ok ist
    protected boolean verifySigId(long sigid)
    {
        if (!dialog.getPassportType().equals("PinTan")) {
            // der normale fall ist, dass die sig-id noch nicht benutzt worden
            // sein darf (einige special messages berschreiben dieses verhalten)
            if (getServerData().existsSigId(dialog.getUserId(),dialog.getSysId(),sigid)) {
                HBCIUtils.log("duplicate sigid found",HBCIUtils.LOG_WARN);
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("SigHead","7","9390","Doppeleinreichung",null);
                return false;
            }
            // aktuelle sigid in liste der schon eingereichten ids aufnehmen
            getServerData().addSigId(dialog.getUserId(),dialog.getSysId(),sigid);
            return true;
        }
        
        return true;
    }
    
    protected boolean checkDigitalSignature(String userid,String sysid)
    {
        HBCIUtils.log("now checking digital signature",HBCIUtils.LOG_DEBUG);

        MsgGen msggen=dialog.getMsgGen();
        msggen.set("_origSignedMsg",decryptedMsgData);
        
        // TODO: this has to be changed for multi-sigs
        HBCIPassportList passports=new HBCIPassportList();
        passports.addPassport(
                getServerData().getPassport(dialog),
                HBCIPassport.ROLE_ISS);
        Sig sig=SigFactory.getInstance().createSig(decryptedMsg,msggen,passports);
        
        boolean sigOk=sig.verify();
        SigFactory.getInstance().unuseObject(sig);
        
        // wenn signatur falsch ist, fehlermeldung erzeugen
        if (!sigOk) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("SigTail",null,"9340","Elektronische Signatur falsch",null);
            return false;
        }
        
        return true;
    }
    
    private final MSG createResponseMessage()
    {
        HBCIUtils.log("creating response message",HBCIUtils.LOG_DEBUG);
        
        String dialogid=dialog.getDialogId();
        String msgnum=dialog.getMsgNum();
        
        // fill msghead of response
        setData("MsgHead.dialogid",dialogid);
        setData("MsgHead.msgnum",msgnum);
        setData("MsgHead.MsgRef.dialogid",dialogid);
        setData("MsgHead.MsgRef.msgnum",msgnum);
        setData("MsgTail.msgnum",msgnum);
        
        StatusProtEntry pentry=new StatusProtEntry();
        pentry.dialogid=dialogid;
        pentry.msgnum=msgnum;
        pentry.timestamp=new Date();

        // create glob rets
        int counter=0;
        for (Iterator i=globRets.iterator();i.hasNext();) {
            HBCIRetVal retval=(HBCIRetVal)i.next();
            String header=HBCIUtilsInternal.withCounter("RetGlob.RetVal",counter++);
            
            setData(header+".code",retval.code);
            setData(header+".text",retval.text);
            // *** parm
            
            // save in status protokoll
            pentry.retval=retval;
            getServerData().addToStatusProt(dialog.getUserId(),pentry);
        }
        
        // creage seg rets
        int segcounter=0;
        for (Iterator i=segRets.iterator();i.hasNext();) {
            ArrayList rets=(ArrayList)i.next();
            
            if (rets!=null) {
                String segheader=HBCIUtilsInternal.withCounter("RetSeg",segcounter++);
                String segref=((HBCIRetVal)rets.get(0)).segref;
                
                setData(segheader+".SegHead.ref",segref);
                pentry.segref=segref;
                
                counter=0;
                for (Iterator j=rets.iterator();j.hasNext();) {
                    HBCIRetVal retval=(HBCIRetVal)j.next();
                    String header=HBCIUtilsInternal.withCounter(segheader+".RetVal",counter++);
                    
                    setData(header+".code",retval.code);
                    setData(header+".text",retval.text);
                    if (retval.deref!=null)
                        setData(header+".ref",retval.deref);
                    // *** parm
                    
                    // save in status protokoll
                    pentry.retval=retval;
                    getServerData().addToStatusProt(dialog.getUserId(),pentry);
                }
            }
        }
        
        // plaintextnachricht erzeugen
        MSG msg=dialog.getMsgGen().generate(msgname+"Res");
        
        // passport fr sig/crypt besorgen
        String userid=dialog.getUserId();
        HBCIPassportInternal passport=(userid==null?dialog.getLocalPassport()
                                                   :getServerData().getPassport(dialog));

        // evtl. signieren
        if (dialog.getFlag("signResponse")) {
            HBCIUtils.log("trying to sign response message",HBCIUtils.LOG_DEBUG);
            
            // this is for multisigs
            HBCIPassportList passports=new HBCIPassportList();
            passports.addPassport(passport,HBCIPassport.ROLE_ISS);
            Sig sig=SigFactory.getInstance().createSig(msg,dialog.getMsgGen(),passports);
            
            try {
                if (!sig.signIt())
                    throw new HBCI_Exception("can not sign response message");
            } finally {
                SigFactory.getInstance().unuseObject(sig);
            }
            HBCIUtils.log("signature process done",HBCIUtils.LOG_DEBUG);
        } else {
            HBCIUtils.log("response message does not need to be signed",HBCIUtils.LOG_DEBUG);
        }
        
        HBCIUtils.log("response message: "+msg.toString(0),HBCIUtils.LOG_DEBUG);
        
        // evtl. verschlsseln
        if (dialog.getFlag("encryptResponse")) {
            HBCIUtils.log("encrypting response message",HBCIUtils.LOG_DEBUG);
            Crypt crypt=CryptFactory.getInstance().createCrypt(msg,dialog.getMsgGen(),passport);
            try {
                MSG old=msg;
                msg=crypt.cryptIt("CryptedRes");
                if (old!=msg) {
                    MSGFactory.getInstance().unuseObject(old);
                }
            } finally {
                CryptFactory.getInstance().unuseObject(crypt);
            }
        } else {
            HBCIUtils.log("response message will not be encrypted",HBCIUtils.LOG_DEBUG);
        }
        
        return msg;
    }
    
    protected ServerData getServerData()
    {
        return serverdata;
    }
    
    protected Properties getData()
    {
        return data;
    }
    
    protected String getData(String key)
    {
        return data.getProperty(key);
    }
    
    protected void setData(String key,String value)
    {
        dialog.getMsgGen().set(msgname+"Res."+key,value);
    }
    
    private boolean retExists(ArrayList rets,HBCIRetVal ret)
    {
        boolean exists=false;
        
        for (Iterator i=rets.iterator();i.hasNext();) {
            if (((HBCIRetVal)i.next()).equals(ret)) {
                exists=true;
                break;
            }
        }
        
        return exists;
    }

    // einen rckgabewert zu einer menge von rckgabewerten hinzufgen.
    // "menge" ist dabei entweder globrets oder die menge von retvals fr ein segment.
    // ein retval wird nur dann hinzugefgt, wenn er noch nicht existiert.
    // ein success-retval wird nur hinzugefgt, wenn noch kein error-retval existiert
    // ein error-retval lscht alle schon vorhandenen success-retvals
    private void addRet(ArrayList rets,HBCIRetVal ret)
    {
        if (!retExists(rets,ret)) {
            boolean addIt=true;
            char firstChar=ret.code.charAt(0);
            
            if (firstChar=='0') {
                for (Iterator i=rets.iterator();i.hasNext();) {
                    if (((HBCIRetVal)i.next()).code.charAt(0)=='9') {
                        HBCIUtils.log("can not add success message because error exists",HBCIUtils.LOG_DEBUG);
                        addIt=false;
                        break;
                    }
                }
            } else if (firstChar=='9') {
                ArrayList newRets=new ArrayList();
                for (Iterator i=rets.iterator();i.hasNext();) {
                    HBCIRetVal r=(HBCIRetVal)i.next();
                    if (r.code.charAt(0)!='0') {
                        newRets.add(r);
                    } else {
                        HBCIUtils.log("removing success message because of error message: "+r,HBCIUtils.LOG_DEBUG);
                    }
                }
                rets.clear();
                rets.addAll(newRets);
            }
            
            if (addIt)
                rets.add(ret);
        }
    }
    
    protected void addGlobRet(String code,String text,String[] params)
    {
        HBCIRetVal ret=new HBCIRetVal(null,null,null,code,text,params);
        addRet(globRets,ret);
    }
    
    protected void addSegRet(String segname,String deref,String code,String text,String[] params)
    {
        String segref=getData(segname+".SegHead.seq");
        HBCIRetVal ret=new HBCIRetVal(segref,deref,null,code,text,params);
        
        int segref2=Integer.parseInt(segref);
        ArrayList rets=(ArrayList)segRets.get(segref2);
        if (rets==null) {
            rets=new ArrayList();
            segRets.set(segref2,rets);
        }
        addRet(rets,ret);
    }
    
    private boolean retsOk(ArrayList rets)
    {
        boolean ok=true;
        
        for (Iterator i=rets.iterator();i.hasNext();) {
            if (((HBCIRetVal)i.next()).code.charAt(0)=='9') {
                ok=false;
                break;
            }
        }
        
        return ok;
    }
    
    protected boolean globRetIsOk()
    {
        return retsOk(globRets);
    }
    
    public Dialog getDialog()
    {
        return dialog;
    }
    
    public int updateMsgNum(int msgnum)
    {
        return msgnum+1;
    }
}
