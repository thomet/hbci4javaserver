
/*  $Id: MsgHandlerFirstKeyReq.java,v 1.6 2005/06/10 18:03:03 kleiner Exp $

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

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerFirstKeyReq 
    extends MsgHandlerDialogInitAnon 
{
    public MsgHandlerFirstKeyReq(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    protected void initMessageHandler()
    {
        super.initMessageHandler();
        dialog.setFlag("signResponse",true);
    }
    
    protected boolean isMessageAllowed()
    {
        return !dialog.isInitialized() && dialog.getPassportType().equals("RDH");
    }
    
    protected void handleMessageContent()
    {
        super.handleMessageContent();
        
        if (globRetIsOk()) {
            // zwei keyrequests 
            int reqsFound=0;
            int responseCounter=0;
            
            for (int i=0;i<2;i++) {
                String srcHeader=HBCIUtilsInternal.withCounter("KeyReq",i);
                String srcHeader2=srcHeader+".KeyName";
                
                // wenn ein keyrequest da ist
                if (getData(srcHeader+".SegHead.code")!=null) { 
                    // schlsseldaten berprfen
                    if (!getData(srcHeader2+".KIK.country").equals(getServerData().getCountry()) ||
                            !getData(srcHeader2+".KIK.blz").equals(getServerData().getBLZ())) {
                        
                        addGlobRet("9010","Verarbeitung nicht mglich",null);
                        addSegRet(srcHeader,"4,1","9210","falsche KIK in Schlsseldaten",null);
                        return;
                    }

                    // schlsseldaten extrahieren
                    String keytype=getData(srcHeader2+".keytype");
                    String keynum=getData(srcHeader2+".keynum");
                    String keyversion=getData(srcHeader2+".keyversion");
                    
                    // welcher inst.-schlssel wird hier abgefragt
                    HBCIKey instKey;
                    if (keytype.equals("S"))
                        instKey=getServerData().getInstPublicRDHSigKey();
                    else
                        instKey=getServerData().getInstPublicRDHEncKey();

                    // schlsselnummer/-version berprfen, ob update notwendig ist
                    boolean needUpdate=false;
                    // update ist ntig, wenn client die schlssel gar nicht kennt 
                    if (keynum.equals("999") && keyversion.equals("999")) {
                        needUpdate=true;
                    } else {
                        // daten der dem client bekannten schlssel ermitteln
                        int sentKeyNum=Integer.parseInt(keynum);
                        int sentKeyVersion=Integer.parseInt(keyversion);
                        
                        // daten des entsprechenden instituts-schlssels ermitteln
                        int sigKeyNum=Integer.parseInt(instKey.num);
                        int sigKeyVersion=Integer.parseInt(instKey.version);
                        
                        // update ist ntig, wenn client-daten lter als inst-daten sind
                        if (sentKeyNum<sigKeyNum ||
                                (sentKeyNum==sigKeyNum && sentKeyVersion<sigKeyVersion)) {
                            needUpdate=true;
                        }
                    }
                    
                    if (needUpdate) {
                        HBCIUtils.log("have to send data for "+keytype+" key",HBCIUtils.LOG_DEBUG);
                        String dstHeader=HBCIUtilsInternal.withCounter("SendPubKey",responseCounter++);
                        
                        // signierschlssel abgefragt
                        if (keytype.equals("S")) {
                            if ((reqsFound&1)!=0) {
                                addGlobRet("9010","Verarbeitung nicht mglich",null);
                                addSegRet(srcHeader,null,"9210","Signaturschlssel zweimal abgefragt",null);
                                return;
                            }
                            
                            reqsFound|=1;
                            
                            setData(dstHeader+".KeyName.keytype","S");
                            setData(dstHeader+".PubKey.usage","6");
                        } else { // chiffrierschlssel abgefragt
                            if ((reqsFound&2)!=0) {
                                addGlobRet("9010","Verarbeitung nicht mglich",null);
                                addSegRet(srcHeader,null,"9210","Chiffrierschlssel zweimal abgefragt",null);
                                return;
                            }
                            
                            reqsFound|=2;
                            
                            setData(dstHeader+".KeyName.keytype","V");
                            setData(dstHeader+".PubKey.usage","5");
                        }
                        
                        addGlobRet("3060","Teilweise liegen Warnungen oder Hinweise vor",null);
                        addSegRet(srcHeader,null,"3050","Schlssel nicht mehr aktuell - neue Schlsseldaten werden bermittelt",null);
                        
                        setData(dstHeader+".SegHead.ref",getData(srcHeader+".SegHead.seq"));
                        setData(dstHeader+".dialogid",dialog.getDialogId());
                        setData(dstHeader+".msgnum",dialog.getMsgNum());
                        
                        HBCIUtils.log("adding key data",HBCIUtils.LOG_DEBUG);
                        setData(dstHeader+".KeyName.KIK.country",getServerData().getCountry());
                        setData(dstHeader+".KeyName.KIK.blz",getServerData().getBLZ());
                        setData(dstHeader+".KeyName.userid",getServerData().getBLZ());
                        setData(dstHeader+".KeyName.keynum",instKey.num);
                        setData(dstHeader+".KeyName.keyversion",instKey.version);
                        
                        setData(dstHeader+".PubKey.mode","16");
                        
                        try {
                            byte[] data=((RSAPublicKey)instKey.key).getModulus().toByteArray();
                            setData(dstHeader+".PubKey.modulus","B"+new String(checkFor96Bytes(data),"ISO-8859-1"));
                            
                            data=((RSAPublicKey)instKey.key).getPublicExponent().toByteArray();
                            setData(dstHeader+".PubKey.exponent","B"+new String(data,"ISO-8859-1"));
                        } catch (Exception e) {
                            throw new HBCI_Exception(e.getMessage(),e);
                        }
                    } else {
                        HBCIUtils.log("data for key "+keytype+" does not need to be sent",HBCIUtils.LOG_DEBUG);
                        addSegRet(srcHeader,null,"0020","angegebener Schlssel ist noch aktuell",null);
                    }
                } else { // kein keyrequest gefunden
                    /* das kann nur bei nicht-firstkeyreq-msgs passieren, weil nur bei
                       diesen in der spez die request-segmente optional sind*/
                    HBCIUtils.log("optional keyrequest #"+(i+1)+" not found",HBCIUtils.LOG_DEBUG);
                }
            }
        }
    }

    private byte[] checkFor96Bytes(byte[] buffer)
    {
        byte[] result=buffer;
        
        if (buffer.length!=96) {
            if (buffer.length>96) {
                int diff=buffer.length-96;
                boolean ok=true;

                for (int i=0;i<diff;i++) {
                    if (buffer[i]!=0x00) {
                        ok=false;
                    }
                }

                if (ok) {
                    result=new byte[96];
                    System.arraycopy(buffer,diff,result,0,96);
                }
            } else if (buffer.length<96) {
                int diff=96-buffer.length;
                result=new byte[96];
                Arrays.fill(result,(byte)0);
                System.arraycopy(buffer,0,result,diff,buffer.length);
            }
        }
        
        return result;
    }
    
    protected void postProcess()
    {
        super.postProcess();
        
        if (globRetIsOk()) {
            dialog.setFlag("forceEnd",true);
            dialog.setFlag("endInSig",false);
            dialog.setFlag("endInCrypt",false);
            dialog.setFlag("endOutSig",false);
            dialog.setFlag("endOutCrypt",false);
        }
    }
}
