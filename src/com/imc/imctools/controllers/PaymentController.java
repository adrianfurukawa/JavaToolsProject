package com.imc.imctools.controllers;

import com.imc.imctools.pojo.BankAccountPOJO;
import com.imc.imctools.pojo.CompanyPOJO;
import com.imc.imctools.pojo.MemberPOJO;
import com.imc.imctools.pojo.ReportPOJO;
import com.imc.imctools.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by faizal on 2/5/14.
 */
public class PaymentController extends Window {

    private Logger log = LoggerFactory.getLogger(PaymentController.class);
    private Listbox lbReimbursement;
    private Listbox lbProvider;
    private Textbox tChequeNumber;
    private Combobox cbPaymentType;
    private Combobox cbBank;
    private Combobox cbAccountNumber;
    private Map<String,String> biCodesMap = new HashMap<String,String>();

    public void onCreate() {
        initComponents();
        loadBiCodes();
    }

    private void initComponents() {
        lbReimbursement = (Listbox) getFellow("lbReimbursement");
        lbProvider = (Listbox) getFellow("lbProvider");
        tChequeNumber = (Textbox) getFellow("tChequeNumber");
        cbPaymentType = (Combobox) getFellow("cbPaymentType");
        cbBank = (Combobox) getFellow("cbBank");
        cbAccountNumber = (Combobox) getFellow("cbAccountNumber");

        cbPaymentType.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                lbReimbursement.getItems().clear();
                lbProvider.getItems().clear();
                if (cbPaymentType.getSelectedIndex()==0) {
                    lbReimbursement.setVisible(true);
                    lbProvider.setVisible(false);
                } else {
                    lbReimbursement.setVisible(false);
                    lbProvider.setVisible(true);
                }
            }
        });
    }

    public void populate() {
        lbReimbursement.getItems().clear();
        lbProvider.getItems().clear();

        String[] chequeSeg = tChequeNumber.getText().split("\\-");

        List<Object[]> l = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "hovccno, HOVCYY,HOVCPONO,ltrim(HOVCTCLAIM),hovcoutno "
                    + "from idnhltpf.dbo.hltovc "
                    + "where "
                    + "hovctyp='" + chequeSeg[0].toUpperCase() + "' "
                    + "and hovccqno=" + chequeSeg[1] + " ";

            l = s.createSQLQuery(q).list();
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s.close();
        }

        if (l!=null && l.size()>0) { 
            if (cbPaymentType.getSelectedIndex()==0) {
//                Reimbursement
                for (Object[] o : l) {
                    s = Libs.sfDB.openSession();
                    try {
                        String q = "select "
                                + "a.hclmcno, a.hclmyy, a.hclmpono, b.hhdrname, "
                                + "sum((" + Libs.runningAddFields("a.hclmcamt", 30) + ")) as proposed, "
                                + "sum((" + Libs.runningAddFields("a.hclmaamt", 30) + ")) as approved, "
                                + "a.hclmidxno, a.hclmseqno "
                                + "from idnhltpf.dbo.hltclm a "
                                + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                                + "where "
                                + "a.hclmnomor=" + Libs.nn(o[0]) + " "
                                + "and a.HCLMYY='" + Libs.nn(o[1]) + "' "
                          		+ "and a.HCLMPONO='" + Libs.nn(o[2]) + "' "
                          		+ "and left(right(rtrim(a.HCLMCNO),2),1) ='" + Libs.nn(o[3]) + "' "
                          		+ "and a.hclmrecid <> 'C' "
                          		+ "group by a.hclmcno, a.hclmyy, a.hclmpono, b.hhdrname, a.hclmidxno, a.hclmseqno  ";
                        
                        List<Object[]> l2 = s.createSQLQuery(q).list();
                        for (Object[] o2 : l2) {
                            BankAccountPOJO bankAccountPOJO = Libs.getBankAccount(Integer.valueOf(Libs.nn(o2[1])), Integer.valueOf(Libs.nn(o2[2])), Integer.valueOf(Libs.nn(o2[6])));

                            Listitem li = new Listitem();
                            li.setValue(new Object[] { o2, bankAccountPOJO });

                            li.appendChild(new Listcell(bankAccountPOJO.getAccountNumber()));
                            li.appendChild(new Listcell(bankAccountPOJO.getAccountName()));
                            li.appendChild(new Listcell(bankAccountPOJO.getBank()));
                            li.appendChild(new Listcell(bankAccountPOJO.getAddress()));
                            li.appendChild(new Listcell(Libs.nn(o2[0])));
                            li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o2[5])), "#,###.00"));
                            li.appendChild(new Listcell(bankAccountPOJO.getBiCode()));
                            li.appendChild(new Listcell(Libs.nn(o2[1]) + "-1-0-" + Libs.nn(o2[2])));
                            li.appendChild(new Listcell(Libs.nn(o2[3])));

                            lbReimbursement.appendChild(li);
                        }
                    } catch (Exception ex) {
                        log.error("populate", ex);
                    } finally {
                        s.close();
                    }
                }
            } else {
//                Provider
                for (Object[] o : l) {
                    s = Libs.sfDB.openSession();
                    try {
                        String q = "select "
                                + "a.hclmcno, a.hclmyy, a.hclmpono, b.hhdrname, "
                                + "sum(" + Libs.runningAddFields("a.hclmcamt", 30) + ") as proposed, "
                                + "sum(" + Libs.runningAddFields("a.hclmaamt", 30) + ") as approved, "
                                + "c.hproname, "
                                + "d.hinsname, "
                                + "c.hpronomor "
                                + "from idnhltpf.dbo.hltclm a "
                                + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                                + "inner join idnhltpf.dbo.hltpro c on c.hpronomor=a.hclmnhoscd "
                                + "inner join idnhltpf.dbo.inslf d on d.hinsid=b.hhdrinsid "
                                + "where "
                                + "a.hclmnomor=" + Libs.nn(o[0]) + "  "
                                + "and a.HCLMYY='" + Libs.nn(o[1]) + "' "
                          		+ "and a.HCLMPONO='" + Libs.nn(o[2]) + "' "
                          		+ "and left(right(rtrim(a.HCLMCNO),2),1) ='" + Libs.nn(o[3]) + "' "
                                + "and a.hclmrecid<>'C' "
                                + "group by a.hclmcno, a.hclmyy, a.hclmpono, b.hhdrname, c.hproname, d.hinsname, c.hpronomor  ";

                        List<Object[]> l2 = s.createSQLQuery(q).list();
                        for (Object[] o2 : l2) {
                            List<Object[]> l3 = null;
                            s = Libs.sfDB.openSession();
                            try {
                                q = "select "
                                        + "a.nointernal, b.ttltagihan, a.nokwitansi, b.diskon, a.tgltrmberkas, c.no_hid "
                                        + "from aso.dbo.preterimaprov a "
                                        + "inner join aso.dbo.preterimaprov_dtlins b on a.nointernal = b.nointernal "
                                        + "inner join [DB] c on a.nointernal=c.nointernal "
                                        + "where "
                                        + "c.no_hid='" + Libs.nn(o2[0]).trim().replace("IDN/", "") + "' ";

                                if (Libs.nn(o2[0]).trim().endsWith("IP")) {
                                    q = q.replace("[DB]", "aso.dbo.pre_ip_provider");
                                } else {
                                    q = q.replace("[DB]", "aso.dbo.pre_op_provider");
                                }

                                l3 = s.createSQLQuery(q).list();
                            } catch (Exception ex) {
                                log.error("populate", ex);
                            } finally {
                                s.close();
                            }

                            if (l3!=null && l3.size()>0) {
                                Object[] o3 = l3.get(0);
                                Listitem li = new Listitem();
                                li.setValue(new Object[] { o2[8], o3[2], "", o2[6], "", o2[4], o3[3], o2[5] });
                                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o2[4])), "#,###.00"));
                                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o2[5])), "#,###.00"));
                                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o3[3])), "#,###.00"));
                                li.appendChild(new Listcell(""));
                                li.appendChild(new Listcell(Libs.nn(o3[0])));
                                li.appendChild(new Listcell(Libs.nn(o3[2])));
                                li.appendChild(new Listcell(Libs.nn(o2[0])));
                                li.appendChild(new Listcell(Libs.nn(o2[6])));
                                li.appendChild(new Listcell(Libs.nn(o2[1]) + "-1-0-" + Libs.nn(o2[2])));
                                li.appendChild(new Listcell(Libs.nn(o2[3])));
                                li.appendChild(new Listcell(Libs.nn(o2[7])));
                                li.appendChild(new Listcell(""));
                                li.appendChild(new Listcell(new SimpleDateFormat("yyyy-MM-dd").format((Date) o3[4])));
                                lbProvider.appendChild(li);
                            }
                        }
                    } catch (Exception ex) {
                        log.error("populate", ex);
                    } finally {
                        if (s!=null && s.isOpen()) s.close();
                    }
                }
            }
        } else Messagebox.show("Cheque Number does not exist!", "Error", Messagebox.OK, Messagebox.ERROR);
    }

    public void submit() {  //submit-------------------------------------------------------------------------------------------------------------
        String uuidCSV = UUID.randomUUID().toString();
        String uuidXLS = UUID.randomUUID().toString();

        File fCSV = new File(Libs.config.get("temp_dir") + File.separator + uuidCSV);
        File fXLS = new File(Libs.config.get("temp_dir") + File.separator + uuidXLS);
        File fTemplate = new File(Executions.getCurrent().getSession().getWebApp().getRealPath("/resources/bank_transfer_templates/dbi.xls"));

        Map<String,Object[]> reimbursementBuffer = new HashMap<String,Object[]>();
        Map<String,Object[]> providerBuffer = new HashMap<String,Object[]>();

        try {
            HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(fTemplate));
            HSSFSheet sheet = wb.getSheetAt(0);
            FileOutputStream out = new FileOutputStream(fXLS);
            PrintWriter pw = new PrintWriter(fCSV);

            Listbox lb = null;
            if (cbPaymentType.getSelectedIndex()==0) {
            	lb = lbReimbursement;
            	lbReimbursement.setVisible(true);
                lbProvider.setVisible(false);

            }
            	
            else if (cbPaymentType.getSelectedIndex()==1){
            	lb = lbProvider;
            	lbReimbursement.setVisible(false);
                lbProvider.setVisible(true);

            } 
            	
            

            int i = 2;
            for (Listitem li : lb.getItems()) {
                Object[] o2 = null;

                if (cbPaymentType.getSelectedIndex()==0) {
//                    REIMBURSEMENT
                    Object[] o = li.getValue();
                    o2 = (Object[]) o[0];

                    BankAccountPOJO bankAccountPOJO = null;
                    bankAccountPOJO = (BankAccountPOJO) o[1];

                    String key = bankAccountPOJO.getAccountName() + bankAccountPOJO.getAccountNumber();
                    Object[] row = reimbursementBuffer.get(key);
                    if (row==null) {
                        row = new Object[] {
                                o[1],
                                o2[1],
                                o2[5], // Payment
                                o2[6],
                                o2[7],
                                o2[0],
                                o2[2],
                                o2[6] + "-" + o2[7] + "|" + o2[0]
                        };
                        reimbursementBuffer.put(key, row);

                    } else {
                        row[2] = Double.valueOf(Libs.nn(row[2])) + Double.valueOf(Libs.nn(o2[5]));
                        row[7] = row[7] + ", " + o2[6] + "-" + o2[7] + "|" + o2[0];
                    }
                } else {
//                    PROVIDER  	                	 
                    o2 = li.getValue();
                    Object[] row = providerBuffer.get(Libs.nn(o2[3]));
                    if (row==null) {
                    	//Messagebox.show("data kosong");
                        row = new Object[] {
                                o2[0],  // Provider Code
                                o2[1],  // Receipt Number
                                Libs.nn(o2[3]).trim(),  // Provider Name
                                o2[7],  // Paid Amount
                                o2[6]  // Discount
                        };
                        
                        providerBuffer.put(Libs.nn(o2[3]), row);
                    } else {
                    	//Messagebox.show("data kosong");
                        row[3] = Double.valueOf(Libs.nn(row[3])) + Double.valueOf(Libs.nn(o2[7]));
                        row[4] = Double.valueOf(Libs.nn(row[4])) + Double.valueOf(Libs.nn(o2[6]));
                    } 
                    
                    
                	 
                }
            }

            if (cbPaymentType.getSelectedIndex()==0) {
//                REIMBURSEMENT
                List<String[]> records = new ArrayList<String[]>();

                for (String ks : reimbursementBuffer.keySet()) {
                    Object[] rw = reimbursementBuffer.get(ks);

                    BankAccountPOJO bankAccountPOJO = null;
                    String[] record = new String[67];

                    bankAccountPOJO = (BankAccountPOJO) rw[0];
                    String accountNumber = cbAccountNumber.getText();
                    String biCode = Libs.nn(biCodesMap.get(bankAccountPOJO.getBank().toUpperCase()));

                    if (Libs.nn(rw[1]).equals("51052")) accountNumber = "0001388038"; // Jakarta Office
                    if (Libs.nn(rw[1]).equals("51053")) accountNumber = "0001396038"; // National Office

                    String trans = "";
                    String[] descSeg = Libs.nn(rw[7]).split(",");
                    for (String desc : descSeg) {
                        String indexSeq = desc.split("\\|")[0];
                        String hid = desc.split("\\|")[1].trim();

                        trans += Libs.getMemberNameByIndex(Integer.valueOf(Libs.nn(rw[1])), Integer.valueOf(Libs.nn(rw[6])), Integer.valueOf(indexSeq.split("\\-")[0].trim()), indexSeq.split("\\-")[1]).trim() + " (" + hid + "), ";
                    }

                    if (trans.endsWith(", ")) trans = trans.substring(0, trans.length()-2);

                    String[] transColumn = new String[] { "", "", "" };
                    for (int j=0; j<(trans.length()/30)+1; j++) {
                        if (j<3) {
                            int max = j*30+30;
                            if (max>trans.length()) max = trans.length();
                            transColumn[j] = trans.substring(j*30, max);
                        }
                    }

                    record[0] = accountNumber;
                    record[1] = "IDR";
                    record[2] = accountNumber;
                    record[3] = "IDR";
                    record[4] = "CNS";
                    record[5] = "15";
                    record[7] = "Y";
                    record[8] = "IDR";
                    record[9] = new SimpleDateFormat("ddMMyyyy").format(new Date());
                    record[14] = new SimpleDateFormat("ddMMyyyy").format(new Date());
                    record[15] = "N";
                    record[17] = Libs.nn(rw[2]);
                    record[19] = transColumn[0];
                    record[20] = transColumn[1];
                    record[21] = transColumn[2];
                    record[26] = bankAccountPOJO.getAccountName();
                    record[27] = bankAccountPOJO.getAccountNumber();
                    record[36] = bankAccountPOJO.getBank();
                    record[38] = biCode;
                    record[39] = "25-ZZZ";
                    record[43] = "ID";
                    record[58] = bankAccountPOJO.getEmail();

                    records.add(record);
                }

                order2(records);

                for (String[] record : records) {
                    String line = "";

                    int c = 0;
                    HSSFRow row = sheet.createRow(i);

                    for (String s : record) {
                        line += Libs.nn(s) + ";";

                        org.apache.poi.ss.usermodel.Cell cell;

                        if (c!=17) Libs.createCell(row, c, Libs.nn(s));
                        else {
                            cell = Libs.createCell(row, c, Double.valueOf(Libs.nn(s)));
                            CellStyle cs = cell.getCellStyle();
                            cs.setDataFormat((short) 4);
                            cell.setCellStyle(cs);
                        }

                        c++;
                    }

                    if (line.endsWith(";")) line = line.substring(0, line.length()-1);
                    pw.println(line + "\r");

                    i++;
                }

//            } else {
            } else if (cbPaymentType.getSelectedIndex()==1) {
            	
            	//Messagebox.show("provider data "+ providerBuffer.keySet().size());
//                PROVIDER
          	
/*              List<String> keyset = new ArrayList(providerBuffer.keySet());
              Collections.sort(keyset);              
              List<String[]> records = new ArrayList<String[]>();
              for (String ks : keyset) {
                  Object[] rw = providerBuffer.get(ks);
*/   
                List<String[]> records = new ArrayList<String[]>();

                for (String ks : providerBuffer.keySet()) {
                	
                	try{
                		//Messagebox.show("key " + ks);
                        Object[] rw = providerBuffer.get(ks);
                	
                        
                        BankAccountPOJO bankAccountPOJO = null;
                        String accountName;
                        String[] record = new String[67];

                        bankAccountPOJO = Libs.getProviderBankAccount(Integer.valueOf(Libs.nn(rw[0])));
                        String biCode = Libs.nn(biCodesMap.get(bankAccountPOJO.getBank().toUpperCase()));
                  
                        record[0] = cbAccountNumber.getText();
                        record[1] = "IDR";
                        record[2] = cbAccountNumber.getText();
                        record[3] = "IDR";
                        record[4] = "CNS";
                        record[5] = "15";
                        record[7] = "Y";
                        record[8] = "IDR";
                        record[9] = new SimpleDateFormat("ddMMyyyy").format(new Date());
                        record[14] = new SimpleDateFormat("ddMMyyyy").format(new Date());
                        record[15] = "N";
                        record[17] = Libs.nn(rw[3]);
                        record[19] = Libs.nn(rw[1]);
                        record[26] = bankAccountPOJO.getAccountName().trim();
                        record[27] = bankAccountPOJO.getAccountNumber();
                        record[36] = bankAccountPOJO.getBank();
                        record[38] = biCode;
                        record[39] = "25-ZZZ";
                        record[43] = "ID";
                        record[58] = (bankAccountPOJO.getEmail().isEmpty() ? "provider.admin@imcare177.com" : bankAccountPOJO.getEmail());

                        records.add(record);
                		
                	}catch (Exception e){
                		//e.printStackTrace();
                   	}
                }
                  
               
             order(records);
//             Messagebox.show("masuk sini dodol,  banyak data " +records.size());

                for (String[] record : records) {
                    String line = "";

                    int c = 0;
                    HSSFRow row = sheet.createRow(i);

                    for (String s : record) {
                        line += Libs.nn(s) + ";";

                        org.apache.poi.ss.usermodel.Cell cell;

                        if (c!=17) Libs.createCell(row, c, Libs.nn(s));
                        else {
                            cell = Libs.createCell(row, c, Double.valueOf(Libs.nn(s)));
                            CellStyle cs = cell.getCellStyle();
                            cs.setDataFormat((short) 4);
                            cell.setCellStyle(cs);
                        }
                        c++;
                    }
                    if (line.endsWith(";")) line = line.substring(0, line.length()-1);
                    pw.println(line + "\r");

                    i++;
                } 
            }

            pw.close();
            wb.write(out);
            out.close();

            String uuidZIP = UUID.randomUUID().toString();
            File fZIP = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuidZIP);
            try {
                byte[] buffer = new byte[1024];

                FileOutputStream fos = new FileOutputStream(fZIP);
                ZipOutputStream zos = new ZipOutputStream(fos);

//                Append CSV
                ZipEntry ze = new ZipEntry("dbi_" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) + ".txt");
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(fCSV);
                int len;
                while ((len = in.read(buffer))>0) zos.write(buffer, 0, len);
                in.close();

//                Append CSV
                ze = new ZipEntry("dbi_" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) + ".xls");
                zos.putNextEntry(ze);
                in = new FileInputStream(fXLS);
                while ((len = in.read(buffer))>0) zos.write(buffer, 0, len);
                in.close();

                zos.closeEntry();
                zos.close();
                InputStream is = new FileInputStream(fZIP);
                Filedownload.save(is, "application/zip", "Payment-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
                fZIP.delete();
            } catch (Exception ex) {
                log.error("generate", ex);
            }

        } catch (Exception ex) {
            log.error("submit", ex);
        }
    }

    private void loadBiCodes() {
        biCodesMap.clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select * "
                    + "from imcs.dbo.bi_codes ";

            List<Object[]> l = s.createSQLQuery(q).list();
            for (Object[] o : l) {
                biCodesMap.put(Libs.nn(o[0]).toUpperCase(), Libs.nn(o[1]).toUpperCase());
            }
        } catch (Exception ex) {
            log.error("loadBiCodes", ex);
        } finally {
            s.close();
        }
    }

    private void order(List<String[]> records) {
//    	Messagebox.show("PROVIDER");
        Collections.sort(records, new Comparator() {
            public int compare(Object o1, Object o2) {
                String x1 = ((String[]) o1)[36];
                String x2 = ((String[]) o2)[36];
                return x1.compareTo(x2);
            }
        });
    }

    private void order2(List<String[]> records) {
        Collections.sort(records, new Comparator() {
            public int compare(Object o1, Object o2) {
                String x1 = ((String[]) o1)[36];
                String x2 = ((String[]) o2)[36];
                int c1 = x1.compareTo(x2);
                if (c1!=0) {
                    return c1;
                } else {
                    String x11 = ((String[]) o1)[26];
                    String x21 = ((String[]) o2)[26];
                    return x11.compareTo(x21);
                }
            }
        });
    }

}
