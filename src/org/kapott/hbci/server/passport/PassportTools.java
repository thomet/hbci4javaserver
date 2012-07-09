
/*  $Id: PassportTools.java,v 1.3 2005/06/10 18:03:03 kleiner Exp $

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.server.ServerData;

public class PassportTools
{
    private static PassportTools _instance;
    
    public synchronized static PassportTools getInstance()
    {
        if (_instance==null) {
            _instance=new PassportTools();
        }
        return _instance;
    }
    
    private PassportTools()
    {
    }
    
    public String calculateSysId(String userid,String sysid)
    {
        String   ret;
        
        HBCIUtils.log("have to create passport with sysid "+sysid,HBCIUtils.LOG_DEBUG);
        
        String[] sysids_a=ServerData.getInstance().getSysIds(userid);
        List     sysids=new ArrayList(Arrays.asList(sysids_a));
        
        if (sysid.equals("0") || sysids.contains(sysid)) {
            HBCIUtils.log("this sysid is ok",HBCIUtils.LOG_DEBUG);
            ret=sysid;
        } else {
            // TODO: was soll das?
            if (sysids_a.length!=0) {
                ret=sysids_a[0];
            } else {
                ret="0";
            }
            HBCIUtils.log("this sysid is not valid - using sysid "+ret+" instead",HBCIUtils.LOG_DEBUG);
        }
        
        return ret;
    }
}
