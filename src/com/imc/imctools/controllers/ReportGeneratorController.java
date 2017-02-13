package com.imc.imctools.controllers;

import bsh.Interpreter;
import com.imc.imctools.pojo.ClientPOJO;
import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.pojo.ReportPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by faizal on 1/28/14.
 */
public class ReportGeneratorController extends Window {

    private Logger log = LoggerFactory.getLogger(ReportGeneratorController.class);
    private Listbox lbClients;
    private Listbox lbProducts;
    private Paging pgClients;
    private Bandbox bbClient;
    private Bandbox bbProduct;
    private String whereClients;
    private String whereProducts;
    private Row rowDateRange;
    private Row rowMonthRange;
    private Row rowYearRange;
    private Combobox cbPeriod;
    private Combobox cbReportType;
    private Combobox cbStartMonth;
    private Combobox cbEndMonth;
    private Datebox dStartDate;
    private Datebox dEndDate;
    private Spinner spnStartMonthYear;
    private Spinner spnEndMonthYear;
    private Spinner spnStartYear;
    private Spinner spnEndYear;
    private Tree tree;

    public void onCreate() {
        initComponents();

        populate();
        populateClients(0, pgClients.getPageSize());
    }

    private void initComponents() {
        lbClients = (Listbox) getFellow("lbClients");
        lbProducts = (Listbox) getFellow("lbProducts");
        pgClients = (Paging) getFellow("pgClients");
        bbClient = (Bandbox) getFellow("bbClient");
        bbProduct = (Bandbox) getFellow("bbProduct");
        rowDateRange = (Row) getFellow("rowDateRange");
        rowMonthRange = (Row) getFellow("rowMonthRange");
        rowYearRange = (Row) getFellow("rowYearRange");
        cbPeriod = (Combobox) getFellow("cbPeriod");
        cbStartMonth = (Combobox) getFellow("cbStartMonth");
        cbEndMonth = (Combobox) getFellow("cbEndMonth");
        cbReportType = (Combobox) getFellow("cbReportType");
        spnStartMonthYear = (Spinner) getFellow("spnStartMonthYear");
        spnEndMonthYear = (Spinner) getFellow("spnEndMonthYear");
        spnStartYear = (Spinner) getFellow("spnStartYear");
        spnEndYear = (Spinner) getFellow("spnEndYear");
        dStartDate = (Datebox) getFellow("dStartDate");
        dEndDate = (Datebox) getFellow("dEndDate");
        tree = (Tree) getFellow("tree");

        dStartDate.setValue(new Date());
        dEndDate.setValue(new Date());

        pgClients.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateClients(evt.getActivePage()*pgClients.getPageSize(), pgClients.getPageSize());
            }
        });

        final java.util.Calendar cal = java.util.Calendar.getInstance();

        cbPeriod.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                switch (cbPeriod.getSelectedIndex()) {
                    case 0:
                        rowDateRange.setVisible(true);
                        rowMonthRange.setVisible(false);
                        rowYearRange.setVisible(false);
                        dStartDate.setValue(new Date());
                        dEndDate.setValue(new Date());
                        break;
                    case 1:
                        rowDateRange.setVisible(false);
                        rowMonthRange.setVisible(true);
                        rowYearRange.setVisible(false);
                        cbStartMonth.setSelectedIndex(cal.get(java.util.Calendar.MONTH));
                        cbEndMonth.setSelectedIndex(cal.get(java.util.Calendar.MONTH));
                        spnStartMonthYear.setValue(cal.get(java.util.Calendar.YEAR));
                        spnEndMonthYear.setValue(cal.get(java.util.Calendar.YEAR));
                        break;
                    case 2:
                        rowDateRange.setVisible(false);
                        rowMonthRange.setVisible(false);
                        rowYearRange.setVisible(true);
                        spnStartYear.setValue(cal.get(java.util.Calendar.YEAR));
                        spnEndYear.setValue(cal.get(java.util.Calendar.YEAR));
                        break;
                }
            }
        });

        Libs.populateCombobox(cbStartMonth, Libs.months);
        Libs.populateCombobox(cbEndMonth, Libs.months);
    }

    private void populate() {
        File dirReportGenerator = new File(Libs.nn(Libs.config.get("report_generator_dir")));
        for (File f : dirReportGenerator.listFiles()) {

            String reportName = f.getName();
            String parent = f.getName();
            String status = "";
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                if (br.ready()) {
                    br.readLine();
                    parent = br.readLine().trim();
                    reportName = br.readLine().trim();
                    status = br.readLine().replace("Status:", "").trim();
                }
            } catch (Exception ex) {
                log.error("populate", ex);
            }

            Treeitem tiParent = (Treeitem) getFellow(parent);
            Treechildren tch = tiParent.getTreechildren();
            if (tch==null) {
                tch = new Treechildren();
                tiParent.appendChild(tch);
            }

            Treeitem ti = new Treeitem(reportName);
            ti.setValue(f);
            tch.appendChild(ti);
            if (status.toLowerCase().equals("inactive")) ti.setDisabled(true);
        }
    }

    private void populateClients(int offset, int limit) {
        lbClients.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "hinsid, hinsname, hinsdesc1 ";
            String q2 = "from idnhltpf.dbo.hltins ";
            String q3 = "";
            String q4 = "order by hinsname asc ";

            if (whereClients!=null) q3 = "where " + whereClients;

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgClients.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ClientPOJO e = new ClientPOJO();
                e.setClientId(Libs.nn(o[0]).trim());
                e.setClientName(Libs.nn(o[1]).trim());

                Listitem li = new Listitem();
                li.setValue(e);
                li.appendChild(new Listcell(e.getClientId()));
                li.appendChild(new Listcell(e.getClientName()));
                li.appendChild(new Listcell(Libs.nn(o[2]).trim()));
                lbClients.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateClients", ex);
        } finally {
            s.close();
        }
    }

    private void populateProducts() {
        ClientPOJO clientPOJO = (ClientPOJO) bbClient.getAttribute("e");
        lbProducts.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q1 = "select "
                    + "hhdrpono, hhdryy, hhdrname, "
                    + "(convert(varchar,hhdrsdtyy)+'-'+convert(varchar,hhdrsdtmm)+'-'+convert(varchar,hhdrsdtdd)) as sdt, "
                    + "(convert(varchar,hhdredtyy)+'-'+convert(varchar,hhdredtmm)+'-'+convert(varchar,hhdredtdd)) as edt, "
                    + "(convert(varchar,hhdrmdtyy)+'-'+convert(varchar,hhdrmdtmm)+'-'+convert(varchar,hhdrmdtdd)) as mdt, "
                    + "(convert(varchar,hhdradtyy)+'-'+convert(varchar,hhdradtmm)+'-'+convert(varchar,hhdradtdd)) as adt ";
            String q2 = "from "
                    + "idnhltpf.dbo.hlthdr ";
            String q3 = "";
            String q4 = "order by hhdryy desc, hhdrname asc ";

            if (clientPOJO!=null) {
                q3 = "where hhdrinsid='" + clientPOJO.getClientId() + "' ";
            }

            if (whereProducts!=null) {
                if (!q3.isEmpty()) q3 += "and (" + whereProducts + ") ";
                else q3 += "where (" + whereProducts + ") ";
            }

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).list();
            for (Object[] o : l) {
                ProductPOJO e = new ProductPOJO();
                e.setClientPOJO(clientPOJO);
                e.setProductId(Libs.nn(o[0]));
                e.setProductYear(Libs.nn(o[1]));
                e.setProductName(Libs.nn(o[2]));
                e.setStartingDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[3])));
                e.setEffectiveDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[4])));
                e.setMatureDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[5])));

                Listitem li = new Listitem();
                li.setValue(e);

                li.appendChild(new Listcell(e.getProductYear()));
                li.appendChild(new Listcell(e.getProductId()));
                li.appendChild(new Listcell(e.getProductName()));

                lbProducts.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateProducts", ex);
        } finally {
            s.close();
        }
    }

    public void clientSelected() {
        if (lbClients.getSelectedCount()==1) {
            ClientPOJO clientPOJO = lbClients.getSelectedItem().getValue();
            bbClient.setText(clientPOJO.getClientName());
            bbClient.setAttribute("e", clientPOJO);
            populateProducts();
            bbClient.close();
        }
    }

    public void productSelected() {
        if (lbProducts.getSelectedCount()>0) {
            List<ProductPOJO> e = new ArrayList<ProductPOJO>();
            for (Listitem li : lbProducts.getSelectedItems()) {
                ProductPOJO productPOJO = li.getValue();
                e.add(productPOJO);
            }

            String text = "";
            if (e.size()==1) {
                text = e.get(0).getProductName();
            } else {
                text = "(MULTIPLE PRODUCTS)";
            }
            bbProduct.setText(text);
            bbProduct.setAttribute("e", e);
            bbProduct.close();
        }
    }

    public void quickSearchClients() {
        Textbox tQuickSearchClients = (Textbox) getFellow("tQuickSearchClients");

        if (!tQuickSearchClients.getText().isEmpty()) {
            whereClients = "hinsname like '%" + tQuickSearchClients.getText() + "%' ";
            populateClients(0, pgClients.getPageSize());
            pgClients.setActivePage(0);
        } else {
            refreshClients();
        }
    }

    public void refreshClients() {
        whereClients = null;
        populateClients(0, pgClients.getPageSize());
    }

    public void quickSearchProducts() {
        Textbox tQuickSearchProducts = (Textbox) getFellow("tQuickSearchProducts");

        if (!tQuickSearchProducts.getText().isEmpty()) {
            whereProducts = "hhdrname like '%" + tQuickSearchProducts.getText() + "%' or "
                    + "convert(varchar,hhdrpono) like '%" + tQuickSearchProducts.getText() + "%' ";
            populateProducts();
        } else {
            refreshProducts();
        }
    }

    public void refreshProducts() {
        whereProducts = null;
        populateProducts();
    }

    public void generate() {
        if (tree.getSelectedCount()==0) {
            Messagebox.show("Please select at least one report to generate", "Error", Messagebox.OK, Messagebox.ERROR);
        } else {
            List<ProductPOJO> products = (ArrayList<ProductPOJO>) bbProduct.getAttribute("e");
            List<ReportPOJO> reports = new ArrayList<ReportPOJO>();

            for (ProductPOJO productPOJO : products) {
                for (Treeitem ti : tree.getSelectedItems()) {
                    if (ti.getValue()!=null) {
                        File f = ti.getValue();
                        if (f.exists()) {
                            String script = Libs.loadReportGenerator(f);
                            if (!script.isEmpty()) {
                                String uuid = UUID.randomUUID().toString();
                                File fuuid = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);

                                Interpreter interpreter = new Interpreter();
                                try {
                                    ReportPOJO reportPOJO = new ReportPOJO();
                                    reportPOJO.setFile(fuuid);

                                    interpreter.set("clientPOJO", bbClient.getAttribute("e"));
                                    interpreter.set("productPOJO", productPOJO);
                                    interpreter.set("period", cbPeriod.getText());
                                    interpreter.set("dStartDate", dStartDate);
                                    interpreter.set("dEndDate", dEndDate);
                                    interpreter.set("cbStartMonth", cbStartMonth);
                                    interpreter.set("cbEndMonth", cbEndMonth);
                                    interpreter.set("spnStartMonthYear", spnStartMonthYear);
                                    interpreter.set("spnEndMonthYear", spnEndMonthYear);
                                    interpreter.set("spnStartYear", spnStartYear);
                                    interpreter.set("spnEndYear", spnEndYear);
                                    interpreter.set("reportType", cbReportType.getText());
                                    interpreter.set("output", fuuid);
                                    interpreter.set("reportPOJO", reportPOJO);
                                    interpreter.set("log", log);
                                    interpreter.eval(script);

                                    reports.add(reportPOJO);
                                } catch (Exception ex) {
                                    log.error("generate", ex);
                                }
                            }
                        }
                    }
                }
            }

            if (reports.size()>1) {
                String uuid = UUID.randomUUID().toString();
                File zipFile = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);
                try {
                    byte[] buffer = new byte[1024];

                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    for (ReportPOJO reportPOJO : reports) {
                        ZipEntry ze = new ZipEntry(reportPOJO.getName());
                        zos.putNextEntry(ze);

                        FileInputStream in = new FileInputStream(reportPOJO.getFile());
                        int len;
                        while ((len = in.read(buffer))>0) {
                            zos.write(buffer, 0, len);
                        }

                        in.close();
                    }

                    zos.closeEntry();;
                    zos.close();
                    InputStream is = new FileInputStream(zipFile);
                    Filedownload.save(is, "application/zip", "ReportGenerator-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
                    zipFile.delete();
                } catch (Exception ex) {
                    log.error("generate", ex);
                }
            } else {
                ReportPOJO reportPOJO = reports.get(0);
                try {
                    InputStream is = new FileInputStream(reportPOJO.getFile());
                    Filedownload.save(is, "application/vnd.ms-excel", reportPOJO.getName());
                    reportPOJO.getFile().delete();
                } catch (Exception ex) {
                    log.error("generate", ex);
                }
            }
        }
    }

}
