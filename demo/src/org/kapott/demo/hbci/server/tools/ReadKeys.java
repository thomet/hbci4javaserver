
/* $Id: ReadKeys.java,v 1.1 2005/01/16 14:18:36 kleiner Exp $

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import org.kapott.hbci.manager.HBCIKey;


public class ReadKeys
{
    // Schlsseldaten aus Datei einlesen (wird ber Objektserialisierung realisiert)
    private static HBCIKey[] getXKeys(String filename)
    {
        // System.out.println("reading "+prefix+" keys");
        HBCIKey[] ret=null;
        
        try {
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(filename));
            ret=(HBCIKey[])in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println("  no "+filename+"_keys found");
        }
        
        return ret;
    }


    public static void main(String[] args)
        throws Exception
    {
        System.out.println("reading keys from "+args[0]);
        
        HBCIKey[] keys=getXKeys(args[0]);
        for (int i=0;i<keys.length;i++) {
            HBCIKey    key=keys[i];
            Properties props=new Properties();
            
            props.setProperty("blz",key.blz);
            props.setProperty("country",key.country);
            props.setProperty("num",key.num);
            props.setProperty("userid",key.userid);
            props.setProperty("version",key.version);

            FileOutputStream out;
            
            if (key.key instanceof RSAPrivateKey) {
                System.out.println("  found private key");
                
                props.setProperty("type","private");
                props.setProperty("privexponent",((RSAPrivateKey)key.key).getPrivateExponent().toString());
                props.setProperty("modulus",((RSAPrivateKey)key.key).getModulus().toString());

                out=new FileOutputStream(args[0]+".priv.txt");
            } else {
                System.out.println("  found public key");
                
                props.setProperty("type","public");
                props.setProperty("pubexponent",((RSAPublicKey)key.key).getPublicExponent().toString());
                props.setProperty("modulus",((RSAPublicKey)key.key).getModulus().toString());

                out=new FileOutputStream(args[0]+".pub.txt");
            }
            
            props.store(out,null);
            out.close();
        }
    }
}
