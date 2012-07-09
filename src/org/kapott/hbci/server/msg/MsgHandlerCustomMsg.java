
/*  $Id: MsgHandlerCustomMsg.java,v 1.7 2005/06/10 18:03:03 kleiner Exp $

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

package org.kapott.hbci.server.msg;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.server.Dialog;
import org.kapott.hbci.server.JobContext;
import org.kapott.hbci.status.HBCIRetVal;

public class MsgHandlerCustomMsg 
    extends AbstractMsgHandler 
{
    public MsgHandlerCustomMsg(MSG cryptedMsg,MSG decryptedMsg,String decryptedMsgData,Dialog conn)
    {
        super(cryptedMsg,decryptedMsg,decryptedMsgData,conn);
    }
    
    protected void initMessageHandler()
    {
        dialog.setFlag("signResponse",true);
        dialog.setFlag("encryptResponse",true);
    }
    
    protected boolean isMessageAllowed()
    {
        return dialog.isInitialized() 
        && !dialog.isAnonymous() 
        && !dialog.getFlag("forceEnd");
    }
    
    protected void handleMessageContent()
    {
        HBCIUtils.log("handling user jobs",HBCIUtils.LOG_DEBUG);
        
        boolean globalOK=true;
        boolean globalWarning=false;
        
        Properties data=getData();
        int resultCounter=0;
        
        // loop through all message data
        for (Enumeration e=data.keys();e.hasMoreElements();) {
            String key=(String)e.nextElement();
            
            // if job segment found
            if (key.startsWith("GV") && key.endsWith(".SegHead.code")) {
                int firstDot=key.indexOf('.');
                int secondDot=key.indexOf('.',firstDot+1);
                
                // extract name of job segment
                String segname=key.substring(0,secondDot);
                HBCIUtils.log("found segmentname '"+segname+"'",HBCIUtils.LOG_INFO);
                
                // extract name of job
                String jobname=key.substring(firstDot+1,secondDot);
                int pos=jobname.indexOf('_');
                if (pos!=-1)
                    jobname=jobname.substring(0,pos);
                
                // extract version of job
                pos=jobname.length()-1;
                while (pos>=0) {
                    char ch=jobname.charAt(pos);
                    if (!(ch>='0' && ch<='9'))
                        break;
                    pos--;
                }
                
                int jobversion=Integer.parseInt(jobname.substring(pos+1));
                jobname=jobname.substring(0,pos+1);
                HBCIUtils.log("found job '"+jobname+"' with version "+jobversion,HBCIUtils.LOG_INFO);
                
                // *** berprfen, ob job/-version in BPD erlaubt sind
                // *** berprfen, ob max*/min* aus BPD eingehalten sind
                
                // extract data for this job
                Properties jobData=new Properties();
                String segnum=data.getProperty(segname+".SegHead.seq");
                int skipSegName=segname.length()+1;
                int skipMsgName=("CustomMsg.").length();
                
                for (Enumeration enum1=data.keys();enum1.hasMoreElements();) {
                    String dataKey=(String)enum1.nextElement();
                    if (dataKey.startsWith(segname+".")) {
                        jobData.setProperty(dataKey.substring(skipSegName),getData(dataKey));
                    } else if (dataKey.startsWith(segnum+":")) {
                        // store deref value of each data element in jobdata object
                        String path=getData(dataKey).substring(skipMsgName+skipSegName);
                        jobData.setProperty("_deref."+path,dataKey.substring(2));
                    }
                }
                
                // call handler for this job
                // *** hier spter irgendeine schnittstelle zu highlevel-handlern implementieren
                JobContext jobContext=new JobContext(dialog,jobname,jobversion,jobData);
                getServerData().handleGVCallback(jobContext);
                
                // resultStatus in antwort einbauen
                for (Iterator it=jobContext.getStatusData().iterator();it.hasNext();) {
                    HBCIRetVal retval=(HBCIRetVal)it.next();
                    addSegRet(segname,retval.deref,retval.code,retval.text,retval.params);
                    if (retval.code.startsWith("9"))
                        globalOK=false;
                    else if (retval.code.startsWith("3"))
                        globalWarning=true;
                }
                
                // resultData in Antwort einbauen
                // suffix hinter "CustomMsgRes.GVRes_i"
                String jobSuffix="."+jobname+"Res"+Integer.toString(jobversion);
                Properties resultData=jobContext.getResultData();
                // zaehler fr anzahl der ergebnis-segmente
                int maxIdx=-1;
                
                for (Enumeration enum1=resultData.keys();enum1.hasMoreElements();) {
                    // "result_i.*"
                    String resultKey=(String)enum1.nextElement();
                    String resultValue=resultData.getProperty(resultKey);
                    
                    // nummer des ergebnisses extrahieren
                    int delimPos=resultKey.indexOf('_');
                    int dotPos=resultKey.indexOf('.');
                    int resultIdx=0;
                    if (delimPos!=-1)
                        resultIdx=Integer.parseInt(resultKey.substring(delimPos+1,dotPos))-1;

                    // hchste jemals gefundene nummer sichern
                    if (resultIdx>maxIdx)
                        maxIdx=resultIdx;

                    /* "GVRes_i.*" erzeugen; i ergibt sich aus bisheriger globaler ergebnisanzahl (resultCounter) 
                       plus nummer des ergebnissegmentes fr diesen job */
                    String resultHeader=HBCIUtilsInternal.withCounter("GVRes",resultCounter+resultIdx)+jobSuffix;
                    setData(resultHeader+resultKey.substring(dotPos),resultValue);
                }
                
                // segrefs und request-tags setzen
                for (int i=0;i<=maxIdx;i++) {
                    String resultHeader=HBCIUtilsInternal.withCounter("GVRes",resultCounter+i)+jobSuffix;
                    setData(resultHeader,"requested");
                    setData(resultHeader+".SegHead.ref",getData(segname+".SegHead.seq"));
                }
                
                // anzahl der globalen ergebissegmente um die anzahl der segm. fr diesen job erhhen
                HBCIUtils.log("created "+(maxIdx+1)+" result segments",HBCIUtils.LOG_INFO);
                resultCounter+=maxIdx+1;
            }
        }
        
        if (globalOK)
            addGlobRet("0010","Alle Auftrge OK",null);
        else 
            addGlobRet("9050","Teilweise fehlerhaft",null);
        
        if (globalWarning)
            addGlobRet("3060","Teilweise liegen Warnungen/Hinweise vor",null);
    }
    
    protected void postProcess()
    {
    }
}
