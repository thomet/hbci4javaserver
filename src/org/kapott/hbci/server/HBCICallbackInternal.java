
/*  $Id: HBCICallbackInternal.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2005 Stefan Palme

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

package org.kapott.hbci.server;

import java.util.Date;

import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassport;

public class HBCICallbackInternal 
    extends HBCICallbackConsole 
{
    public void callback(HBCIPassport passport,int reason,String msg,int datatype,StringBuffer retData)
    {
        HBCIUtils.log("there occured a callback, which should never happen",HBCIUtils.LOG_WARN);
        super.callback(passport,reason,msg,datatype,retData);
    }
    
    public void log(String msg,int level,Date date,StackTraceElement trace)
    {
        ServerData.getInstance().log(msg,level,date,trace);
    }
}
