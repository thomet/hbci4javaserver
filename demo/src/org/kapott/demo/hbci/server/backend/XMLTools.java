
/*  $Id: XMLTools.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.demo.hbci.server.backend;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLTools 
{
    private DocumentBuilder docBuilder; 
    private Transformer     trans;

    public XMLTools()
    {
        try {
            // document builder fr xml dokumente initialisieren
            DocumentBuilderFactory fac=DocumentBuilderFactory.newInstance();
            fac.setIgnoringComments(true);
            fac.setValidating(false);
            docBuilder=fac.newDocumentBuilder();
            
            // transformer zum abspeichern von xml dokumenten initialisieren
            TransformerFactory tfac=TransformerFactory.newInstance();
            trans=tfac.newTransformer();
            trans.setOutputProperty("indent","yes");
            trans.setOutputProperty("{http://xml.apache.org/xalan}indent-amount","2");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    // evtl. nicht vorhandene Datendatei anlegen
    public Document getFileContent(File file,String basename)
    {
        try {
            Document doc;
            
            if (!file.exists()) {
                doc=docBuilder.newDocument();
                doc.appendChild(doc.createElement(basename));
                trans.transform(new DOMSource(doc),new StreamResult(file));
            } else {
                doc=docBuilder.parse(file);
            }
            
            return doc;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    private Element getChildElement(Element parent,String name)
    {
        return (Element)parent.getElementsByTagName(name).item(0);
    }
    
    private String getElementValue(Element element)
    {
        Node node=element.getFirstChild();
        return node!=null?node.getNodeValue():null;
    }
    
    // Ein XML-Element mit Inhalt erzeugen
    // <name>value</name>
    public Element createElement(Document doc,String name,String value)
    {
        Element e=doc.createElement(name);
        if (value!=null)
            e.appendChild(doc.createTextNode(value));
        return e;
    }
    
    public String readElement(Element elem,String name,String def)
    {
        String  ret=def;
        Element child=getChildElement(elem,name);
        if (child!=null) {
            String st=getElementValue(child);
            if (st!=null && st.length()!=0)
                ret=st;
        }
        return ret;
    }
    
    // ein Zeitstempelelement erzeugen
    public Element createDateTimeElement(Document doc,String name,Date date)
    {
        return createElement(doc,name,(date!=null)?(new SimpleDateFormat("yyyyMMddHHmmss").format(date)):null);
    }
    
    public Date readDateTimeElement(Element elem,String name,Date def)
    {
        try {
            Date   ret=def;
            String st=readElement(elem,name,null);
            if (st!=null)
                ret=new SimpleDateFormat("yyyyMMddHHmmss").parse(st);
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    // ein Zeitstempelelement erzeugen
    // <timestamp>zeit</timestamp>
    public Element createTimestampElement(Document doc)
    {
        return createDateTimeElement(doc,"timestamp",new Date());
    }
    
    // ein konto-element erzeugen
    // <name>
    //   <country>acc.country</country> usw.
    public Element createAccountElement(Document doc,String name,Konto acc)
    {
        Element ret=doc.createElement(name);
        
        if (acc!=null) {
            if (acc.country!=null)
                ret.appendChild(createElement(doc,"country",acc.country));
            if (acc.blz!=null)
                ret.appendChild(createElement(doc,"blz",acc.blz));
            if (acc.number!=null)
                ret.appendChild(createElement(doc,"number",acc.number));
            if (acc.name!=null)
                ret.appendChild(createElement(doc,"name",acc.name));
            if (acc.name2!=null)
                ret.appendChild(createElement(doc,"name2",acc.name2));
        }
        
        return ret;
    }
    
    public Konto readAccountElement(Element elem,String name,Konto def)
    {
        Konto ret=def;
        
        Element accelem=getChildElement(elem,name);
        if (accelem!=null) {
            Konto acc=new Konto();
            acc.country=readElement(accelem,"country","DE");
            acc.blz=readElement(accelem,"blz","");
            acc.number=readElement(accelem,"number","");
            acc.name=readElement(accelem,"name","");
            acc.name2=readElement(accelem,"name2",null);
            
            if (acc.number!=null && acc.blz!=null)
                ret=acc;
        }
        return ret;
    }
    
    // ein value-element erzeugen
    // <name>
    //   <value>btg.value</value> usw.
    public Element createValueElement(Document doc,String name,Value btg)
    {
        Element ret=doc.createElement(name);
        
        if (btg!=null) {
            ret.appendChild(createElement(doc,"value",Long.toString(Math.round(btg.value*10000))));
            ret.appendChild(createElement(doc,"curr",btg.curr));
        }
        
        return ret;
    }
    
    public Value readValueElement(Element elem,String name,Value def)
    {
        Value ret=def;
        
        Element elem_value=getChildElement(elem,name);
        if (elem_value!=null) {
            Value val=new Value();
            val.value=Double.parseDouble(readElement(elem_value,"value","0"))/10000.0;
            val.curr=readElement(elem_value,"curr",null);

            if (val.curr!=null)
                ret=val;
        }
        
        return ret;
    }
    
    // ein string[]-element erzeugen
    // <name>
    //   <value>data[0]</value> usw.
    public Element createStringArrayElement(Document doc,String name,String[] data)
    {
        Element ret=doc.createElement(name);
        
        if (data!=null) {
            for (int i=0;i<data.length;i++) {
                String st=data[i];
                ret.appendChild(createElement(doc,"value",st));
            }
        }
        
        return ret;
    }
    
    public String[] readStringArrayElement(Element elem,String name,String[] def)
    {
        String[] ret=def;
        
        Element child=getChildElement(elem,name);
        if (child!=null) {
            ArrayList lines=new ArrayList();
            NodeList values=child.getElementsByTagName("value");
            int len=values.getLength();
            
            for (int i=0;i<len;i++) {
                lines.add(getElementValue((Element)values.item(i)));
            }
            
            if (len!=0)
                ret=(String[])lines.toArray(new String[0]);
        }
        return ret;
    }
    
    public Element createUmsLineElement(Document doc,String name,GVRKUms.UmsLine line,String id)
    {
        Element ret=doc.createElement(name);
        
        if (line!=null) {
            ret.appendChild(createElement(doc,"id",id));
            
            ret.appendChild(createDateTimeElement(doc,"bdate",line.bdate));
            ret.appendChild(createDateTimeElement(doc,"valuta",line.valuta));
            ret.appendChild(createAccountElement(doc,"other",line.other));
            
            Value btg=new Value(line.value.value,line.value.curr);
            if (line.cd.equals("D"))
                btg.value=-btg.value;
            ret.appendChild(createValueElement(doc,"value",btg));
            ret.appendChild(createValueElement(doc,"charge",line.charge_value));
            ret.appendChild(createValueElement(doc,"orig",line.orig_value));
            
            btg=new Value(line.saldo.value.value,line.saldo.value.curr);
            if (line.saldo.cd.equals("D"))
                btg.value=-btg.value;
            ret.appendChild(createValueElement(doc,"saldo",btg));
            
            ret.appendChild(createStringArrayElement(doc,"usage",line.usage));
            ret.appendChild(createElement(doc,"primanota",line.primanota));
            ret.appendChild(createElement(doc,"gvcode",line.gvcode));
            ret.appendChild(createElement(doc,"addkey",line.addkey));
            ret.appendChild(createElement(doc,"instref",line.instref));
            ret.appendChild(createElement(doc,"customerref",line.customerref));
            ret.appendChild(createElement(doc,"additional",line.additional));
        }
        
        return ret;
    }
    
    public void transform(Document doc,File file)
    {
        try {
            trans.transform(new DOMSource(doc),new StreamResult(file));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    public String key2Text(String gvcode)
    {
        int    code=Integer.parseInt(gvcode);
        String ret;
        
        // *** mehr gvcodes->text implementieren
        switch (code) {
            case 1:
                ret="Inhaberscheck"; break;
            case 2:
                ret="Orderscheck"; break;
            case 4:
                ret="Lastschrift (Abbuchung)"; break;
            case 5:
                ret="Lastschrift (Einzug)"; break;
            case 8:
                ret="Dauerauftrag"; break;
            case 17:
                ret="BZ-berweisung"; break;
            case 20:
                ret="berweisung"; break;
            case 51:
                ret="berweisungsgutschrift"; break;
            case 52:
                ret="Dauerauftragsgutschrift"; break;
            case 53:
                ret="Lohn/Gehalt/Rentengutschrift"; break;
            case 67:
                ret="BZ-Gutschrift"; break;
            case 71:
                ret="Lastschrifteinreichung"; break;
            default:
                ret=gvcode;
        }
        
        return ret;
    }
    
    public Document newDoc()
    {
        return docBuilder.newDocument();
    }
}
