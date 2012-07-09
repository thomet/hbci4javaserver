
/*  $Id: MsgHandlerDialogInit.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerDialogInit 
    extends MsgHandlerFirstKeyReq
{
    public MsgHandlerDialogInit(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }

    protected void initMessageHandler()
    {
        dialog.setFlag("signResponse",true);
        dialog.setFlag("encryptResponse",true);
    }
    
    protected boolean isMessageAllowed()
    {
        return !dialog.isInitialized();
    }
    
    protected void handleMessageContent()
    {
        super.handleMessageContent();
        
        if (globRetIsOk()) {
            // customer-id berprfen
            String customerId=getData("Idn.customerid");
            if (!(new ArrayList(Arrays.asList(getServerData().getCustomerIds(dialog.getUserId()))).contains(customerId))) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("Idn","3","9210","ungltige Kunden-ID",null);
                return;
            }
            dialog.setCustomerId(customerId);
        
            // sysid berprfen
            if (!getData("Idn.sysid").equals(dialog.getSysId())) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("Idn","4","9210","falsche System-ID",null);
                return;
            }
            
            // sysstatus berprfen
            if (!getData("Idn.sysStatus").equals("1")) {
                addGlobRet("9010","Verarbeitung nicht mglich",null);
                addSegRet("Idn","5","9210","Systemstatus falsch gesetzt",null);
                return;
            }
        }
    }
    
    protected void postProcess()
    {
        if (globRetIsOk()) {
            dialog.setInitialized(true);
            dialog.setAnonymous(false);
            dialog.setFlag("forceEnd",false);
        }
    }
}
