package com.imc.imctools.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.PagingEvent;

import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.tools.Libs;

/**
 * Created by faizal on 2/20/14.
 */
@SuppressWarnings("serial")
public class AnalyzeUploadFileController extends Window {

    private Logger log = LoggerFactory.getLogger(AnalyzeUploadFileController.class);
    private Listbox lbProducts;
    private Listbox lbMissing;
    private Listbox lbMismatch;
    private Listbox lbInconsistency;
    private Paging pgProducts;
    private Bandbox bbProduct;
    private String whereProducts;
    private Label lFilename;
    private Media media;
    private List<Object[]> hltdt1_as400;
    private List<Object[]> hltdt2_as400;
    private List<Object[]> hltemp_as400;
    private List<Object[]> hltpcf_as400;
    private int errors = 0;

    public void onCreate() {
        initComponents();
        populateProducts(0, pgProducts.getPageSize());
    }

    private void initComponents() {
        lbProducts = (Listbox) getFellow("lbProducts");
        lbMissing = (Listbox) getFellow("lbMissing");
        lbMismatch = (Listbox) getFellow("lbMismatch");
        lbInconsistency = (Listbox) getFellow("lbInconsistency");
        pgProducts = (Paging) getFellow("pgProducts");
        bbProduct = (Bandbox) getFellow("bbProduct");
        lFilename = (Label) getFellow("lFilename");

        pgProducts.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateProducts(evt.getActivePage()*pgProducts.getPageSize(), pgProducts.getPageSize());
            }
        });
    }

    private void populateProducts(int offset, int limit) {
        lbProducts.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
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

            if (whereProducts!=null) {
                if (!q3.isEmpty()) q3 += "and (" + whereProducts + ") ";
                else q3 += "where (" + whereProducts + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgProducts.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ProductPOJO e = new ProductPOJO();
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

    public void productSelected() {
        if (lbProducts.getSelectedCount()==1) {
            ProductPOJO productPOJO = lbProducts.getSelectedItem().getValue();
            bbProduct.setText(productPOJO.getProductName());
            bbProduct.setAttribute("e", productPOJO);
            populateProducts(0, pgProducts.getPageSize());
            bbProduct.close();

            loadData();
        }
    }

    public void quickSearchProducts() {
        Textbox tQuickSearchProducts = (Textbox) getFellow("tQuickSearchProducts");

        if (!tQuickSearchProducts.getText().isEmpty()) {
            whereProducts = "hhdrname like '%" + tQuickSearchProducts.getText() + "%' or "
                    + "convert(varchar,hhdrpono) like '%" + tQuickSearchProducts.getText() + "%' ";
            populateProducts(0, pgProducts.getPageSize());
        } else {
            refreshProducts();
        }
    }

    public void refreshProducts() {
        whereProducts = null;
        populateProducts(0, pgProducts.getPageSize());
    }

    public void fileUploaded(UploadEvent evt) {
        media = evt.getMedia();
        lFilename.setValue(media.getName());
    }

    private void loadData() {
        ProductPOJO productPOJO = (ProductPOJO) bbProduct.getAttribute("e");

//        Load from as400
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select * "
                    + "from idnhltpf.dbo.hltdt1 "
                    + "where "
                    + "hdt1yy=" + productPOJO.getProductYear() + " "
                    + "and hdt1pono=" + productPOJO.getProductId() + " "
                    + "and hdt1ctr=0 ";

            hltdt1_as400 = s.createSQLQuery(q).list();
            s.flush();

            q = "select * "
                    + "from idnhltpf.dbo.hltdt2 "
                    + "where "
                    + "hdt2yy=" + productPOJO.getProductYear() + " "
                    + "and hdt2pono=" + productPOJO.getProductId() + " "
                    + "and hdt2ctr=0 ";

            hltdt2_as400 = s.createSQLQuery(q).list();
        } catch (Exception ex) {
            log.error("loadData", ex);
        } finally {
            s.close();
        }
    }

    public void analyze() {
        errors = 0;
        lbMissing.getItems().clear();
        try {
            HSSFWorkbook wb = new HSSFWorkbook(media.getStreamData());
            HSSFSheet sheet = wb.getSheetAt(0);
            for (int i=1; i<sheet.getLastRowNum()+1; i++) {
                HSSFRow row = sheet.getRow(i);

                if (row!=null && row.getCell(0)!=null && !Libs.nn(Libs.getCellAt(row, 0)).isEmpty()) {
                    int index = Integer.valueOf(Libs.nn(Libs.getCellAt(row, 0)).replace(".0", ""));
                    String sequence = Libs.nn(Libs.getCellAt(row, 1));
                    String name = Libs.nn(Libs.getCellAt(row, 2));
                    String sex = Libs.nn(Libs.getCellAt(row, 3));

                    String bdt = "";
                    HSSFCell cellBDT = row.getCell(5);
                    if (cellBDT.getCellType()==HSSFCell.CELL_TYPE_STRING) {
                        bdt = Libs.nn(Libs.getCellAt(row, 5));
                    } else {
                        if (HSSFDateUtil.isCellDateFormatted(cellBDT)) {
                            bdt = new SimpleDateFormat("yyyyMMdd").format(cellBDT.getDateCellValue());
                        } else {
                            bdt = Libs.nn(Libs.getCellAt(row, 5));
                        }
                    }

                    String sdt = "";
                    HSSFCell cellSDT = row.getCell(6);
                    if (cellSDT.getCellType()==HSSFCell.CELL_TYPE_STRING) {
                        sdt = Libs.nn(Libs.getCellAt(row, 6));
                    } else {
                        if (HSSFDateUtil.isCellDateFormatted(cellSDT)) {
                            sdt = new SimpleDateFormat("yyyyMMdd").format(cellSDT.getDateCellValue());
                        } else {
                            sdt = Libs.nn(Libs.getCellAt(row, 6));
                        }
                    }

                    Object[] o = new Object[] {
                            index,
                            sequence,
                            name,
                            sex,
                            bdt,
                            sdt
                    };

                    checkMissing(o);
                    checkMismatch(o);
                    checkInconsistency(o);
                }

                Messagebox.show("Process completed. " + errors + " errors found", "Information", Messagebox.OK, Messagebox.INFORMATION);            }
        } catch (Exception ex) {
            log.error("analyze", ex);
        }
    }

    private void checkMissing(Object[] e) {
        boolean missing = true;

        for (Object[] o : hltdt1_as400) {
            int index = Integer.valueOf(Libs.nn(o[5]).replace(".0", ""));
            String sequence = Libs.nn(o[6]);

            if ((index==(Integer) e[0]) && sequence.equals(Libs.nn(e[1]))) {
                missing = false;
                break;
            }
        }

        if (missing) {
            logMissing(e);
        }
    }

    private void checkMismatch(Object[] e) {
        for (Object[] o : hltdt1_as400) {
            int index = Integer.valueOf(Libs.nn(o[5]).replace(".0", ""));
            String sequence = Libs.nn(o[6]);

            if ((index==(Integer) e[0]) && sequence.equals(Libs.nn(e[1]))) {

//                Check name
                if (!Libs.nn(o[9]).trim().equals(Libs.nn(e[2]).trim())) logMismatch(e, "Name", Libs.nn(o[9]).trim(), Libs.nn(e[2]).trim());

//                Check sex
                if (!Libs.nn(o[17]).trim().equals(Libs.nn(e[3]))) logMismatch(e, "Sex", Libs.nn(o[17]), Libs.nn(e[3]));

//                Check bdt
                String bdt = Libs.fixDate(Libs.nn(o[10]) + "-" + Libs.nn(o[11]) + "-" + Libs.nn(o[12]));
                if (!bdt.equals(Libs.nn(e[4]))) logMismatch(e, "Birthdate", bdt, Libs.nn(e[4]));
            }
        }

        for (Object[] o : hltdt2_as400) {
            int index = Integer.valueOf(Libs.nn(o[5]).replace(".0", ""));
            String sequence = Libs.nn(o[6]);
        }
    }

    private void checkInconsistency(Object[] e) {
        boolean inconsistency;

//        Check A sequence exists
        inconsistency = true;
        for (Object[] o : hltdt1_as400) {
            int index = Integer.valueOf(Libs.nn(o[5]).replace(".0", ""));
            String sequence = Libs.nn(o[6]);

            if ((index==(Integer) e[0]) && sequence.equals("A")) {
                inconsistency = false;
                break;
            }
        }
        if (inconsistency) logInconsistency(e, "Index A not found");
    }

    private void logMissing(Object[] o) {
        errors++;
        Listitem li = new Listitem();
        li.appendChild(new Listcell(Libs.nn(o[0])));
        li.appendChild(new Listcell(Libs.nn(o[1])));
        li.appendChild(new Listcell(Libs.nn(o[2])));
        lbMissing.appendChild(li);
    }

    private void logMismatch(Object[] o, String reason, String value1, String value2) {
        errors++;
        Listitem li = new Listitem();
        li.appendChild(new Listcell(Libs.nn(o[0])));
        li.appendChild(new Listcell(Libs.nn(o[1])));
        li.appendChild(new Listcell(Libs.nn(o[2])));
        li.appendChild(new Listcell(reason));
        li.appendChild(new Listcell(value1));
        li.appendChild(new Listcell(value2));
        lbMismatch.appendChild(li);
    }

    private void logInconsistency(Object[] o, String reason) {
        errors++;
        Listitem li = new Listitem();
        li.appendChild(new Listcell(Libs.nn(o[0])));
        li.appendChild(new Listcell(Libs.nn(o[1])));
        li.appendChild(new Listcell(Libs.nn(o[2])));
        li.appendChild(new Listcell(reason));
        lbInconsistency.appendChild(li);
    }

    public void save() {
        String uuid = UUID.randomUUID().toString();
        File f = new File(Libs.nn(Libs.config.get("temp_dir")) + File.separator + uuid);
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheetMissing = wb.createSheet("Missing");
        HSSFSheet sheetMismatch = wb.createSheet("Mismatch");
        HSSFSheet sheetInconsistency = wb.createSheet("Inconsistency");

        int i = 1;
        Libs.createRow(sheetMissing.createRow(0), new Object[] { "INDEX", "SEQUENCE", "NAME" }, null);
        for (Listitem li : lbMismatch.getItems()) {
            String index = ((Listcell) li.getChildren().get(0)).getLabel();
            String sequence = ((Listcell) li.getChildren().get(1)).getLabel();
            String name = ((Listcell) li.getChildren().get(2)).getLabel();

            HSSFRow row = sheetMissing.createRow(i);
            Libs.createRow(row, new Object[] { Integer.valueOf(index), sequence, name }, null);
            i++;
        }

        i = 1;
        Libs.createRow(sheetMismatch.createRow(0), new Object[] { "INDEX", "SEQUENCE", "NAME", "REASON", "DB", "FILE" }, null);
        for (Listitem li : lbMismatch.getItems()) {
            String index = ((Listcell) li.getChildren().get(0)).getLabel();
            String sequence = ((Listcell) li.getChildren().get(1)).getLabel();
            String name = ((Listcell) li.getChildren().get(2)).getLabel();
            String reason = ((Listcell) li.getChildren().get(3)).getLabel();
            String db = ((Listcell) li.getChildren().get(4)).getLabel();
            String file = ((Listcell) li.getChildren().get(5)).getLabel();

            HSSFRow row = sheetMismatch.createRow(i);
            Libs.createRow(row, new Object[] { Integer.valueOf(index), sequence, name, reason, db, file }, null);
            i++;
        }

        i = 1;
        Libs.createRow(sheetInconsistency.createRow(0), new Object[] { "INDEX", "SEQUENCE", "NAME", "REASON" }, null);
        for (Listitem li : lbInconsistency.getItems()) {
            String index = ((Listcell) li.getChildren().get(0)).getLabel();
            String sequence = ((Listcell) li.getChildren().get(1)).getLabel();
            String name = ((Listcell) li.getChildren().get(2)).getLabel();
            String reason = ((Listcell) li.getChildren().get(3)).getLabel();

            HSSFRow row = sheetInconsistency.createRow(i);
            Libs.createRow(row, new Object[] { Integer.valueOf(index), sequence, name, reason }, null);
            i++;
        }

        try {
            FileOutputStream out = new FileOutputStream(f);
            wb.write(out);
            out.close();

            InputStream is = new FileInputStream(f);
            Filedownload.save(is, "application/excel", "AnalyzeUploadFile-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xls");
            f.delete();
        } catch (Exception ex) {
            log.error("save", ex);
        }
    }

}
