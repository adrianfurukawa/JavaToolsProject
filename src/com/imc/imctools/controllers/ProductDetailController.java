package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Calendar;

/**
 * Created by faizal on 1/20/14.
 */
public class ProductDetailController extends Window {

    private Logger log = LoggerFactory.getLogger(ProductDetailController.class);
    private Listbox lbMembers;
    private Listbox lbClaims;
    private Listbox lbLabels;
    private Listbox lbPlans;
    private Listbox lbBenefits;
    private Listbox lbHLTGAJISUH;
    private Paging pgMembers;
    private Paging pgClaims;
    private Paging pgHLTGAJISUH;
    private ProductPOJO productPOJO;
    private String whereMembers;
    private String whereClaims;
    private String whereHLTGAJISUH;

    public void onCreate() {
        productPOJO = (ProductPOJO) getAttribute("productPOJO");

        initComponents();
        populateInformation();
        populatePlans();
        populateLabels();
        populateMembers(0, pgMembers.getPageSize());
        populateClaims(0, pgClaims.getPageSize());
        populateHLTGAJISUH(0, pgHLTGAJISUH.getPageSize());
    }

    private void initComponents() {
        lbMembers = (Listbox) getFellow("lbMembers");
        lbClaims = (Listbox) getFellow("lbClaims");
        lbPlans = (Listbox) getFellow("lbPlans");
        lbBenefits = (Listbox) getFellow("lbBenefits");
        lbLabels = (Listbox) getFellow("lbLabels");
        lbHLTGAJISUH = (Listbox) getFellow("lbHLTGAJISUH");
        pgMembers = (Paging) getFellow("pgMembers");
        pgClaims = (Paging) getFellow("pgClaims");
        pgHLTGAJISUH = (Paging) getFellow("pgHLTGAJISUH");

        pgMembers.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateMembers(evt.getActivePage()*pgMembers.getPageSize(), pgMembers.getPageSize());
            }
        });

        pgClaims.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateClaims(evt.getActivePage() * pgClaims.getPageSize(), pgClaims.getPageSize());
            }
        });

        pgHLTGAJISUH.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateHLTGAJISUH(evt.getActivePage() * pgHLTGAJISUH.getPageSize(), pgHLTGAJISUH.getPageSize());
            }
        });
    }

    private void populateInformation() {
        Session s = Libs.sfDB.openSession();
        try {
            String q1 = "select "
                    + "a.hhdrname, b.hinsname, "
                    + "(convert(varchar,a.hhdrsdtyy)+'-'+convert(varchar,a.hhdrsdtmm)+'-'+convert(varchar,a.hhdrsdtdd)) as sdt, "
                    + "(convert(varchar,a.hhdredtyy)+'-'+convert(varchar,a.hhdredtmm)+'-'+convert(varchar,a.hhdredtdd)) as edt, "
                    + "(convert(varchar,a.hhdrmdtyy)+'-'+convert(varchar,a.hhdrmdtmm)+'-'+convert(varchar,a.hhdrmdtdd)) as mdt ";
            String q2 = "from idnhltpf.dbo.hlthdr a "
                    + "inner join idnhltpf.dbo.hltins b on b.hinsid=a.hhdrinsid ";
            String q3 = "where "
                    + "a.hhdryy=" + productPOJO.getProductYear() + " "
                    + "and a.hhdrpono=" + productPOJO.getProductId() + " ";

            Object[] o = (Object[]) s.createSQLQuery(q1 + q2 + q3).uniqueResult();

            ((Label) getFellow("lProductNumber")).setValue(productPOJO.getProductYear() + "-1-0-" + productPOJO.getProductId());
            ((Label) getFellow("lProductName")).setValue(Libs.nn(o[0]).trim());
            ((Label) getFellow("lClient")).setValue(Libs.nn(o[1]).trim());
            ((Label) getFellow("lStartingDate")).setValue(Libs.nn(o[2]).trim());
            ((Label) getFellow("lEffectiveDate")).setValue(Libs.nn(o[3]).trim());
            ((Label) getFellow("lMatureDate")).setValue(Libs.nn(o[4]).trim());
        } catch (Exception ex) {
            log.error("populateInformation", ex);
        } finally {
            s.close();
        }
    }

    private void populatePlans() {
        lbPlans.getItems().clear();
        lbBenefits.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q1 = "select "
                    + "hbftcode, hbftlimit, hbftlmtamt, hbftreim, "
                    + Libs.runningFields("hbftbcd", 30) + ", "
                    + Libs.runningFields("hbftbpln", 30) + " ";
            String q2 = "from "
                    + "idnhltpf.dbo.hltbft ";
            String q3 = "where "
                    + "hbftyy=" + productPOJO.getProductYear() + " "
                    + "and hbftpono=" + productPOJO.getProductId() + " ";

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.setValue(o);
                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[1])));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[2])), "#,###.00"));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[3])), "#"));
                lbPlans.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populatePlans", ex);
        } finally {
            s.close();
        }
    }

    private void populateLabels() {
        lbLabels.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "hlblcode, hlblttl, hlblname, hlbladdr "
                    + "from idnhltpf.dbo.hltlbl "
                    + "where "
                    + "hlblyy=" + productPOJO.getProductYear() + " "
                    + "and hlblpono=" + productPOJO.getProductId() + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.setValue(o);
                li.appendChild(new Listcell(Libs.nn(o[0]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[1]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[2]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[3]).trim()));
                lbLabels.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateLabels", ex);
        } finally {
            s.close();
        }
    }

    private void populateBenefits(Object[] o) {
        String benefitCodes = "";

        for (int i=0; i<30; i++) {
            String benefitCode = Libs.nn(o[i+4]).trim();
            if (!benefitCode.isEmpty()) benefitCodes += "'" + benefitCode + "', ";
        }

        if (benefitCodes.endsWith(", ")) benefitCodes = benefitCodes.substring(0, benefitCodes.length()-2);

        Map<String,String> benefitDescriptionMap = Libs.getBenefitDescriptionMap(benefitCodes);

        lbBenefits.getItems().clear();
        for (int i=0; i<30; i++) {
            String benefitCode = Libs.nn(o[i+4]).trim();
            if (!benefitCode.isEmpty()) {
                Double limit = Double.valueOf(Libs.nn(o[i+34]));
                Listitem li = new Listitem();
                li.appendChild(new Listcell(benefitCode));
                li.appendChild(new Listcell(benefitDescriptionMap.get(benefitCode)));
                li.appendChild(Libs.createNumericListcell(limit, "#,###.00"));
                lbBenefits.appendChild(li);
            }
        }
    }

    private void populateMembers(int offset, int limit) {
        lbMembers.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.hdt1idxno, a.hdt1seqno, a.hdt1ncard, a.hdt1name ";
            String q2 = "from idnhltpf.dbo.hltdt1 a ";
            String q3 = "where "
                    + "a.hdt1ctr=0 ";
            String q4 = "order by a.hdt1idxno asc, a.hdt1seqno asc ";

            if (productPOJO!=null) {
                q3 += "and a.hdt1yy=" + productPOJO.getProductYear() + " "
                        + "and a.hdt1pono=" + productPOJO.getProductId() + " ";
            }

            if (whereMembers!=null) {
                q3 += "and (" + whereMembers + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgMembers.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.setValue(o);
                li.appendChild(new Listcell(Libs.nn(o[0]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[1]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[2]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[3]).trim()));
                lbMembers.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateMembers", ex);
        } finally {
            s.close();
        }
    }

    private void populateClaims(int offset, int limit) {
        lbClaims.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.hclmtclaim, a.hclmcno, "
                    + "convert(varchar,a.hclmcdatey)+'-'+convert(varchar,a.hclmcdatem)+'-'+convert(varchar,a.hclmcdated) as cdt, "
                    + "(a.hclmdiscd1 + ', ' + a.hclmdiscd2 + ', ' + a.hclmdiscd3) as icd ";
            String q2 = "from idnhltpf.dbo.hltclm a ";
            String q3 = "where "
                    + "a.hclmrecid<>'C' ";
            String q4 = "order by convert(varchar,a.hclmcdatey)+'-'+convert(varchar,a.hclmcdatem)+'-'+convert(varchar,a.hclmcdated) desc ";

            if (productPOJO!=null) {
                q3 += "and a.hclmyy=" + productPOJO.getProductYear() + " "
                        + "and a.hclmpono=" + productPOJO.getProductId() + " ";
            }

            if (whereClaims!=null) {
                q3 += "and (" + whereClaims + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgClaims.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String icds = "";
                String[] icdseg = Libs.nn(o[3]).split("\\,");
                for (String icd : icdseg) {
                    if (!icd.trim().isEmpty()) icds += icd.trim() + ", ";
                }
                if (icds.endsWith(", ")) icds = icds.substring(0, icds.length()-2);

                Listitem li = new Listitem();
                li.setValue(o);
                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[1])));
                li.appendChild(new Listcell(Libs.nn(o[2])));
                li.appendChild(new Listcell(icds));
                lbClaims.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateClaims", ex);
        } finally {
            s.close();
        }
    }

    private void populateHLTGAJISUH(int offset, int limit) {
        lbHLTGAJISUH.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.hgajiidxno, a.hgajiseqno, a.hgajilmt, b.hdt1ncard, b.hdt1name ";
            String q2 = "from idnhltpf.dbo.hltgajisuh a "
                    + "inner join idnhltpf.dbo.hltdt1 b on b.hdt1yy=a.hgajiyy and b.hdt1pono=a.hgajipono and b.hdt1idxno=a.hgajiidxno and b.hdt1seqno=a.hgajiseqno and b.hdt1ctr=0 ";
            String q3 = "";
            String q4 = "order by a.hgajiidxno asc ";

            if (productPOJO!=null) {
                q3 += "where a.hgajiyy=" + productPOJO.getProductYear() + " "
                        + "and a.hgajipono=" + productPOJO.getProductId() + " ";
            }

            if (whereClaims!=null) {
                q3 += "and (" + whereHLTGAJISUH + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgHLTGAJISUH.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.setValue(o);
                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[1])));
                li.appendChild(new Listcell(Libs.nn(o[3])));
                li.appendChild(new Listcell(Libs.nn(o[4])));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[2])), "#,###.00"));
                lbHLTGAJISUH.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateHLTGAJISUH", ex);
        } finally {
            s.close();
        }
    }

    public void refreshMembers() {
        whereMembers = null;
        populateMembers(0, pgMembers.getPageSize());
    }

    public void refreshClaims() {
        whereClaims = null;
        populateClaims(0, pgClaims.getPageSize());
    }

    public void refreshHLTGAJISUH() {
        whereHLTGAJISUH = null;
        populateHLTGAJISUH(0, pgHLTGAJISUH.getPageSize());
    }

    public void quickSearchMembers() {
        String val = ((Textbox) getFellow("tQuickSearchMembers")).getText();
        if (!val.isEmpty()) {
            whereMembers = "a.hdt1name like '%" + val + "%' or "
                    + "a.hdt1ncard like '%" + val + "%' ";

            populateMembers(0, pgMembers.getPageSize());
        } else refreshMembers();
    }

    public void quickSearchClaims() {
        String val = ((Textbox) getFellow("tQuickSearchClaims")).getText();
        if (!val.isEmpty()) {
            whereClaims = "a.hclmcno like '%" + val + "%' ";

            populateClaims(0, pgClaims.getPageSize());
        } else refreshClaims();
    }

    public void quickSearchHLTGAJISUH() {
        String val = ((Textbox) getFellow("tQuickSearchHLTGAJISUH")).getText();
        if (!val.isEmpty()) {
            whereHLTGAJISUH = "b.hdt1name like '%" + val + "%' "
                    + "or b.hdt1ncard like '%" + val + "%' ";

            populateHLTGAJISUH(0, pgHLTGAJISUH.getPageSize());
        } else refreshHLTGAJISUH();
    }

    public void planSelected() {
        if (lbPlans.getSelectedCount()>0) {
            populateBenefits((Object[]) lbPlans.getSelectedItem().getValue());
        }
    }

    public void renewProduct() {
        Window w = (Window) Executions.createComponents("views/RenewProduct.zul", this, null);
        w.setAttribute("productPOJO", productPOJO);
        w.doModal();
    }

//    Utilities

    public void fixHLTGAJISUH() {
        if (Messagebox.show("You are going to execute FixHLTGAJISUH utility. Proceed?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            Session s = Libs.sfDB.openSession();
            List<Object[]> l = null;

            try {
                String q = "select "
                        + "hdt1idxno, hdt1seqno "
                        + "from idnhltpf.dbo.hltdt1 a "
                        + "left outer join idnhltpf.dbo.hltgajisuh b on a.hdt1yy=b.hgajiyy and a.hdt1pono=b.hgajipono and a.hdt1idxno=b.hgajiidxno and a.hdt1seqno=b.hgajiseqno "
                        + "where "
                        + "a.hdt1yy=" + productPOJO.getProductYear() + " "
                        + "and a.hdt1pono=" + productPOJO.getProductId() + " "
                        + "and b.hgajiyy is null "
                        + "and a.hdt1seqno<>'A' ";

                l = s.createSQLQuery(q).list();
            } catch (Exception ex) {
                log.error("fixHLTGAJISUH", ex);
            } finally {
                s.close();
            }

            int fc = 0;
            if (l!=null) {
                for (Object[] o : l) {
                    String q = "insert into [DB] ("
                            + "hgajiid, "
                            + "hgajiyy, hgajibr, hgajidist, hgajipono, "
                            + "hgajiidxno, hgajiseqno, "
                            + "hgajiamt, hgajilmt, "
                            + "hgajiupdyy, hgajiupdmm, hgajiupddd, "
                            + "hgajiusrid "
                            + ") select "
                            + "hgajiid, "
                            + "hgajiyy, hgajibr, hgajidist, hgajipono, "
                            + "hgajiidxno, '" + o[1] + "', "
                            + "hgajiamt, hgajilmt, "
                            + "hgajiupdyy, hgajiupdmm, hgajiupddd, "
                            + "hgajiusrid "
                            + "from [DB] "
                            + "where "
                            + "hgajiyy=" + productPOJO.getProductYear() + " "
                            + "and hgajipono=" + productPOJO.getProductId() + " "
                            + "and hgajiidxno=" + o[0] + " "
                            + "and hgajiseqno='A' ";

                    s = Libs.sfDB.openSession();
                    try {
                        s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltgajisuh")).executeUpdate();
                        s.beginTransaction().commit();
                        s.flush();
                        fc++;
                    } catch (Exception ex) {
                        log.error("fixHLTGAJISUH", ex);
                    } finally {
                        s.close();
                    }
                }
            }

            Messagebox.show("Process completed. " + fc + " records fixed.", "Information", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    public void fixKonversi() {
        if (Messagebox.show("You are going to execute FixKonversi utility. Proceed?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            int fc = 0;
            List<Object[]> l = null;
            Session s = Libs.sfDB.openSession();
            try {
                String q = "select "
                        + "hdt2idxno, hdt2seqno "
                        + "from idnhltpf.dbo.hltdt2 "
                        + "where "
                        + "hdt2yy=" + productPOJO.getProductYear() + " "
                        + "and hdt2pono=" + productPOJO.getProductId() + " "
                        + "and hdt2ctr=99 "
                        + "and hdt2moe='K' ";

                l = s.createSQLQuery(q).list();
            } catch (Exception ex) {
                log.error("fixKonversi", ex);
            } finally {
                s.close();
            }

            if (l!=null) {
                for (Object[] o : l) {
                    s = Libs.sfDB.openSession();
                    try {
                        String q = "select count(*) "
                                + "from idnhltpf.dbo.hltdt2 "
                                + "where "
                                + "hdt2yy=" + productPOJO.getProductYear() + " "
                                + "and hdt2pono=" + productPOJO.getProductId() + " "
                                + "and hdt2idxno=" + o[0] + " "
                                + "and hdt2seqno='" + o[1] + "' "
                                + "and hdt2ctr=0 ";

                        Integer rc = (Integer) s.createSQLQuery(q).uniqueResult();

                        if (rc==0) {
                            q = "update [DB] "
                                    + "set "
                                    + "hdt2ctr=0, hdt2moe='' "
                                    + "where "
                                    + "hdt2yy=" + productPOJO.getProductYear() + " "
                                    + "and hdt2pono=" + productPOJO.getProductId() + " "
                                    + "and hdt2idxno=" + o[0] + " "
                                    + "and hdt2seqno='" + o[1] + "' "
                                    + "and hdt2ctr=99 ";

                            s = Libs.sfDB.openSession();
                            s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltdt2")).executeUpdate();
                            s.beginTransaction().commit();
                            Session s3 = Libs.sfEDC.openSession();

                            s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltdt2")).executeUpdate();
                            s.beginTransaction().commit();
                            fc++;
                        }
                    } catch (Exception ex) {
                        log.error("fixKonversi", ex);
                    } finally {
                        if (s!=null && s.isOpen()) s.close();
                    }
                }
            }
            Messagebox.show("Process completed. " + fc + " records fixed.", "Information", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    public void uploadLimitGaji(UploadEvent event) {
        Calendar cal = Calendar.getInstance();

        Media media = event.getMedia();
        if (Messagebox.show("You are going to upload " + media.getName() + ". Proceed?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            try {
//                Load source
                HSSFWorkbook wb = new HSSFWorkbook(media.getStreamData());
                HSSFSheet sheet = wb.getSheetAt(0);
                List<Object[]> source = new ArrayList<Object[]>();

                for (int i=1; i<sheet.getLastRowNum()+1; i++) {
                    HSSFRow row = sheet.getRow(i);
                    Object[] o = new Object[4];
                    o[0] = Integer.valueOf(Libs.nn(Libs.getCellAt(row, 0)).replace(".0", ""));
                    o[1] = Libs.getCellAt(row, 1);
                    o[2] = Libs.getCellAt(row, 2);
                    o[3] = new DecimalFormat("##########").format(Double.valueOf(Libs.nn(Libs.getCellAt(row, 3))));
                    source.add(o);
                }

//                Load members
                Session s = Libs.sfDB.openSession();
                String q = "select "
                        + "hdt1idxno, hdt1seqno "
                        + "from idnhltpf.dbo.hltdt1 "
                        + "where "
                        + "hdt1yy=" + productPOJO.getProductYear() + " "
                        + "and hdt1pono=" + productPOJO.getProductId() + " "
                        + "and hdt1ctr=0 ";
                List<Object[]> l = s.createSQLQuery(q).list();
                s.close();

//                Register source with members
                for (Object[] o : l) {
                    double limit = 0;
                    for (Object[] p : source) {
                        if (Libs.nn(o[0]).equals(Libs.nn(p[0]))) {
                            limit = Double.valueOf(Libs.nn(p[3]));

                            s = Libs.sfDB.openSession();
                            try {
                                q = "insert into [DB] ("
                                        + "hgajiid, "
                                        + "hgajiyy, hgajibr, hgajidist, hgajipono, "
                                        + "hgajiidxno, hgajiseqno, "
                                        + "hgajiamt, hgajilmt, "
                                        + "hgajiupdyy, hgajiupdmm, hgajiupddd, hgajiusrid "
                                        + ") values ("
                                        + "'" + p[2] + "', "
                                        + productPOJO.getProductYear() + ", 1, 0, " + productPOJO.getProductId() + ", "
                                        + p[0] + ", '" + p[1] + "', "
                                        + limit + ", " + limit + ", "
                                        + cal.get(Calendar.YEAR) + ", " + (cal.get(Calendar.MONTH)+1) + ", " + cal.get(Calendar.DATE) + ", 'FAIZAL' "
                                        + ") ";

                                s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltgajisuh")).executeUpdate();
                                s.beginTransaction().commit();
                            } catch (Exception ex) {
                                log.error("uploadLimitGaji", ex);
                            } finally {
                                s.close();
                            }
                        }
                    }
                }

                Messagebox.show("Limit Gaji has been uploaded", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (Exception ex) {
                log.error("uploadLimitGaji", ex);
            }
        }
    }

}
