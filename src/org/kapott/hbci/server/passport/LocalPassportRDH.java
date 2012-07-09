
/*  $Id: LocalPassportRDH.java,v 1.3 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.passport;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportRDH;
import org.kapott.hbci.server.Dialog;
import org.kapott.hbci.server.ServerData;

public class LocalPassportRDH 
    extends HBCIPassportRDH 
{
    // for decryption and for signing messages
    public LocalPassportRDH()
    {
        super("local rdh server passport",0);
        HBCIUtils.log("creating anonymous local rdh passport",HBCIUtils.LOG_INFO);
        
        ServerData sd=ServerData.getInstance();

        setCountry(sd.getCountry());
        setBLZ(sd.getBLZ());
        setUserId(getBLZ());
        setSigId(sd.getSigId());
        setMyPublicSigKey(sd.getInstPublicRDHSigKey());
        setMyPrivateSigKey(sd.getInstPrivateRDHSigKey());
        setMyPublicEncKey(sd.getInstPublicRDHEncKey());
        setMyPrivateEncKey(sd.getInstPrivateRDHEncKey());
    }
    
    // for encryption end for verifying signatures
    public LocalPassportRDH(Dialog dialog)
    {
        super("rdh passport for "+dialog.getUserId(),0);
        
        String userid=dialog.getUserId();
        HBCIUtils.log("creating local rdh passport for userid "+userid,HBCIUtils.LOG_INFO);
        
        ServerData sd=ServerData.getInstance();
        
        setBLZ(sd.getBLZ());
        setCountry(sd.getCountry());
        setUserId(userid); // TODO: warum?
        setSysId(PassportTools.getInstance().calculateSysId(userid,dialog.getSysId()));
        setSigId(sd.getSigId());
        setMyPublicSigKey(sd.getInstPublicRDHSigKey());
        setMyPrivateSigKey(sd.getInstPrivateRDHSigKey());
        setMyPublicEncKey(sd.getInstPublicRDHEncKey());
        setMyPrivateEncKey(sd.getInstPrivateRDHEncKey());
        setInstSigKey(sd.getUserRDHSigKey(userid));
        setInstEncKey(sd.getUserRDHEncKey(userid));
    }
    
    public void saveChanges()
    {
        ServerData.getInstance().setSigId(getSigId());
    }
}
