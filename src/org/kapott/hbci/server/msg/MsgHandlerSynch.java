
/*  $Id: MsgHandlerSynch.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerSynch 
    extends MsgHandlerDialogInit 
{
    public MsgHandlerSynch(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    // fr sync-messages gelten andere regeln fr die system-id
    protected boolean verifySysId(String sysid)
    {
        boolean ret;
        int     syncmode=Integer.parseInt(getData("Sync.mode"));
        
        if (syncmode==0) {
            // sync sysid, da muss die sysid=0 sein
            ret=sysid.equals("0");
            if (!ret) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("Sync",null,"9010","System-ID muss 0 sein",null);
            }
        } else if (syncmode==1) {
            // sync msgnum, da muss die normale berprfung vorgenommen werden
            ret=super.verifySysId(sysid);
        } else {
            // sync sigid, da kann die sysid 0 oder eine der schon vorhandenen
            // sein
            ret=(sysid.equals("0") || super.verifySysId(sysid));
        }
            
        return ret;
    }
    
    // alternative checks fr signatur-id bei sync-messages
    protected boolean verifySigId(long sigid)
    {
        boolean ret;
        int     syncmode=Integer.parseInt(getData("Sync.mode"));
        
        if (syncmode==0) {
            // sync sysid, da kann die sigid ignoriert werden
            ret=true;
        } else if (syncmode==1) {
            // sync msgnum, normale prfung
            ret=super.verifySigId(sigid);
        } else {
            // sync sigid, sigid muss hier speziellen wert haben
            ret=Long.toString(sigid).equals("9999999999999999");
            if (!ret) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("Sync",null,"9010","Signatur-ID fr Sync ungltig",null);
                return false;
            }
        }
        
        return ret;
    }
    
    protected void handleMessageContent()
    {
        super.handleMessageContent();
        
        // handle sync segment
        if (globRetIsOk()) {
            int syncmode=Integer.parseInt(getData("Sync.mode"));
            
            if (syncmode==0) {
                // sync sysid
                if (!getData("Idn.sysid").equals(dialog.getSysId())) {
                    addGlobRet("9010","Verarbeitung nicht mglich",null);
                    addSegRet("Idn",null,"9010","falsche System-ID",null);
                    return;
                }
                
                String sysid=Long.toString(getServerData().nextRandom()).substring(1);
                if (sysid.length()>30)
                    sysid=sysid.substring(0,30);
                HBCIUtils.log("created new sysid "+sysid,HBCIUtils.LOG_INFO);
                
                dialog.setSysId(sysid);
                getServerData().addSysId(dialog.getUserId(),sysid);
                
                // *** hier evtl. sigids irgendwie aufrumen (fr sigid-doppel-
                // einreichungskontrolle via userid/sysid)
                getServerData().clearSigIds(dialog.getUserId(),sysid);
                getServerData().addSigId(dialog.getUserId(),sysid,Long.parseLong(getData("SigHead.secref")));

                setData("SyncRes.SegHead.ref",getData("Sync.SegHead.seq"));
                setData("SyncRes.sysid",dialog.getSysId());
                
                addGlobRet("0020","Auftrag ausgefhrt",null);
                addSegRet("Sync",null,"0020","Ausgefhrt",null);
            } else if (syncmode==1) {
                // *** sync msgnum
            } else {
                // sync sigid
                String sigid=Long.toString(getServerData().getLastSigId(dialog.getUserId(),dialog.getSysId())+1);
                HBCIUtils.log("returning next valid sigid "+sigid,HBCIUtils.LOG_INFO);
                
                setData("SyncRes.SegHead.ref",getData("Sync.SegHead.seq"));
                setData("SyncRes.sigid",sigid);
                
                addGlobRet("0020","Auftrag ausgefhrt",null);
                addSegRet("Sync",null,"0020","Ausgefhrt",null);
            }
        }
    }
    
    protected void postProcess()
    {
        super.postProcess();
        
        if (globRetIsOk()) {
            dialog.setFlag("forceEnd",true);
            dialog.setFlag("endInSig",true);
            dialog.setFlag("endInCrypt",true);
            dialog.setFlag("endOutSig",true);
            dialog.setFlag("endOutCrypt",true);
        }
    }
}
