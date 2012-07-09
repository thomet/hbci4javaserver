
/*  $Id: MsgHandlerSendKeys.java,v 1.9 2005/06/10 18:03:03 kleiner Exp $

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

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerSendKeys 
    extends AbstractMsgHandler 
{
    public MsgHandlerSendKeys(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    // sysid kann hier zustzlich auch 0 sein
    protected boolean verifySysId(String sysid)
    {
        return (sysid.equals("0") || super.verifySysId(sysid));
    }
    
    // signatur-id wird hier komplett ignoriert, weil sie ja 
    // mit dieser nachricht quasi neu initialisiert wird
    // *** hier sollte evtl. nur "1" als sig-id anerkannt werden
    protected boolean verifySigId(long sigid)
    {
        return true;
    }
    
    protected void initMessageHandler()
    {
        dialog.setFlag("signResponse",true);
        dialog.setFlag("encryptResponse",false);
    }
    
    protected boolean isMessageAllowed()
    {
        return !dialog.isInitialized() && dialog.getPassportType().equals("RDH");
    }
    
    protected void handleMessageContent()
    {
        // check idn
        // kik ok
        if (!getData("Idn.KIK.country").equals(getServerData().getCountry()) ||
                !getData("Idn.KIK.blz").equals(getServerData().getBLZ())) {
            
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("Idn","2","9210","ungltige Bankkennung",null);
            return;
        }
        
        // customer-id berprfen
        String customerId=getData("Idn.customerid");
        if (!(new ArrayList(Arrays.asList(getServerData().getCustomerIds(dialog.getUserId()))).contains(customerId))) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("Idn","3","9210","ungltige Kunden-ID",null);
            return;
        }
        
        // sysid berprfen
        if (!getData("Idn.sysid").equals(dialog.getSysId())) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("Idn","4","9210","falsche System-ID",null);
            return;
        }
            
        HBCIKey[] userkeys=new HBCIKey[2];
        int foundKeys=0;

        // handle incoming key data
        for (int i=0;i<2;i++) {
            String header=HBCIUtilsInternal.withCounter("KeyChange",i);
            
            String blz=getData(header+".KeyName.KIK.blz");
            String country=getData(header+".KeyName.KIK.country");
            String userid=getData(header+".KeyName.userid");
            String keytype=getData(header+".KeyName.keytype");
            String keynum=getData(header+".KeyName.keynum");
            String keyversion=getData(header+".KeyName.keyversion");
            
            HBCIUtils.log("received "+keytype+" key for userid "+userid,HBCIUtils.LOG_INFO);
            
            // KIK in keyname verifizieren
            if (!blz.equals(getServerData().getBLZ()) ||
                    !country.equals(getServerData().getCountry())) {
                
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet(header,"4,1","9210","ungltige Bankkennung",null);
                return;
            }
            
            // userid in keyname verifizieren
            if (!userid.equals(dialog.getUserId())) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet(header,"4,2","9210","User-ID falsch",null);
                return;
            }
            
            if (keytype.equals("S")) {
                if ((foundKeys&1)!=0) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet(header,null,"9010","zwei Signierschlssel eingereicht",null);
                    return;
                }
                
                // prfen, ob schlssel tatschlich noch nicht vorhanden ist
                if (getServerData().getUserRDHSigKey(userid)!=null) {
                    dialog.setFlag("encryptResponse",true); // *** das geht nicht, weil crypt() das dontcrypt-flag auswertet
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet(header,null,"9010","Signierschlssel schon vorhanden",null);
                    return;
                }

                foundKeys|=1;
            } else {
                if ((foundKeys&2)!=0) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet(header,null,"9010","zwei Chiffrierschlssel eingereicht",null);
                    return;
                }

                // prfen, ob schlssel tatschlich noch nicht vorhanden ist
                if (getServerData().getUserRDHEncKey(userid)!=null) {
                    dialog.setFlag("encryptResponse",true); // *** das geht nicht, weil crypt() das dontcrypt-flag auswertet
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet(header,null,"9010","Chiffrierschlssel schon vorhanden",null);
                    return;
                }

                foundKeys|=2;
            }
            
            // keynum und -version != "999"
            if (keynum.equals("999")) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet(header,"4,4","9010","ungltige Schlsselnummer",null);
                return;
            }
            if (keyversion.equals("999")) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet(header,"4,5","9010","ungltige Schlsselversion",null);
                return;
            }
            
            // *** pubkey.usage und .mode ueberpruefen
            
            try {
                BigInteger modulus=new BigInteger(+1,getData(header+".PubKey.modulus").getBytes("ISO-8859-1"));
                BigInteger exponent=new BigInteger(+1,getData(header+".PubKey.exponent").getBytes("ISO-8859-1"));
                
                RSAPublicKeySpec spec=new RSAPublicKeySpec(modulus,exponent);
                KeyFactory fac=KeyFactory.getInstance("RSA");
                Key key=fac.generatePublic(spec);
                HBCIKey hbcikey=new HBCIKey(country,blz,userid,keynum,keyversion,key);
                
                if (keytype.equals("S"))
                    userkeys[0]=hbcikey;
                else
                    userkeys[1]=hbcikey;
            } catch (Exception e) {
                throw new HBCI_Exception(e.getMessage(),e);
            }
        }

        getServerData().setUserRDHSigKey(dialog.getUserId(),userkeys[0]);
        getServerData().setUserRDHEncKey(dialog.getUserId(),userkeys[1]);
        
        // signatur berprfen, bei fehler schlssel reverten
        if (!checkDigitalSignature(dialog.getUserId(),dialog.getSysId())) {
            // fehlermeldung wird hier schon von checkDigitalSignature() erzeugt 
            HBCIUtils.log("will not store new user keys",HBCIUtils.LOG_WARN);
            getServerData().setUserRDHSigKey(dialog.getUserId(),null);
            getServerData().setUserRDHEncKey(dialog.getUserId(),null);
        } else {
            // erst hier die Erfolgsmeldung dranpacken
            HBCIUtils.log("new user keys stored",HBCIUtils.LOG_INFO);
            addGlobRet("0020","Ausgefhrt",null);
            addSegRet("KeyChange",null,"0020","Schlssel entgegengenommen",null);
            addSegRet("KeyChange_2",null,"0020","Schlssel entgegengenommen",null);
            
            // noch die sigids zurcksetzen
            getServerData().clearSigIds(dialog.getUserId(),dialog.getSysId());
            getServerData().addSigId(dialog.getUserId(),dialog.getSysId(),Long.parseLong(getData("SigHead.secref")));
        }
    }
    
    protected void postProcess()
    {
        if (globRetIsOk()) {
            dialog.setInitialized(true);
            dialog.setAnonymous(false);
            dialog.setFlag("forceEnd",true);
            dialog.setFlag("endInSig",false);
            dialog.setFlag("endInCrypt",true);
            dialog.setFlag("endOutSig",true);
            dialog.setFlag("endOutCrypt",false);
        }
    }
}
