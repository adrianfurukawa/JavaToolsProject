package com.imc.imctools.controllers;

import bsh.Interpreter;
import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.pojo.ReportPOJO;
import com.imc.imctools.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by faizal on 11/26/13.
 */
public class MainController extends Window {

    private Logger log = LoggerFactory.getLogger(MainController.class);

    public void onCreate() {
        if (Executions.getCurrent().getSession().getAttribute("user")==null) {
            Executions.sendRedirect("index.zul");
        } else {
            getFellow("wMain").setVisible(true);
        }
    }

    public void open(String viewName) {
        Window w = (Window) Executions.createComponents("views/" + viewName + ".zul", this, null);
        w.doOverlapped();
    }

    public void allActiveMembersReport() {
        long numberOfActiveMembers = 0;

        String[] columns = new String[] {
                "INSURANCE ID", "POLICY YEAR", "POLICY NUMBER", "COMPANY NAME", "INDEX",
                "SEQ", "NAME", "SEX", "MARITAL STATUS", "BDATE", "SDATE", "EDATE",
                "LABEL", "OCCUPATION", "IPPLAN", "OPPLAN", "MTPLAN", "DTPLAN", "GLPLAN",
                "OTPLAN", "CARD NUMBER", "POLCLIENT", "IDCLIENT", "STATUS",
                "MEMO 1", "MEMO 2", "MEMO 3", "MEMO 4", "MEMO 5", "MEMO 6", "MEMO 7",
                "MEMO 8", "MEMO 9", "MEMO 10", "ADDITION DATE", "MODIFICATION DATE",
                "EFFECTIVE DATE", "NIK", "CTR"
        };

        Session s = Libs.sfDB.openSession();

        List<Object[]> lClients = null;
        try {
            String q = "select "
                    + "b.hinsid, b.hinsname, a.hhdryy, a.hhdrpono, a.hhdrname "
                    + "from idnhltpf.dbo.hlthdr a "
                    + "inner join idnhltpf.dbo.inslf b on b.hinsid=a.hhdrinsid "
                    + "where "
                    + "b.hinsstat='Y' "
                    + "and a.hhdrmoe='' ";

            lClients = s.createSQLQuery(q).list();
        } catch (Exception ex) {
            log.error("allActiveMembersReport", ex);
        } finally {
            s.close();
        }

        class ClientFile {

            private File file;
            private String name;
            private HSSFWorkbook wb;

            public File getFile() {
                return file;
            }

            public void setFile(File file) {
                this.file = file;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public HSSFWorkbook getWb() {
                return wb;
            }

            public void setWb(HSSFWorkbook wb) {
                this.wb = wb;
            }

        }

        Map<String,ClientFile> clientMap = new HashMap<String,ClientFile>();

        if (lClients!=null) {
            for (Object[] o : lClients) {
                String clientId = Libs.nn(o[0]);
                ClientFile clientFile = clientMap.get(clientId);

                if (clientFile==null) {
                    String uuid = UUID.randomUUID().toString();
                    File fuuid = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);
                    clientFile = new ClientFile();
                    clientFile.setFile(fuuid);
                    clientFile.setName(Libs.nn(o[1]).trim());
                    clientFile.setWb(new HSSFWorkbook());
                    clientMap.put(clientId, clientFile);
                }

                if (clientFile!=null) {
                    HSSFWorkbook wb = clientFile.getWb();
                    HSSFSheet sheet = wb.createSheet(Libs.nn(o[2]).trim() + "-" + Libs.nn(o[3]).trim());

                    sheet.createFreezePane(0, 1, 0, 1);
                    int cnt = 0;
                    HSSFRow row = sheet.createRow(cnt);

                    for (int i=0; i<columns.length; i++) {
                        Libs.createCell(row, i, columns[i]);
                    }

                    String qry = "select "
                            + "HHDRINSID as INSURANCE_ID, HDT1YY as POLICY_YEAR, HDT1PONO as POLICY_NUMBER, "
                            + "HHDRNAME as COMPANY_NAME, HDT1IDXNO as 'INDEX', HDT1SEQNO as SEQ, HDT1NAME as NAME, HDT1SEX as SEX, "
                            + "HDT1MSTAT as MARITAL_STATUS, "
                            + "convert(varchar, HDT1BDTYY) + '-' + convert(varchar, HDT1BDTMM) + '-' + convert(varchar, HDT1BDTDD) as BDATE, "
                            + "convert(varchar, HDT2SDTYY) + '-' + convert(varchar, HDT2SDTMM) + '-' + convert(varchar, HDT2SDTDD) as SDATE, "
                            + "convert(varchar, HDT2MDTYY) + '-' + convert(varchar, HDT2MDTMM) + '-' + convert(varchar, HDT2MDTDD) as EDATE, "
                            + "HDT1CODE as LABEL, HDT1OCCU as OCCUPATION, "
                            + "HDT2PLAN1 as IPPLAN, HDT2PLAN2 as OPPLAN, HDT2PLAN3 as MTPLAN, HDT2PLAN4 as DTPLAN, "
                            + "HDT2PLAN5 as GLPLAN, HDT2PLAN6 as OTPLAN, HDT1NCARD as CARD_NUMBER, d.HEMPCNPOL as POLCLIENT, d.HEMPCNID as IDCLIENT, "
                            + "HDT2MOE as STATUS, "
                            + "HMEM1DATA1, HMEM1DATA2, HMEM1DATA3, HMEM1DATA4, HMEM1DATA5, HMEM1DATA6, HMEM1DATA7, HMEM1DATA8, HMEM1DATA9, HMEM1DATA0, "
                            + "(convert(varchar,HDT2ADTYY) + '-' + convert(varchar,HDT2ADTMM) + '-' + convert(varchar,HDT2ADTDD)) as ADDITION_DATE, "
                            + "(convert(varchar,HDT2UPDYY) + '-' + convert(varchar,HDT2UPDMM) + '-' + convert(varchar,HDT2UPDDD)) as MODIFICATION_DATE, "
                            + "(convert(varchar,HDT2EDTYY) + '-' + convert(varchar,HDT2EDTMM) + '-' + convert(varchar,HDT2EDTDD)) as EFFECTIVE_DATE, "
                            + "HEMPMEMO3 as NIK, "
                            + "a.HDT1CTR as CTR "
                            + "from IDNHLTPF.dbo.HLTDT1 a "
                            + "inner join IDNHLTPF.dbo.HLTHDR b "
                            + "on a.HDT1YY=b.HHDRYY "
                            + "and a.HDT1BR=b.HHDRBR "
                            + "and a.HDT1DIST=b.HHDRDIST "
                            + "and a.HDT1PONO=b.HHDRPONO "
                            + "inner join IDNHLTPF.dbo.HLTDT2 c "
                            + "on c.HDT2YY=b.HHDRYY "
                            + "and c.HDT2BR=b.HHDRBR and c.HDT2DIST=b.HHDRDIST "
                            + "and c.HDT2PONO=b.HHDRPONO and c.HDT2IDXNO=a.HDT1IDXNO "
                            + "and c.HDT2SEQNO=a.HDT1SEQNO "
                            + "and c.HDT2CTR=a.HDT1CTR "
                            + "inner join IDNHLTPF.dbo.HLTEMP d "
                            + "on a.HDT1YY=d.HEMPYY "
                            + "and a.HDT1BR=d.HEMPBR "
                            + "and a.HDT1DIST=d.HEMPDIST "
                            + "and a.HDT1PONO=d.HEMPPONO "
                            + "and a.HDT1IDXNO=d.HEMPIDXNO "
                            + "and a.HDT1SEQNO=d.HEMPSEQNO "
                            + "and a.HDT1CTR=d.HEMPCTR "
                            + "left outer join idnhltpf.dbo.hltmemo1 e "
                            + "on a.HDT1YY=e.HMEM1YY "
                            + "and a.HDT1BR=e.HMEM1BR "
                            + "and a.HDT1DIST=e.HMEM1DIST "
                            + "and a.HDT1PONO=e.HMEM1PONO "
                            + "and a.HDT1IDXNO=e.HMEM1IDXNO "
                            + "and a.HDT1SEQNO=e.HMEM1SEQNO "
                            + "and a.HDT1CTR=e.HMEM1CTR "
                            + "where "
                            + "a.hdt1idxno not in (99998, 99999) "
                            + "and a.hdt1ctr=0 "
                            + "and c.hdt2moe='' "
                            + "and a.hdt1yy=" + Libs.nn(o[2]) + " "
                            + "and a.hdt1pono=" + Libs.nn(o[3]) + " ";

                    s = Libs.sfDB.openSession();
                    try {
                        FileOutputStream fos = new FileOutputStream(clientFile.getFile());

                        List<Object[]> l = s.createSQLQuery(qry).list();

                        for (Object[] o2 : l) {
                            cnt++;

                            row = sheet.createRow(cnt);
                            numberOfActiveMembers++;

                            for (int i=0; i<o2.length; i++) {
                                Libs.createCell(row, i, o2[i]);
                            }
                        }

                        wb.write(fos);
                        fos.close();
                    } catch (Exception ex) {
                        log.error("allActiveMembersReport", ex);
                    }
                }
            }
        }

//        Create readme
        String uuid = UUID.randomUUID().toString();
        File fReadme = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);
        try {
            PrintWriter pw = new PrintWriter(fReadme);
            pw.println(numberOfActiveMembers);
            pw.close();
        } catch (Exception ex) {

        }

//        Create ZIP
        if (clientMap.keySet().size()>0) {
            uuid = UUID.randomUUID().toString();
            File zipFile = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);
            try {
                byte[] buffer = new byte[1024];

                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);

                for (String clientId : clientMap.keySet()) {
                    ClientFile clientFile = clientMap.get(clientId);

                    ZipEntry ze = new ZipEntry(clientFile.getName() + ".xls");
                    zos.putNextEntry(ze);

                    FileInputStream in = new FileInputStream(clientFile.getFile());
                    int len;
                    while ((len = in.read(buffer))>0) {
                        zos.write(buffer, 0, len);
                    }

                    in.close();
                }

                ZipEntry ze = new ZipEntry("readme" + ".txt");
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(fReadme);
                int len;
                while ((len = in.read(buffer))>0) {
                    zos.write(buffer, 0, len);
                }
                in.close();

                zos.closeEntry();
                zos.close();
                InputStream is = new FileInputStream(zipFile);
                Filedownload.save(is, "application/zip", "All Active Members Report-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
                zipFile.delete();

                for (String clientName : clientMap.keySet()) {
                    ClientFile clientFile = clientMap.get(clientName);
                    clientFile.getFile().delete();
                }
            } catch (Exception ex) {
                log.error("generate", ex);
            }
        }
    }

    public void restartIDNDB01() {
        if (Messagebox.show("Are you sure you want to restart IDN-DB-01?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            try {
                InetAddress addr = InetAddress.getByName("10.100.1.5");
                Socket socket = new Socket(addr, 60000);
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw.println("cmd.exe /C net stop mssqlserver");
                pw.close();
                br.close();
                socket.close();

                Thread.sleep(20000);

                socket = new Socket(addr, 60000);
                pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw.println("cmd.exe /C net start mssqlserver");
                pw.close();
                br.close();
                socket.close();

                Messagebox.show("IDN-DB-01 has been restarted", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (Exception ex) {
                log.error("restartIDNDB01", ex);
            }
        }
    }

    public void restartIDNEDC01() {
        if (Messagebox.show("Are you sure you want to restart IDN-EDC-01?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            try {
                InetAddress addr = InetAddress.getByName("10.100.1.15");
                Socket socket = new Socket(addr, 60000);
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw.println("cmd.exe /C net stop mssqlserver");
                pw.close();
                br.close();
                socket.close();

                Thread.sleep(20000);

                socket = new Socket(addr, 60000);
                pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw.println("cmd.exe /C net start mssqlserver");
                pw.close();
                br.close();
                socket.close();

                Messagebox.show("IDN-EDC-01 has been restarted", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (Exception ex) {
                log.error("restartIDNEDC01", ex);
            }
        }
    }

    public void logout() {
        if (Messagebox.show("Are you sure you want to logout?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
            Executions.getCurrent().getSession().removeAttribute("user");
            Executions.getCurrent().getSession().setMaxInactiveInterval(0);
            Executions.getCurrent().getSession().invalidate();
            Executions.sendRedirect("index.zul");
        }
    }

}
