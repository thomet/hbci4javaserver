
/*  $Id: ServerAdmin.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.demo.hbci.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.kapott.hbci.structures.Konto;

public interface ServerAdmin 
    extends Remote 
{
    public String getBLZ()
        throws RemoteException;
    
    public boolean verify(String userid,String passwd)
        throws RemoteException;
    public void setPassphrase(String userid,String passwd)
        throws RemoteException;
    
    public String[] getSysIds(String userid)
        throws RemoteException;
    public void addSysId(String userid,String sysid)
        throws RemoteException;
    public void removeSysId(String userid,String sysid)
        throws RemoteException;
    
    public String getAccInfoVersion(String userid)
        throws RemoteException;
    public void setAccInfoVersion(String userid,String version)
        throws RemoteException;
    
    public String[] getCustomerIds(String userid)
        throws RemoteException;
    public void setCustomerIds(String userid,String[] customerids)
        throws RemoteException;
    
    public boolean hasKeys(String userid)
        throws RemoteException;
    public void removeKeys(String userid)
        throws RemoteException;
    
    public Konto[] getAccounts(String userid)
        throws RemoteException;
    public void setAccounts(String userid,Konto[] accounts)
        throws RemoteException;
    
    public void reloadUserData(String userid)
        throws RemoteException;
    
    public String[] getSigIds(String userid,String sysid)
        throws RemoteException;
    public void setSigIds(String userid,String sysid,String[] sigids)
        throws RemoteException;
    
    public String getUserPIN(String userid)
        throws RemoteException;
    public void setUserPIN(String userid,String tan)
        throws RemoteException;
    
    public String[] getUserTANList(String userid)
        throws RemoteException;
    public void setUserTANList(String userid,String[] tans)
        throws RemoteException;
}
