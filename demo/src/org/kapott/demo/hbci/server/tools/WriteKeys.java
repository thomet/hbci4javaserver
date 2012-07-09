
/* $Id: WriteKeys.java,v 1.1 2005/01/16 14:18:36 kleiner Exp $

   This file is part of org.kapott.demo.hbci.server.tools
   Copyright (C) 2005  Stefan Palme

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.demo.hbci.server.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Properties;

import org.kapott.hbci.manager.HBCIKey;


public class WriteKeys
{
    public static void main(String[] args)
        throws Exception
    {
        System.out.println("processing "+args[0]);
        
        HBCIKey pubkey=null;
        HBCIKey privkey=null;
        
        try {
            FileInputStream in=new FileInputStream(args[0]+".pub.txt");
            Properties props=new Properties();
            props.load(in);
            in.close();
            
            BigInteger mod=new BigInteger(props.getProperty("modulus"));
            BigInteger exp=new BigInteger(props.getProperty("pubexponent"));
            RSAPublicKeySpec spec=new RSAPublicKeySpec(mod,exp);
            KeyFactory fac=KeyFactory.getInstance("RSA");
            Key key=fac.generatePublic(spec);
            
            pubkey=new HBCIKey(
                props.getProperty("country"),
                props.getProperty("blz"),
                props.getProperty("userid"),
                props.getProperty("num"),
                props.getProperty("version"),
                key);
        } catch (Exception e) {
            System.out.println("  no public key found");
        }
        
        try {
            FileInputStream in=new FileInputStream(args[0]+".priv.txt");
            Properties props=new Properties();
            props.load(in);
            in.close();
            
            BigInteger mod=new BigInteger(props.getProperty("modulus"));
            BigInteger exp=new BigInteger(props.getProperty("privexponent"));
            RSAPrivateKeySpec spec=new RSAPrivateKeySpec(mod,exp);
            KeyFactory fac=KeyFactory.getInstance("RSA");
            Key key=fac.generatePrivate(spec);
            
            privkey=new HBCIKey(
                props.getProperty("country"),
                props.getProperty("blz"),
                props.getProperty("userid"),
                props.getProperty("num"),
                props.getProperty("version"),
                key);
        } catch (Exception e) {
            System.out.println("  no private key found");
        }
        
        HBCIKey[] keys=new HBCIKey[privkey!=null?2:1];
        
        keys[0]=pubkey;
        if (privkey!=null) {
            keys[1]=privkey;
        }
        
        FileOutputStream out=new FileOutputStream(args[0]);
        ObjectOutputStream oout=new ObjectOutputStream(out);
        oout.writeObject(keys);
        oout.close();
    }
}
