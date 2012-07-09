
/*  $Id: ServerAdminImpl.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;

import org.kapott.demo.hbci.server.backend.MyBackend;
import org.kapott.hbci.server.HBCIServer;
import org.kapott.hbci.structures.Konto;

public class ServerAdminImpl
    extends UnicastRemoteObject
    implements ServerAdmin 
{
    private HBCIServer  server;
    private MyDataStore dataStore;
    private MyBackend   backend;
    
    public String getBLZ()
    {
        return dataStore.getBLZ();
    }
    
    public ServerAdminImpl(HBCIServer server,MyDataStore dataStore,MyBackend backend)
        throws RemoteException
    {
        this.server=server;
        this.dataStore=dataStore;
        this.backend=backend;
    }
    
    public boolean verify(String userid,String passwd)
    {
        return dataStore.getSingleLine(userid+"_passphrase").equals(passwd);
    }
    
    public void setPassphrase(String userid,String passwd)
    {
        dataStore.storeSingleLine(passwd,userid+"_passphrase");
    }
    
    public String[] getSysIds(String userid)
    {
        return dataStore.getSysIds(userid);
    }
    
    public void addSysId(String userid,String sysid)
    {
        dataStore.addSysId(userid,sysid);
    }
    
    public void removeSysId(String userid,String sysid)
    {
        ArrayList sysids=new ArrayList(Arrays.asList(dataStore.getMultipleLines(userid+"_sysids")));
        sysids.remove(sysid);
        dataStore.storeMultipleLines((String[])sysids.toArray(new String[0]),userid+"_sysids");
    }

    public String getAccInfoVersion(String userid)
    {
        return Integer.toString(dataStore.getAccountInfoVersion(userid));
    }
    
    public void setAccInfoVersion(String userid,String version)
    {
        dataStore.storeSingleLine(version,userid+"_accinfoversion");
    }
    
    public String[] getCustomerIds(String userid)
    {
        return dataStore.getCustomerIds(userid);
    }
    
    public void setCustomerIds(String userid,String[] customerids)
    {
        dataStore.storeMultipleLines(customerids,userid+"_customerids");
    }
    
    public boolean hasKeys(String userid)
    {
        return dataStore.getUserKeys(userid)!=null;
    }
    
    public void removeKeys(String userid)
    {
        dataStore.storeUserSigKey(userid,null);
        dataStore.storeUserEncKey(userid,null);
    }
    
    public Konto[] getAccounts(String userid)
    {
        return dataStore.getAccounts(userid);
    }
    
    public void setAccounts(String userid,Konto[] accounts)
    {
        String[] accdata=new String[accounts.length];
        for (int i=0;i<accounts.length;i++) {
            Konto acc=accounts[i];
            accdata[i]=acc.number+"|"+acc.type+"|"+acc.name+"|"+acc.customerid;
        }
        dataStore.storeMultipleLines(accdata,userid+"_accounts");
    }
    
    public void reloadUserData(String userid)
    {
        server.reInitializeUserData(userid);
    }
    
    public String[] getSigIds(String userid,String sysid)
    {
        long[]   sigids_l=dataStore.getSigIds(userid,sysid);
        int      len=sigids_l.length;
        String[] sigids_s=new String[len];
        
        for (int i=0;i<len;i++) {
            sigids_s[i]=Long.toString(sigids_l[i]);
        }
        
        return sigids_s;
    }
    
    public void setSigIds(String userid,String sysid,String[] sigids_s)
    {
        dataStore.clearSigIds(userid,sysid);
        int len=sigids_s.length;
        for (int i=0;i<len;i++) {
            dataStore.addSigId(userid,sysid,Long.parseLong(sigids_s[i]));
        }
    }
    
    public String getUserPIN(String userid)
    {
        return dataStore.getUserPIN(userid);
    }
    
    public void setUserPIN(String userid,String pin)
    {
        dataStore.setUserPIN(userid,pin);
    }
    
    public String[] getUserTANList(String userid)
    {
        return dataStore.getUserTANList(userid);
    }
    
    public void setUserTANList(String userid,String[] tans)
    {
        dataStore.setUserTANList(userid,tans);
    }
}
