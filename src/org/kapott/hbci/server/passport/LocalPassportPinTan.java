
/*  $Id: LocalPassportPinTan.java,v 1.6 2005/06/10 18:03:03 kleiner Exp $

    This file is part of hbci4java-server
    Copyright (C) 2001-2005  Stefan Palme

    hbci4java-server is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    hbci4java-server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.server.passport;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.passport.HBCIPassportPinTan;
import org.kapott.hbci.server.Dialog;
import org.kapott.hbci.server.ServerData;

public class LocalPassportPinTan 
    extends HBCIPassportPinTan
{
    private String    pin;
    private Hashtable tans;
    
    // for decryption and for signing messages
    public LocalPassportPinTan()
    {
        super("local pintan server passport",0);
        HBCIUtils.log("creating anonymous local pintan passport",HBCIUtils.LOG_INFO);
        
        ServerData sd=ServerData.getInstance();

        setCountry(sd.getCountry());
        setBLZ(sd.getBLZ());
    }
    
    // for encryption end for verifying signatures
    public LocalPassportPinTan(Dialog dialog)
    {
        super("pintan passport for "+dialog.getUserId(),0);
        
        String userid=dialog.getUserId();
        HBCIUtils.log("creating local pintan passport for userid "+userid,HBCIUtils.LOG_INFO);
        
        ServerData sd=ServerData.getInstance();
        
        setCountry(sd.getCountry());
        setBLZ(sd.getBLZ());
        setUserId(userid);
        setSysId(PassportTools.getInstance().calculateSysId(userid,dialog.getSysId()));
        // bpd wird bentigt, um zu ermitteln, ob ein gv eine tan bentigt
        setBPD(dialog.getBPD()); 
        
        this.pin=sd.getUserPIN(userid);
        this.tans=sd.getUserTANList(userid);
    }
    
    public void saveChanges()
    {
    }
    
    public byte[] sign(byte[] data)
    {
        return new byte[0];
    }
    
    public boolean verify(byte[] data,byte[] sig)
    {
        try {
            String sig_s=new String(sig,"ISO-8859-1");
            int    idx=sig_s.indexOf("|");
            String pin=sig_s.substring(0,idx);
            String tan=sig_s.substring(idx+1);
            
            HBCIUtils.log("verifying signature pin="+pin+" and tan="+tan,
                    HBCIUtils.LOG_DEBUG);
            
            // TODO die fehlermeldungen mssen an den client durchgegeben werden
            boolean ret=pin.equals(this.pin);
            
            if (ret) {
                String          segcodes=new String(data);
                StringTokenizer tok=new StringTokenizer(segcodes,"|");

                while (tok.hasMoreTokens()) {
                    String code=tok.nextToken();
                    String info=getPinTanInfo(code);

                    if (info.equals("J")) {
                        HBCIUtils.log(HBCIUtilsInternal.getLocMsg("INFO_PT_NEEDTAN",code),HBCIUtils.LOG_DEBUG);
                        String available=(String)tans.get(tan);
                        
                        if (available==null) {
                            HBCIUtils.log("TAN "+tan+" not in current TAN list",HBCIUtils.LOG_INFO);
                            ret=false;
                            break;
                        }
                        if (available.equals("0")) {
                            HBCIUtils.log("TAN "+tan+" already used",HBCIUtils.LOG_INFO);
                            ret=false;
                            break;
                        } 
                        
                        HBCIUtils.log("TAN "+tan+" is OK",HBCIUtils.LOG_DEBUG);
                        removeTAN(tan);
                    } else if (info.equals("N")) {
                        HBCIUtils.log(HBCIUtilsInternal.getLocMsg("INFO_PT_NOTNEEDED",code),HBCIUtils.LOG_DEBUG);
                    } else if (info.length()==0) {
                        HBCIUtils.log(HBCIUtilsInternal.getLocMsg("WRN_PT_CODENOTFOUND",code),HBCIUtils.LOG_WARN);
                    }
                }
            } else {
                HBCIUtils.log("PIN ist falsch",HBCIUtils.LOG_INFO);
            }
            
            return ret;
        } catch (UnsupportedEncodingException e) {
            throw new HBCI_Exception(e);
        }
    }
    
    private void removeTAN(String tan) 
    {
        tans.put(tan,"0");
        ServerData.getInstance().removeUserTAN(getUserId(),tan);
    }
}
