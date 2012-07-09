
/*  $Id: MsgHandlerDialogInitAnon.java,v 1.5 2005/06/10 18:03:03 kleiner Exp $

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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerDialogInitAnon 
    extends AbstractMsgHandler 
{
    public MsgHandlerDialogInitAnon(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    protected void initMessageHandler()
    {
        dialog.setFlag("signResponse",false);
        dialog.setFlag("encryptResponse",false);
    }
    
    protected boolean isMessageAllowed()
    {
        return !dialog.isInitialized() && getServerData().isAnonAllowed();
    }
    
    protected void handleMessageContent()
    {
        // check idn
        // kik ok
        if (!getData("Idn.KIK.country").equals(getServerData().getCountry()) ||
                !getData("Idn.KIK.blz").equals(getServerData().getBLZ())) {
            
            addGlobRet("9050","Teilweise fehlerhaft",null);
            addSegRet("Idn","2","9210","ungltige Bankkennung",null);
            return;
        }

        // procprep
        //   *** language ok (choose if default)
        
        // now start creating response message
        addGlobRet("0020","Dialoginitialisierung erfolgreich",null);
        
        // check for bpd
        if (Integer.parseInt(getData("ProcPrep.BPD")) <
                Integer.parseInt(dialog.getBPD().getProperty("BPA.version"))) {
            HBCIUtils.log("adding newer BPD to response message",HBCIUtils.LOG_DEBUG);

            // add segref headers to all segments
            String segref=getData("ProcPrep.SegHead.seq");
            setData("BPD.BPA.SegHead.ref",segref);
            setData("BPD.CommListRes.SegHead.ref",segref);
            setData("BPD.SecMethod.SegHead.ref",segref);
            setData("BPD.CompMethod.SegHead.ref",segref);
            
            // create segrefs for all gv param segments
            Properties bpd=dialog.getBPD();
            for (Enumeration e=bpd.keys();e.hasMoreElements();) {
                String key=(String)e.nextElement();
                if (key.startsWith("Params") && key.endsWith(".maxnum")) {
                    int firstDot=key.indexOf('.');
                    int secondDot=key.indexOf('.',firstDot+1);
                    String segname=key.substring(0,secondDot);
                    setData("BPD."+segname+".SegHead.ref",segref);
                }
            }

            // fill bpd response
            for (Iterator i=bpd.entrySet().iterator();i.hasNext();) {
                Map.Entry entry=(Map.Entry)i.next();
                setData("BPD."+entry.getKey(),(String)entry.getValue());
            }
            
            addGlobRet("3060","Teilweise liegen Warnungen oder Hinweise vor",null);
            addSegRet("ProcPrep",null,"3050","BPD nicht mehr aktuell - neue Version folgt",null);
        }
        
        // add UPD for current userid
        Properties upd=getServerData().getUPD(dialog.getUserId());
        if (Integer.parseInt(getData("ProcPrep.UPD")) <
                Integer.parseInt(upd.getProperty("UPA.version"))) {
            HBCIUtils.log("adding newer UPD to response message",HBCIUtils.LOG_DEBUG);

            // add segref headers to all segments
            String segref=getData("ProcPrep.SegHead.seq");
            setData("UPD.UPA.SegHead.ref",segref);
            for (int i=0;;i++) {
                String header=HBCIUtilsInternal.withCounter("KInfo",i);
                if (upd.getProperty(header+".KTV.number")==null)
                    break;
                setData("UPD."+header+".SegHead.ref",segref);
            }

            // fill UPD response
            for (Iterator i=upd.entrySet().iterator();i.hasNext();) {
                Map.Entry entry=(Map.Entry)i.next();
                setData("UPD."+entry.getKey(),(String)entry.getValue());
            }
            
            addGlobRet("3060","Teilweise liegen Warnungen oder Hinweise vor",null);
            addSegRet("ProcPrep",null,"3050","UPD nicht mehr aktuell - neue Version folgt",null);
        }
        
        // *** add messages for current userid
    }
    
    protected void postProcess()
    {
        if (globRetIsOk()) {
            dialog.setInitialized(true);
            dialog.setAnonymous(true);
            dialog.setFlag("forceEnd",false);
        }
    }
}
