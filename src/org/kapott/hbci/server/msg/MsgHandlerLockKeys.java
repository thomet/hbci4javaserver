
/*  $Id: MsgHandlerLockKeys.java,v 1.4 2005/06/10 18:03:03 kleiner Exp $

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

import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;

public class MsgHandlerLockKeys 
    extends AbstractMsgHandler 
{
    public MsgHandlerLockKeys(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    protected void initMessageHandler()
    {
        dialog.setFlag("signResponse",true);
        dialog.setFlag("encryptResponse",false);
    }
    
    protected boolean isMessageAllowed()
    {
        return dialog.isInitialized() &&
        !dialog.isAnonymous() && 
        !dialog.getFlag("forceEnd");
    }
    
    protected void handleMessageContent()
    {
        // *** check locktype
        
        // *** check keyname
        String header="KeyLock.KeyName";
        if (!getData(header+".KIK.blz").equals(getServerData().getBLZ()) ||
                !getData(header+".KIK.country").equals(getServerData().getCountry())) {
            
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet(header,"4,1","9210","ungltige Bankkennung",null);
            return;
        }
        
        // userid in keyname verifizieren
        if (!getData(header+".userid").equals(dialog.getUserId())) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet(header,"4,2","9210","User-ID falsch",null);
            return;
        }
        
        // keytype berprfen
        if (!getData(header+".keytype").equals("S")) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet(header,"4,3","9210","Falscher Schlssel - es muss der Signierschlssel angegeben werden",null);
            return;
        }
        
        // prfen, ob schlssel-nummer/-version passt
        HBCIKey key=getServerData().getUserRDHSigKey(dialog.getUserId());
        if (!getData(header+".keynum").equals(key.num) ||
                !getData(header+".keyversion").equals(key.version)) {

            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet(header,null,"9210","Falsche Schlsselnummer/-version",null);
            return;
        }

        // *** check timestamp
        
        getServerData().setUserRDHSigKey(dialog.getUserId(),null);
        getServerData().setUserRDHEncKey(dialog.getUserId(),null);
        getServerData().clearSigIds(dialog.getUserId(),dialog.getSysId());
        
        addGlobRet("0020","Ausgefhrt",null);
        addSegRet("KeyLock",null,"0020","Schlssel gesperrt",null);
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
