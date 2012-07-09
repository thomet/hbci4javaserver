
/*  $Id: AdminServlet.java,v 1.2 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.demo.hbci.server.admin;

import java.io.PrintWriter;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.kapott.demo.hbci.server.ServerAdmin;
import org.kapott.hbci.structures.Konto;

public class AdminServlet 
    extends HttpServlet 
{
    private String rmiAddr; // adresse der registry der hbciserver-objekte
    
    // adresse des rmi-servers aus den web.xml-daten extrahieren
    public void init()
    {
        try {
            super.init();
            
            // initialize rmi server address
            rmiAddr=getServletConfig().getInitParameter("rmiServer");
            System.out.println("rmiServer address set to "+rmiAddr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // servlet informationen
    public String getServletInfo()
    {
        return "Version 0.2.0 - Admin Console for managing HBCI4Java-Server user accounts (see http://hbci4java.kapott.org) - Copyright 2003-2004 by Stefan Palme";
    }
    
    // alle requests gehen ber POST ein 
    public void doPost(HttpServletRequest request,HttpServletResponse response)
    {
        try {
            // was wurde eigentlich requested?
            String path=request.getServletPath();
            
            // bei login session komplett frisch initialisieren
            if (path.equals("/login.html")) {
                handleUserLogin(request,response);
            } else { // alles auer login
                // aktuelle session holen
                HttpSession session=request.getSession(false);
                
                if (session==null) { // session ungltig?
                    response.sendRedirect("session_error.html");
                } else { // gltige session gefunden
                    if (path.equals("/commit.html")) {
                        handleCommitChanges(session,request,response);
                    } else if (path.equals("/logout.html")) {
                        handleLogout(session,request,response);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // user hat login-formular abgeschickt
    private void handleUserLogin(HttpServletRequest request,HttpServletResponse response)
    {
        try {
            // neue session initialisieren
            System.out.println("creating new session");
            HttpSession session=request.getSession(true);
            if (!session.isNew()) {
                System.out.println("have to remove old session first");
                session.invalidate();
                session=request.getSession(true);
            }
            System.out.println("this session is ok: "+session.isNew());
            
            // holen des objektes zur "fernsteuerung" des hbci-servers
            ServerAdmin admin=(ServerAdmin)Naming.lookup("//"+rmiAddr+"/serverAdmin");
            session.setAttribute("admin",admin);
            
            // userid und passwort aus formulardaten extrahieren
            String userid=request.getParameter("userid");
            String passphrase=request.getParameter("passwd");
            System.out.println("user "+userid+" tries to log in");
            
            // wenn userid/passwort ok sind
            if (admin.verify(userid,passphrase)) {
                System.out.println("user "+userid+" logged in");
                session.setAttribute("userid",userid);
                session.setAttribute("passphrase",passphrase);
                
                // ausgabestream erzeugen
                response.setContentType("text/html");
                PrintWriter out=response.getWriter();

                // html-kopf erzeugen
                printHeader(out,userid);
                
                // datentabelle schreiben
                printCurrentUserDataTable(session,response);
                // logout button erzeugen
                printLogoutButton(out);

                // footer erzeugen
                printFooter(out);
            } else { // fehlerhaftes login
                System.out.println("user "+userid+" not logged in");
                response.sendRedirect("index2.html");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void printCurrentUserDataTable(HttpSession session,HttpServletResponse response)
    {
        try {
            ServerAdmin admin=(ServerAdmin)session.getAttribute("admin");
            String userid=(String)session.getAttribute("userid");
            
            PrintWriter out=response.getWriter();
            int row=0; // wird fr einfrbung der tabelle benutzt
            
            // temporre objekte
            String st;
            String[] sta;
            String[] sta2;
            boolean b;
            
            // javascript fr sysid->sigid ansicht erzeugen
            out.println("<script type=\"text/javascript\">");
            out.println("<!--");
            out.println("var sigids=new Array();");
            
            Hashtable old_sigids=new Hashtable();
            sta=admin.getSysIds(userid);
            for (int i=0;i<sta.length;i++) {
                out.print("sigids[\""+sta[i]+"\"]=new Array(");
                
                sta2=admin.getSigIds(userid,sta[i]);
                old_sigids.put(sta[i],sta2);
                for (int j=0;j<sta2.length;j++) {
                    out.print("\""+sta2[j]+"\"");
                    if (j<sta2.length-1)
                        out.print(",");
                }
                
                out.println(");");
            }
            out.println();
            out.println("function displaySigIds()");
            out.println("{");
            out.println("  var sysid=document.forms[0].sysid.value;");
            out.println("  var data=sigids[sysid];");
            out.println("  var i;");
            out.println("  var res=\"\";");
            out.println();
            out.println("  for (i=0;i<data.length;i++) {");
            out.println("    res=res.concat(data[i]);");
            out.println("    res=res.concat(\"\\n\");");
            out.println("  }");
            out.println("  document.forms[0].sigids.value=res;");
            out.println("}");
            out.println("-->");
            out.println("</script>");
            
            // formularkopf erzeugen
            out.println("<form method=\"POST\" action=\"commit.html\">");
            out.println("<hr size=\"3\"/>");
            out.println("<p>Allgemeine Daten</p>");
            out.println("<table border=\"1\" frame=\"box\" rules=\"none\" cellpadding=\"4\">");
            
            // tabelleneintrag mit passwort erzeugen
            String passphrase=(String)session.getAttribute("passphrase");
            printTableLineSingle(row++,out,"Passwort","password",passphrase,"password");
            session.setAttribute("old_passphrase",passphrase);
            
            // tabelleneintrag mit gltigen kunden-ids erzeugen
            printTableLineMulti(row++,out,"Gltige Kunden-IDs",(sta=admin.getCustomerIds(userid)),"customerids");
            session.setAttribute("old_customerids",sta);
            
            // tabelleneintrag mit system-ids erzeugen
            sta=admin.getSysIds(userid);
            out.println("<tr bgcolor=\""+(((row++)&1)!=0?"#dddddd":"#f0f0f0")+"\">");
            out.println("<td valign=\"top\">Gltige System-IDs</td>");
            out.println("<td>");
            out.println("<select name=\"sysids\" size=\"5\" multiple=\"yes\">");
            for (int i=0;i<sta.length;i++) {
                out.println("  <option>"+sta[i]+"</option>");
            }
            out.println("</select><p/>");
            out.println("<input type=\"checkbox\" name=\"delselected\" value=\"true\">Markierte lschen</input><p/>");
            out.println("Neue hinzufgen: <input type=\"text\" name=\"newsysid\" size=\"20\"/>");
            out.println("</td></tr>");
            session.setAttribute("old_sysids",sta);
            
            // liste mit eingereichten sigids zeigen
            out.println("<tr bgcolor=\""+(((row++)&1)!=0?"#dddddd":"#f0f0f0")+"\">");
            out.println("<td valign=\"top\">Eingereichte Signatur-IDs</td>");
            out.println("<td>");
            sta=admin.getSysIds(userid);
            out.println("System-ID: <select name=\"sysid\" size=\"1\" onChange=\"displaySigIds()\">");
            for (int i=0;i<sta.length;i++) {
                out.println("  <option>"+sta[i]+"</option>");
            }
            out.println("</select><p/>");
            
            sta2=null;
            if (sta.length!=0)
                sta2=admin.getSigIds(userid,sta[0]);
            out.println("<textarea cols=\"30\" rows=\"5\" name=\"sigids\">");
            if (sta2!=null) {
                for (int i=0;i<sta2.length;i++) {
                    out.println(sta2[i]);
                }
            }
            out.println("</textarea>");
            out.println("</td></tr>");
            session.setAttribute("old_sigids",old_sigids);
            
            // tabelleintrag mit upd-version erzeugen
            printTableLineSingle(row++,out,"Versionsnummer Kontodaten (UPD)","text",(st=admin.getAccInfoVersion(userid)),"accinfoversion");
            session.setAttribute("old_accinfoversion",st);
            
            // tabelle abschlieen
            out.println("</table>");
            out.println("<p></p>");
            out.println("<hr size=\"3\">");
            
            // rdh-daten
            out.println("<p>Daten fr den RDH-Zugang</p>");
            out.println("<table border=\"1\" frame=\"box\" rules=\"none\" cellpadding=\"4\">");

            // tabelleneintrag mit flag fr vorhandene schlssel erzeugen
            printTableLineCheck(row++,out,"Schlssel vorhanden?",(b=admin.hasKeys(userid)),"haskeys");
            session.setAttribute("old_haskeys",new Boolean(b));
            
            out.println("</table>");
            out.println("<p></p>");
            out.println("<hr size=\"3\">");
            
            // pintan-daten
            out.println("<p>Daten fr den Zugang via HBCI-PIN/TAN</p>");
            out.println("<table border=\"1\" frame=\"box\" rules=\"none\" cellpadding=\"4\">");
            
            // tabelleneintrag mit pin erzeugen
            String pin=admin.getUserPIN(userid);
            printTableLineSingle(row++,out,"PIN","text",pin,"pin");
            session.setAttribute("old_pin",pin);
            
            // tabelleneintrag mit tans erzeugen
            String[] tans=admin.getUserTANList(userid);
            printTableLineMulti(row++,out,"TANs (TAN:avail)",tans,"tans");
            session.setAttribute("old_tans",tans);

            out.println("</table>");
            out.println("<p></p>");
            out.println("<hr size=\"3\">");

            // kontoinformationen anzeigen
            out.println("<p>Konten, auf die zugegriffen werden kann</p>");
            out.println("<table border=\"1\" frame=\"box\" rules=\"groups\" cellpadding=\"8\">");
            out.println("<thead><tr bgcolor=\"dddddd\"><th>#</th><th>Kontonummer</th><th>Kunden-ID</th><th>Kontoinhaber</th><th>Konto-Bezeichnung</th><th>Lschen</th></tr></thead>");
            
            out.println("<tbody>");
            Konto[] old_accounts=admin.getAccounts(userid);
            session.setAttribute("old_accounts",old_accounts);
            
            int len=old_accounts.length;
            for (int i=0;i<=len;i++) {
                Konto acc=(i<len)?old_accounts[i]:null;
                
                out.println("<tr bgcolor=\""+((i&1)!=0?"#dddddd":"#c0c0c0")+"\">");
                out.println("<td>"+(i+1)+"</td>");
                out.println("<td><input type=\"text\" name=\"number_"+i+"\" value=\""+(i<len?acc.number:"")+"\"></td>");
                out.println("<td><input type=\"text\" name=\"customerid_"+i+"\" value=\""+(i<len?acc.customerid:"")+"\"></td>");
                out.println("<td><input type=\"text\" name=\"name_"+i+"\" value=\""+(i<len?acc.name:"")+"\"></td>");
                out.println("<td><input type=\"text\" name=\"type_"+i+"\" value=\""+(i<len?acc.type:"")+"\"></td>");
                out.println("<td><input type=\"checkbox\" name=\"del_"+i+"\" value=\"delete\"></td>");
                out.println("</tr>");
            }
            out.println("</tbody>");
            
            out.println("</table");
            out.println("<p></p>");
            
            out.println("<input type=\"submit\" name=\"action\" value=\"Daten neu ermitteln\">");
            out.println("<input type=\"submit\" name=\"action\" value=\"nderungen bernehmen\">");
            out.println("</form>");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void printLogoutButton(PrintWriter out)
    {
        out.println("<form method=\"POST\" action=\"logout.html\">");
        out.println("<input type=\"submit\" name=\"action\" value=\"Logout\">");
        out.println("</form>");
    }
    
    // tabellenzeile mit einem einzelnen texteingabefeld erzeugen
    private void printTableLineSingle(int row,PrintWriter out,String title,String type,String content,String name)
    {
        out.println("<tr bgcolor=\""+((row&1)!=0?"#dddddd":"#f0f0f0")+"\">");
        out.println("<td valign=\"top\">"+title+"</td>");
        out.println("<td><input type=\""+type+"\" name=\""+name+"\" value=\""+content+"\" maxlength=\"30\"></td>");
        out.println("</tr>");
    }

    // tabellenzeile mit einem multi-line-text-eingabefeld erzeugen
    private void printTableLineMulti(int row,PrintWriter out,String title,String[] content,String name)
    {
        out.println("<tr bgcolor=\""+((row&1)!=0?"#dddddd":"#f0f0f0")+"\">");
        out.println("<td valign=\"top\">"+title+"</td>");
        out.println("<td><textarea cols=\"30\" rows=\"4\" name=\""+name+"\">");
        for (int i=0;i<content.length;i++) {
            out.println(content[i]);
        }
        out.println("</textarea></td>");
        out.println("</tr>");
    }
    
    // tabellenzeile mit checkbox-eintrag erzeugen
    private void printTableLineCheck(int row,PrintWriter out,String title,boolean content,String name)
    {
        out.println("<tr bgcolor=\""+((row&1)!=0?"#dddddd":"#f0f0f0")+"\">");
        out.println("<td valign=\"top\">"+title+"</td>");
        out.println("<td><input type=\"checkbox\" name=\""+name+"\" value=\"true\""+(content?" checked":"")+"></td>");
        out.println("</tr>");
    }
    
    private void handleCommitChanges(HttpSession session,HttpServletRequest request,HttpServletResponse response)
    {
        try {
            String userid=(String)session.getAttribute("userid");
            boolean wantedChanges=false;
            boolean somethingChanged=false;
            ArrayList errors=new ArrayList();
            ArrayList warnings=new ArrayList();
            
            if (request.getParameter("action").indexOf("nehmen")!=-1) {
                System.out.println("user clicked \"bernehmen\"");
                
                ServerAdmin admin=(ServerAdmin)session.getAttribute("admin");
                wantedChanges=true;

                // neues passwort extrahieren und berprfen
                String new_passphrase=request.getParameter("password");
                if (new_passphrase==null || new_passphrase.length()<5) {
                    errors.add("Passwort muss mindestens 5 Zeichen haben");
                }
                
                // neue systemids extrahieren und berprfen
                ArrayList new_sysids=new ArrayList(Arrays.asList((String[])session.getAttribute("old_sysids")));
                String st=request.getParameter("delselected");
                if (st!=null && st.equals("true")) {
                    String[] todelete=request.getParameterValues("sysids");
                    if (todelete!=null) {
                        for (int i=0;i<todelete.length;i++) {
                            System.out.println("removing sysid "+todelete[i]+" from sysids");
                            new_sysids.remove(todelete[i]);
                        }
                    }
                }
                st=request.getParameter("newsysid");
                if (st!=null && st.trim().length()!=0) {
                    System.out.println("adding sysid "+st+" to sysids");
                    new_sysids.add(st);
                }
                
                // neue kunden-ids extrahieren und berprfen
                ArrayList new_customerids=new ArrayList();
                String c=request.getParameter("customerids");
                if (c!=null) {
                    StringTokenizer tok=new StringTokenizer(c,"\n\r");
                    while (tok.hasMoreTokens()) {
                        String id=tok.nextToken().trim();
                        if (id.length()!=0) {
                            if (!new_customerids.contains(id)) {
                                new_customerids.add(id);
                            } else {
                                warnings.add("berspringe doppelte Kunden-ID \""+id+"\"");
                            }
                        }
                    }
                }
                if (new_customerids.size()==0) {
                    errors.add("Es muss mindestens eine Kunden-ID angegeben werden");
                }
                
                // UPD-version extrahieren und berprfen
                String new_accinfoversion=request.getParameter("accinfoversion");
                if (new_accinfoversion!=null && new_accinfoversion.length()!=0) {
                    int version;
                    try {
                        version=Integer.parseInt(new_accinfoversion);
                        if (version<=0) {
                            errors.add("UPD-Version muss grer \"0\" sein");
                        } else if (version<Integer.parseInt((String)session.getAttribute("old_accinfoversion"))) {
                            warnings.add("neue UPD-Version ist kleiner als alte UPD-Version");
                        }
                    } catch (Exception e) {
                        errors.add("Ungltiges Zahlenformat fr UPD-Version");
                    }
                } else {
                    errors.add("Es muss eine UPD-Version angegeben werden");
                }
                
                // schlssel-flag berprfen
                st=request.getParameter("haskeys");
                boolean new_haskeys=(st!=null && st.equals("true"));
                boolean old_haskeys=((Boolean)session.getAttribute("old_haskeys")).booleanValue();
                if (new_haskeys && !old_haskeys) {
                    errors.add("Kann Schlsselflag nicht einschalten, da keine Schlsseldaten bekannt");
                } else if (!new_haskeys && old_haskeys) {
                    warnings.add("Schlsseldaten werden gelscht, UNDO nicht mglich");
                }
                
                // neue sig-ids extrahieren und berprfen
                String    sysid=request.getParameter("sysid");
                ArrayList new_sigids=new ArrayList();
                String    s=request.getParameter("sigids");
                if (s!=null) {
                    StringTokenizer tok=new StringTokenizer(s,"\n\r");
                    while (tok.hasMoreTokens()) {
                        String id=tok.nextToken().trim();
                        if (id.length()!=0) {
                            if (!new_sigids.contains(id)) {
                                new_sigids.add(id);
                            } else {
                                warnings.add("berspringe doppelte Signatur-ID \""+id+"\"");
                            }
                        }
                    }
                }

                // neue pin extrahieren und berprfen
                String new_pin=request.getParameter("pin");
                if (new_pin==null || new_pin.length()<5) {
                    errors.add("PIN muss mindestens 5 Zeichen haben");
                }
                
                // neue tanliste extrahieren und berprfen
                ArrayList new_tans=new ArrayList();
                String tans=request.getParameter("tans");
                if (tans!=null) {
                    StringTokenizer tok=new StringTokenizer(tans,"\n\r");
                    while (tok.hasMoreTokens()) {
                        String tan=tok.nextToken().trim();
                        if (tan.length()!=0) {
                            int idx=tan.indexOf(":");
                            if (idx==-1 || idx!=tan.length()-2) {
                                errors.add("ungltiges TAN-Format: "+tan);
                                continue;
                            }
                            
                            String atan=tan.substring(0,idx);
                            String avail=tan.substring(idx+1);
                            
                            if (!avail.equals("0") && !avail.equals("1")) {
                                errors.add("ungltiges TAN-Format (:0 oder :1): "+tan);
                                continue;
                            }
                            
                            if (atan.length()<5) {
                                errors.add("TAN muss mindestens 5 Zeichen haben: "+tan);
                                continue;
                            }
                                
                            if (!new_tans.contains(tan)) {
                                new_tans.add(tan);
                            } else {
                                warnings.add("berspringe doppelte TAN \""+tan+"\"");
                            }
                        }
                    }
                }
                
                // Kontodaten extrahieren und berprfen
                ArrayList new_accounts=new ArrayList();
                String blz=admin.getBLZ();
                for (int i=0;;i++) {
                    st=request.getParameter("del_"+i);
                    // wenn schlssel nicht als "delete" markiert ist
                    if (st==null || !st.equals("delete")) {
                        // nummer berprfen
                        String number=request.getParameter("number_"+i);
                        // keine weiteren kontodaten, wenn nummer leer ist
                        if (number==null || number.length()==0)
                            break;
                        
                        String customerid=request.getParameter("customerid_"+i);
                        if (customerid==null || customerid.length()==0) {
                            errors.add("Kunden-ID #"+(i+1)+" darf nicht leer sein");
                        } else if (!new_customerids.contains(customerid)) {
                            errors.add("Kunden-ID #"+(i+1)+" ist keine gltige Kunden-ID");
                        } else {
                            String name=request.getParameter("name_"+i);
                            String type=request.getParameter("type_"+i);
                            
                            Konto acc=new Konto("DE",blz,number);
                            acc.curr="EUR";
                            acc.customerid=customerid;
                            acc.name=name;
                            acc.type=type;
                            new_accounts.add(acc);
                        }
                    }
                }
                
                // keine fehleraufgetreten
                if (errors.size()==0) {
                    // also serverdaten aktualisieren
                    
                    // passphrase aktualisieren
                    if (!new_passphrase.equals(session.getAttribute("old_passphrase"))) {
                        System.out.println("settings new passphrase for user "+userid+" to "+new_passphrase);
                        admin.setPassphrase(userid,new_passphrase);
                        somethingChanged=true;
                    }
                    
                    // system-id aktualisieren
                    ArrayList old_sysids=new ArrayList(Arrays.asList((String[])session.getAttribute("old_sysids")));
                    if (!new_sysids.equals(old_sysids)) {
                        System.out.println("settings new sysids for user "+userid+":");
                        for (Iterator i=new_sysids.iterator();i.hasNext();) {
                            String temp=(String)i.next();
                            System.out.print("checking new sysid "+temp+": ");
                            
                            if (old_sysids.contains(temp)) {
                                System.out.println("wird beibehalten");
                                old_sysids.remove(temp);
                            } else {
                                System.out.println("wird hinzugefgt");
                                admin.addSysId(userid,temp);
                                somethingChanged=true;
                            }
                        }
                        
                        for (Iterator i=old_sysids.iterator();i.hasNext();) {
                            String temp=(String)i.next();
                            System.out.println("removing sysid "+temp);
                            admin.removeSysId(userid,temp);
                            somethingChanged=true;
                        }
                    }
                    
                    // kunden-ids aktualisieren
                    String[] new_customerids_a=(String[])new_customerids.toArray(new String[0]);
                    if (!Arrays.equals(new_customerids_a,(String[])session.getAttribute("old_customerids"))) {
                        System.out.println("setting new customerids for user "+userid);
                        admin.setCustomerIds(userid,new_customerids_a);
                        somethingChanged=true;
                    }
                    
                    // upd-version aktualisieren
                    if (!new_accinfoversion.equals(session.getAttribute("old_accinfoversion"))) {
                        System.out.println("setting new upd version for user "+userid+" to "+new_accinfoversion);
                        admin.setAccInfoVersion(userid,new_accinfoversion);
                        somethingChanged=true;
                    }
                    
                    // evtl. schlssel lschen
                    if (old_haskeys && !new_haskeys) {
                        System.out.println("removing current user keys of user "+userid);
                        admin.removeKeys(userid);
                        somethingChanged=true;
                    }
                    
                    // sig-ids aktualisieren
                    if (sysid!=null && sysid.trim().length()!=0) {
                        ArrayList old_sigids=new ArrayList(Arrays.asList((String[])((Hashtable)session.getAttribute("old_sigids")).get(sysid)));
                        if (!new_sigids.equals(old_sigids)) {
                            System.out.println("setting new sigids for user "+userid);
                            admin.setSigIds(userid,sysid,
                                    (String[])new_sigids.toArray(new String[0]));
                            somethingChanged=true;
                        }
                    }
                    
                    // pin aktualisieren
                    if (!new_pin.equals(session.getAttribute("old_pin"))) {
                        System.out.println("settings new PIN for user "+userid+" to "+new_pin);
                        admin.setUserPIN(userid,new_pin);
                        somethingChanged=true;
                    }
                    
                    // tanliste aktualisieren
                    ArrayList old_tans=new ArrayList(Arrays.asList((String[])session.getAttribute("old_tans")));
                    if (!new_tans.equals(old_tans)) {
                        System.out.println("setting new TAN list for user "+userid);
                        admin.setUserTANList(userid,(String[])new_tans.toArray(new String[0]));
                        somethingChanged=true;
                    }
                    
                    // evtl. kontodaten aktualisieren
                    Konto[] new_accounts_a=(Konto[])new_accounts.toArray(new Konto[0]);
                    if (!Arrays.equals(new_accounts_a,(Konto[])session.getAttribute("old_accounts"))) {
                        System.out.println("updating account information for user "+userid);
                        admin.setAccounts(userid,new_accounts_a);
                        somethingChanged=true;
                    }
                    
                    if (somethingChanged) {
                        // wenn tatschlich nderungen stattgefunden haben, server-
                        // daten neu laden
                        System.out.println("have to reload user data because of changes being made");
                        admin.reloadUserData(userid);
                    } else {
                        System.out.println("no changes beeing made");
                    }
                }
            } else {
                System.out.println("user wants reload of all his data");
            }
                
            // ausgabestream erzeugen
            response.setContentType("text/html");
            PrintWriter out=response.getWriter();
            
            // html-kopf erzeugen
            printHeader(out,userid);
            
            if (wantedChanges) {
                if (errors.size()==0) {
                    if (somethingChanged) {
                        out.println("<p><font color=\"green\">nderungen ausgefhrt</font></p>");
                    } else {
                        out.println("<p><font color=\"green\">keine Vernderungen vorgenommen</font></p>");
                    }
                    
                    // evtl. warnungen ausgeben
                    if (warnings.size()!=0) {
                        out.println("<p><font color=\"orange\">Warnungen:</font></p>");
                        out.println("<ul>");
                        for (Iterator i=warnings.iterator();i.hasNext();) {
                            out.println("<p><font color=\"orange\">"+i.next()+"</font></p>");
                        }
                        out.println("</ul>");
                    }
                } else {
                    out.println("<p><font color=\"green\">keine nderungen ausgefhrt!</font></p>");
                    out.println("<p><font color=\"red\">Fehler:</font></p>");
                    out.println("<ul>");
                    for (Iterator i=errors.iterator();i.hasNext();) {
                        out.println("<p><font color=\"red\">"+i.next()+"</font></p>");
                    }
                    out.println("</ul>");
                }
            }
            
            // datentabelle schreiben
            printCurrentUserDataTable(session,response);
            // logout button erzeugen
            printLogoutButton(out);
            
            // footer erzeugen
            printFooter(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void handleLogout(HttpSession session,HttpServletRequest request,HttpServletResponse response)
    {
        try {
            session.invalidate();
            response.sendRedirect("index.html");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void printHeader(PrintWriter out,String userid)
    {
        out.println("<html><head></head><body>");
        out.println("<h1><em>HBCI4Java-Server</em> Account Administration</h1>");
        out.println("<p><b>Nutzerkennung</b>: <code>"+userid+"</code></p>");
    }
    
    private void printFooter(PrintWriter out)
    {
        out.println("<hr size=\"1\"/>");
        out.println("<p><font size=\"-2\">Copyright 2003-2004 by <a href=\"mailto:hbci4java@kapott.org\">Stefan Palme</a></font></p>");
        out.println("</body></html>");
    }
}
