
/*  $Id: HBCISpec.java,v 1.2 2005/06/10 18:03:02 kleiner Exp $

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

package org.kapott.hbci.server;

import org.kapott.hbci.exceptions.HBCI_Exception;

public class HBCISpec 
{
    public static String mapISO2HBCILang(String lang)
    {
        String ret="";
        String data=lang.toUpperCase();
        
        if (data.equals("DE"))
            ret="1";
        else if (data.equals("EN"))
            ret="2";
        else if (data.equals("FR"))
            ret="3";
        else 
            throw new HBCI_Exception("invalid language code: "+lang);
        
        return ret;
    }
}

