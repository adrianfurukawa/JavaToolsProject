package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ProviderPOJO;
import com.imc.imctools.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by faizal on 12/19/13.
 */
public class ReceiptListByProvider extends Window {

    private Logger log = LoggerFactory.getLogger(ReceiptListByProvider.class);
    private Listbox lb;
    private Paging pg;
    private Bandbox bbProviderName;
    private Combobox cbScope;
    private Datebox dStartDate;
    private Datebox dEndDate;
    private String where;

    public void onCreate() {
        initComponents();
        populate(0, pg.getPageSize());
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");
        bbProviderName = (Bandbox) getFellow("bbProviderName");
        cbScope = (Combobox) getFellow("cbScope");
        dStartDate = (Datebox) getFellow("dStartDate");
        dEndDate = (Datebox) getFellow("dEndDate");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage() * pg.getPageSize(), pg.getPageSize());
            }
        });

        cbScope.setSelectedIndex(0);

        dStartDate.setValue(new Date());
        dEndDate.setValue(new Date());
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";

            String q1 = "select "
                    + "a.hpronomor, a.hproname ";

            String q2 = "from idnhltpf.dbo.hltpro a ";

            String q3 = "[where] ";

            String q4 = "order by a.hproname asc ";

            if (where!=null) {
                q3 = q3.replace("[where]", "where " + where + " ");
            } else {
                q3 = q3.replace("[where]", "");
            }

            Integer i = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pg.setTotalSize(i);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ProviderPOJO providerPOJO = new ProviderPOJO();
                providerPOJO.setProviderCode(Integer.valueOf(Libs.nn(o[0])));
                providerPOJO.setName(Libs.nn(o[1]).trim());

                Listitem li = new Listitem();
                li.setValue(providerPOJO);
                li.appendChild(new Listcell(String.valueOf(providerPOJO.getProviderCode())));
                li.appendChild(new Listcell(providerPOJO.getName()));
                lb.appendChild(li);
            }

        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s.close();
        }
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {

            where = "a.hpronomor like '%" + val + "%' "
                    + "or a.hproname like '%" + val + "%' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void refresh() {
        where = null;
        pg.setActivePage(0);
        populate(0, pg.getPageSize());
    }

    public void lbSelected() {
        ProviderPOJO providerPOJO = lb.getSelectedItem().getValue();
        bbProviderName.setAttribute("e", providerPOJO);
        bbProviderName.setText(providerPOJO.getName());
        bbProviderName.close();
    }

    public void export() {
        ProviderPOJO providerPOJO = (ProviderPOJO) bbProviderName.getAttribute("e");

        Session s = Libs.sfDB.openSession();
        try {
            String uuid = UUID.randomUUID().toString();

            String q1 = "select "
                    + "a.nointernal, a.nokwitansi, a.tglkwitansi, a.nmprov, "
                    + "(select count(*) from aso.dbo.preterimaprov_dtlins b where b.nointernal=a.nointernal) as insurances, "
                    + "(select top 1 c.tipe from aso.dbo.preterimaprov_dtlins c where c.nointernal=a.nointernal) as claim_type, "
                    + "a.tglinput, a.totaltagihan ";

            String q2 = "from aso.dbo.preterimaprov a ";

            String q3 = "where "
                    + "a.tgladmprov > '2011-01-01' "
                    + "and a.tgladmprov < {fn now()} "
                    + "and kdprov='" + providerPOJO.getProviderCode() + "' ";

            String q4 = "order by a.tgladmprov desc ";

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).list();

            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("Receipts");
            HSSFSheet sheet2 = wb.createSheet("With Detail");

            int i = 0;
            int i2 = 0;

            HSSFRow rowHeader = sheet.createRow(i);
            Libs.createRow(rowHeader, new Object[] { "Internal #", "Receipt #", "Date", "Provider Name", "Type", "Input Date", "Processed", "Outstanding", "Proposed", "Paid" }, null);

            HSSFRow rowHeader2 = sheet2.createRow(i);
            Libs.createRow(rowHeader2, new Object[] { "Internal #", "Receipt #", "Date", "Provider Name", "Type", "Input Date", "Processed", "Outstanding", "Proposed", "Paid" }, null);

            for (Object[] o : l) {
                i++;
                i2++;

                Double[] processed = ReceiptListController.processed(Libs.nn(o[0]), Libs.nn(o[5]));
                String processedStatus = "no";
                if (processed[0]>0) processedStatus = "yes";

                HSSFRow row = sheet2.createRow(i);
                HSSFRow row2 = sheet.createRow(i2);

                CellStyle style = wb.createCellStyle();
                style.setFillBackgroundColor(IndexedColors.AQUA.getIndex());
                row.setRowStyle(style);

                Libs.createRow(row, new Object[] {
                        Libs.nn(o[0]),
                        Libs.nn(o[1]),
                        Libs.nn(o[2]),
                        Libs.nn(o[3]),
                        Libs.nn(o[5]),
                        Libs.nn(o[6]),
                        processedStatus,
                        processed[1],
                        o[7],
                        processed[2]
                }, style);

                Libs.createRow(row2, new Object[] {
                        Libs.nn(o[0]),
                        Libs.nn(o[1]),
                        Libs.nn(o[2]),
                        Libs.nn(o[3]),
                        Libs.nn(o[5]),
                        Libs.nn(o[6]),
                        processedStatus,
                        processed[1],
                        o[7],
                        processed[2]
                }, style);

                i++;
                Libs.createRow(sheet2.createRow(i), new Object[]{
                        "",
                        "HID #",
                        "Policy #",
                        "Company",
                        "Proposed",
                        "Paid",
                        "Payment Date"
                }, null);

                Session s2 = Libs.sfDB.openSession();
                try {
                    String qry = "select "
                            + "(convert(varchar,thn_polis)+'-'+convert(varchar,br_polis)+'-'+convert(varchar,dist_polis)+'-'+convert(varchar,no_polis)) as policyString, "
                            + "c.hhdrname, "
                            + "a.total_tagihan_polis, "
                            + "b.dibayar, "
                            + "b.tglpenarikan, "
                            + "a.no_hid "
                            + "from aso.dbo.pre_" + Libs.nn(o[5]) + "_provider a "
                            + "left outer join idnhltpf.dbo.fin_paid b on b.hclmcno='IDN/'+a.no_hid "
                            + "inner join idnhltpf.dbo.hlthdr c on c.hhdryy=a.thn_polis and c.hhdrpono=a.no_polis "
                            + "where "
                            + "a.nointernal=" + Libs.nn(o[0]) + " "
                            + "and a.flg=1 "
                            + "and b.updateas400=1 ";

                    List<Object[]> l2 = s2.createSQLQuery(qry).list();
                    for (Object[] o2 : l2) {
                        i++;
                        Libs.createRow(sheet2.createRow(i), new Object[] {
                                "",
                                Libs.nn(o2[5]),
                                Libs.nn(o2[0]),
                                Libs.nn(o2[1]).trim(),
                                (o2[2]!=null) ? o2[2] : "",
                                (o2[3]!=null) ? o2[3] : "",
                                (o2[4]!=null) ? Libs.nn(o2[4]).substring(0, 10) : ""
                        }, null);
                    }
                } catch (Exception ex) {
                    log.error("createDetail", ex);
                } finally {
                    s2.close();
                }

                i++;
                Libs.createRow(sheet2.createRow(i), new Object[] { "" }, null);
            }

            File f = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid + ".xls");
            FileOutputStream fos = new FileOutputStream(f);
            wb.write(fos);
            fos.close();

            InputStream is = new FileInputStream(f);
            Filedownload.save(is, "application/xls", "ReceiptList-" + bbProviderName.getText() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xls");
            f.delete();
        } catch (Exception ex) {
            log.error("export", ex);
        } finally {
            s.close();
        }
    }

    public void bbProviderNameOpen(OpenEvent evt) {
        if (evt.isOpen()) {
            ((Textbox) getFellow("tQuickSearch")).setFocus(true);
        }
    }

}
