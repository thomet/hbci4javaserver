
/*  $Id: MsgHandlerDialogEndAnon.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;
import org.kapott.hbci.server.DialogMgr;

public class MsgHandlerDialogEndAnon 
    extends AbstractMsgHandler 
{
    public MsgHandlerDialogEndAnon(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    protected void initMessageHandler()
    {
        if (dialog.getFlag("forceEnd")) {
            dialog.setFlag("signResponse",dialog.getFlag("endOutSig"));
            dialog.setFlag("encryptResponse",dialog.getFlag("endOutCrypt"));
        } else {
            dialog.setFlag("signResponse",false);
            dialog.setFlag("encryptResponse",false);
        }
    }
    
    protected boolean isMessageAllowed()
    {
        return dialog.isInitialized();
    }

    protected void handleMessageContent()
    {
        // check dialogends
        if (!getData("DialogEndS.dialogid").equals(dialog.getDialogId())) {
            addGlobRet("9010","Verarbeitung nicht mglich",null);
            addSegRet("DialogEndS","2","9210","falsche Dialog-ID",null);
            return;
        }

        // now start creating response message
        addGlobRet("0100","Dialog beendet",null);
    }
    
    protected void postProcess()
    {
        if (globRetIsOk()) {
            DialogMgr.getInstance().removeDialog(dialog.getDialogId());
        }
    }
    
    public int updateMsgNum(int msgnum)
    {
        return globRetIsOk()?1:super.updateMsgNum(msgnum);
    }
}
