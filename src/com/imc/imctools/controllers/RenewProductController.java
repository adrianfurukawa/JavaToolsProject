package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by faizal on 1/20/14.
 */
public class RenewProductController extends Window {

    private Logger log = LoggerFactory.getLogger(RenewProductController.class);
    private ProductPOJO productPOJO;
    private Bandbox bbClient;
    private Bandbox bbProduct;
    private Label lMatureDate;
    private Label lNewProductNumber;
    private Datebox dNewStartingDate;
    private Datebox dNewEffectiveDate;
    private Datebox dNewMatureDate;
    private Combobox cbRenewMembers;
    private Toolbarbutton btnRenew;

    public void onCreate() {
        productPOJO = (ProductPOJO) getAttribute("productPOJO");
        initComponents();
        populate();
    }

    private void initComponents() {
        bbClient = (Bandbox) getFellow("bbClient");
        bbProduct = (Bandbox) getFellow("bbProduct");
        lMatureDate = (Label) getFellow("lMatureDate");
        lNewProductNumber = (Label) getFellow("lNewProductNumber");
        dNewStartingDate = (Datebox) getFellow("dNewStartingDate");
        dNewEffectiveDate = (Datebox) getFellow("dNewEffectiveDate");
        dNewMatureDate = (Datebox) getFellow("dNewMatureDate");
        cbRenewMembers = (Combobox) getFellow("cbRenewMembers");
        btnRenew = (Toolbarbutton) getFellow("btnRenew");
    }

    private void populate() {
        if (productPOJO!=null) {
            bbClient.setAttribute("e", productPOJO.getClientPOJO());
            bbClient.setText(productPOJO.getClientPOJO().getClientName());
            bbProduct.setAttribute("e", productPOJO);
            bbProduct.setText(productPOJO.getProductName());
            lMatureDate.setValue(new SimpleDateFormat("yyyy-MM-dd").format(productPOJO.getMatureDate()));
            calculateRenewal();
        }
    }

    private void calculateRenewal() {
        Calendar calMature = Calendar.getInstance();
        Calendar calCurrent = Calendar.getInstance();
        calMature.setTime(productPOJO.getMatureDate());

        int diff = Libs.getDiffDays(calCurrent.getTime(), calMature.getTime());

        if (diff<=0) {
            Calendar calNewStartingDate = Calendar.getInstance();
            Calendar calNewEffectiveDate = Calendar.getInstance();
            Calendar calNewMatureDate = Calendar.getInstance();

            calNewStartingDate.setTime(productPOJO.getStartingDate());
            calNewEffectiveDate.setTime(productPOJO.getMatureDate());
            calNewMatureDate.setTime(productPOJO.getMatureDate());

            calNewMatureDate.add(Calendar.YEAR, 1);
            calNewEffectiveDate.add(Calendar.DATE, 1);

            dNewStartingDate.setValue(calNewStartingDate.getTime());
            dNewEffectiveDate.setValue(calNewEffectiveDate.getTime());
            dNewMatureDate.setValue(calNewMatureDate.getTime());

            lNewProductNumber.setValue((Integer.valueOf(productPOJO.getProductYear())+1) + "-" + productPOJO.getProductId());
        } else {
            Calendar calNewStartingDate = Calendar.getInstance();
            Calendar calNewEffectiveDate = Calendar.getInstance();
            Calendar calNewMatureDate = Calendar.getInstance();

            calNewStartingDate.setTime(productPOJO.getStartingDate());

            calNewEffectiveDate.setTime(productPOJO.getEffectiveDate());
            calNewEffectiveDate.add(Calendar.YEAR, 1);

            calNewMatureDate.setTime(productPOJO.getEffectiveDate());
            calNewMatureDate.add(Calendar.YEAR, 2);
            calNewMatureDate.add(Calendar.DATE, -1);

            dNewStartingDate.setValue(calNewStartingDate.getTime());
            dNewEffectiveDate.setValue(calNewEffectiveDate.getTime());
            dNewMatureDate.setValue(calNewMatureDate.getTime());

            lNewProductNumber.setValue((Integer.valueOf(productPOJO.getProductYear())+1) + "-" + productPOJO.getProductId());
        }

        if (!lNewProductNumber.getValue().isEmpty()) checkForRenewedVersion();
    }

    private void checkForRenewedVersion() {
        String[] newProductNumber = lNewProductNumber.getValue().split("\\-");
        boolean invalid = true;

        Session s = Libs.sfDB.openSession();
        try {
            String q = "select count(*) "
                    + "from idnhltpf.dbo.hlthdr "
                    + "where "
                    + "hhdryy=" + newProductNumber[0] + " "
                    + "and hhdrpono=" + newProductNumber[1] + " ";

            Integer rc = (Integer) s.createSQLQuery(q).uniqueResult();
            if (rc==0) invalid = false;
        } catch (Exception ex) {
            log.error("checkForRenewedVersion", ex);
        } finally {
            s.close();
        }

        if (invalid) {
            btnRenew.setDisabled(true);
            lNewProductNumber.setValue(newProductNumber[0] + "-" + newProductNumber[1] + ". Renewed version already exists!");
        } else {
            btnRenew.setDisabled(false);
        }
    }

    public void renew() {
        if (Messagebox.show("Are you sure you want to renew this product?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            renewProduct();
            renewPlans();
            renewLabels();
            if (cbRenewMembers.getSelectedIndex()==0) renewMembers();
            Messagebox.show("Product has been renewed", "Information", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    private void renewProduct() {
        String[] newProductNumber = lNewProductNumber.getValue().split("\\-");
        Calendar curDate = Calendar.getInstance();
        Calendar startingDate = Calendar.getInstance();
        Calendar matureDate = Calendar.getInstance();
        Calendar effectiveDate = Calendar.getInstance();
        startingDate.setTime(dNewStartingDate.getValue());
        matureDate.setTime(dNewMatureDate.getValue());
        effectiveDate.setTime(dNewEffectiveDate.getValue());

        String qry = "insert into [DB] ("
                + "hhdrrecid, hhdryy, hhdrbr, hhdrdist, hhdrpono, hhdrinsid, hhdrtpol, hhdrcur, hhdrttl, hhdrname, hhdraddr1, hhdraddr2, hhdrcity, hhdrzip, "
                + "hhdrphone1, hhdrphone2, hhdrphone3, hhdradtyy, hhdradtmm, hhdradtdd, hhdrsdtyy, hhdrsdtmm, hhdrsdtdd, hhdredtyy, hhdredtmm, hhdredtdd, "
                + "hhdrmdtyy, hhdrmdtmm, hhdrmdtdd, hhdrmoe, hhdrxdtyy, hhdrxdtmm, hhdrxdtdd, hhdreduryy, hhdredurmm, hhdrlindex, hhdragbr, hhdragcd, hhdrcomr, "
                + "hhdrcomamt, hhdrmbr, hhdrxmbr, hhdrflag, hhdrmode, hhdrlobcd, hhdrfreq, hhdrdisc, hhdrdisamt, hhdrload, hhdrlamt, hhdrinst, hhdrpco, "
                + "hhdrpcd, hhdrtprmin, hhdrtprmop, hhdrtprmmt, hhdrtprmdt, hhdrtprmgl, hhdrtprmot, hhdrlob, hhdrupdyy, hhdrupdmm, hhdrupddd, hhdrprgid, hhdrusrid "
                + ") select "
                + "hhdrrecid, " + newProductNumber[0] + ", hhdrbr, hhdrdist, hhdrpono, hhdrinsid, hhdrtpol, hhdrcur, hhdrttl, hhdrname, hhdraddr1, hhdraddr2, hhdrcity, hhdrzip, "
                + "hhdrphone1, hhdrphone2, hhdrphone3, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", " + curDate.get(Calendar.DATE) + ", " + startingDate.get(Calendar.YEAR) + ", " + (startingDate.get(Calendar.MONTH)+1) + ", " + startingDate.get(Calendar.DATE) + ", " + effectiveDate.get(Calendar.YEAR) + ", " + (effectiveDate.get(Calendar.MONTH)+1) + ", " + effectiveDate.get(Calendar.DATE) + ", "
                + matureDate.get(Calendar.YEAR) + ", " + (matureDate.get(Calendar.MONTH)+1) + ", " + matureDate.get(Calendar.DATE) + ", hhdrmoe, hhdrxdtyy, hhdrxdtmm, hhdrxdtdd, hhdreduryy, hhdredurmm, hhdrlindex, hhdragbr, hhdragcd, hhdrcomr, "
                + "hhdrcomamt, hhdrmbr, hhdrxmbr, hhdrflag, hhdrmode, hhdrlobcd, hhdrfreq, hhdrdisc, hhdrdisamt, hhdrload, hhdrlamt, hhdrinst, hhdrpco, "
                + "hhdrpcd, hhdrtprmin, hhdrtprmop, hhdrtprmmt, hhdrtprmdt, hhdrtprmgl, hhdrtprmot, hhdrlob, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", "
                + curDate.get(Calendar.DATE) + ", 'TARTXTNEW', 'FAIZAL' "
                + "from [DB] "
                + "where "
                + "hhdryy=" + productPOJO.getProductYear() + " and "
                + "hhdrpono=" + productPOJO.getProductId() + " ";

        Session s = Libs.sfDB.openSession();
        try {
            s.createSQLQuery(qry.replace("[DB]", "idnhltpf.dbo.hlthdr")).executeUpdate();
            s.beginTransaction();
            s.getTransaction().commit();
        } catch (Exception ex) {
            log.error("renewProduct", ex);
        } finally {
            s.close();
        }
    }

    private void renewPlans() {
        String[] newProductNumber = lNewProductNumber.getValue().split("\\-");
        Calendar curDate = Calendar.getInstance();
        Calendar matureDate = Calendar.getInstance();
        Calendar effectiveDate = Calendar.getInstance();
        matureDate.setTime(dNewMatureDate.getValue());
        effectiveDate.setTime(dNewEffectiveDate.getValue());

        String qry = "";

        Session s = Libs.sfDB.openSession();
        try {
            qry = "select "
                    + "hbftyy, hbftbr, hbftdist, hbftpono, hbftcode "
                    + "from idnhltpf.dbo.hltbft "
                    + "where "
                    + "hbftyy=" + productPOJO.getProductYear() + " and "
                    + "hbftpono=" + productPOJO.getProductId() + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();

            for (Object[] o : l) {
                qry = "insert into [DB] ( "
                        + "hbftrecid, hbftyy, hbftbr, hbftdist, hbftpono, hbftcode, hbftcprm1, hbftcprm2, hbftcprm3, hbftcprm4, hbftcprm5, hbftcprm6, "
                        + "hbftpremi1, hbftpremi2, hbftpremi3, hbftpremi4, hbftpremi5, hbftpremi6, hbftmode, hbftreim, hbftlimit, hbftlmtamt, "
                        + "hbftbcd1, hbftbcd2, hbftbcd3, hbftbcd4, hbftbcd5, hbftbcd6, hbftbcd7, hbftbcd8, hbftbcd9, hbftbcd10, "
                        + "hbftbcd11, hbftbcd12, hbftbcd13, hbftbcd14, hbftbcd15, hbftbcd16, hbftbcd17, hbftbcd18, hbftbcd19, hbftbcd20, "
                        + "hbftbcd21, hbftbcd22, hbftbcd23, hbftbcd24, hbftbcd25, hbftbcd26, hbftbcd27, hbftbcd28, hbftbcd29, hbftbcd30, "
                        + "hbftbpln1, hbftbpln2, hbftbpln3, hbftbpln4, hbftbpln5, hbftbpln6, hbftbpln7, hbftbpln8, hbftbpln9, hbftbpln10, "
                        + "hbftbpln11, hbftbpln12, hbftbpln13, hbftbpln14, hbftbpln15, hbftbpln16, hbftbpln17, hbftbpln18, hbftbpln19, hbftbpln20, "
                        + "hbftbpln21, hbftbpln22, hbftbpln23, hbftbpln24, hbftbpln25, hbftbpln26, hbftbpln27, hbftbpln28, hbftbpln29, hbftbpln30, "
                        + "hbftbday1, hbftbday2, hbftbday3, hbftbday4, hbftbday5, hbftbday6, hbftbday7, hbftbday8, hbftbday9, hbftbday10, "
                        + "hbftbday11, hbftbday12, hbftbday13, hbftbday14, hbftbday15, hbftbday16, hbftbday17, hbftbday18, hbftbday19, hbftbday20, "
                        + "hbftbday21, hbftbday22, hbftbday23, hbftbday24, hbftbday25, hbftbday26, hbftbday27, hbftbday28, hbftbday29, hbftbday30, "
                        + "hbftbpct1, hbftbpct2, hbftbpct3, hbftbpct4, hbftbpct5, hbftbpct6, hbftbpct7, hbftbpct8, hbftbpct9, hbftbpct10, "
                        + "hbftbpct11, hbftbpct12, hbftbpct13, hbftbpct14, hbftbpct15, hbftbpct16, hbftbpct17, hbftbpct18, hbftbpct19, hbftbpct20, "
                        + "hbftbpct21, hbftbpct22, hbftbpct23, hbftbpct24, hbftbpct25, hbftbpct26, hbftbpct27, hbftbpct28, hbftbpct29, hbftbpct30, "
                        + "hbftbflg1, hbftbflg2, hbftbflg3, hbftbflg4, hbftbflg5, hbftbflg6, hbftbflg7, hbftbflg8, hbftbflg9, hbftbflg10, "
                        + "hbftbflg11, hbftbflg12, hbftbflg13, hbftbflg14, hbftbflg15, hbftbflg16, hbftbflg17, hbftbflg18, hbftbflg19, hbftbflg20, "
                        + "hbftbflg21, hbftbflg22, hbftbflg23, hbftbflg24, hbftbflg25, hbftbflg26, hbftbflg27, hbftbflg28, hbftbflg29, hbftbflg30, "
                        + "hbftbmod1, hbftbmod2, hbftbmod3, hbftbmod4, hbftbmod5, hbftbmod6, hbftbmod7, hbftbmod8, hbftbmod9, hbftbmod10, "
                        + "hbftbmod11, hbftbmod12, hbftbmod13, hbftbmod14, hbftbmod15, hbftbmod16, hbftbmod17, hbftbmod18, hbftbmod19, hbftbmod20, "
                        + "hbftbmod21, hbftbmod22, hbftbmod23, hbftbmod24, hbftbmod25, hbftbmod26, hbftbmod27, hbftbmod28, hbftbmod29, hbftbmod30, "
                        + "hbftwprd, hbftmoe, hbftupdyy, hbftupdmm, hbftupddd, hbftprgid, hbftusrid "
                        + ") select "
                        + "hbftrecid, " + newProductNumber[0] + ", hbftbr, hbftdist, hbftpono, hbftcode, hbftcprm1, hbftcprm2, hbftcprm3, hbftcprm4, hbftcprm5, hbftcprm6, "
                        + "hbftpremi1, hbftpremi2, hbftpremi3, hbftpremi4, hbftpremi5, hbftpremi6, hbftmode, hbftreim, hbftlimit, hbftlmtamt, "
                        + "hbftbcd1, hbftbcd2, hbftbcd3, hbftbcd4, hbftbcd5, hbftbcd6, hbftbcd7, hbftbcd8, hbftbcd9, hbftbcd10, "
                        + "hbftbcd11, hbftbcd12, hbftbcd13, hbftbcd14, hbftbcd15, hbftbcd16, hbftbcd17, hbftbcd18, hbftbcd19, hbftbcd20, "
                        + "hbftbcd21, hbftbcd22, hbftbcd23, hbftbcd24, hbftbcd25, hbftbcd26, hbftbcd27, hbftbcd28, hbftbcd29, hbftbcd30, "
                        + "hbftbpln1, hbftbpln2, hbftbpln3, hbftbpln4, hbftbpln5, hbftbpln6, hbftbpln7, hbftbpln8, hbftbpln9, hbftbpln10, "
                        + "hbftbpln11, hbftbpln12, hbftbpln13, hbftbpln14, hbftbpln15, hbftbpln16, hbftbpln17, hbftbpln18, hbftbpln19, hbftbpln20, "
                        + "hbftbpln21, hbftbpln22, hbftbpln23, hbftbpln24, hbftbpln25, hbftbpln26, hbftbpln27, hbftbpln28, hbftbpln29, hbftbpln30, "
                        + "hbftbday1, hbftbday2, hbftbday3, hbftbday4, hbftbday5, hbftbday6, hbftbday7, hbftbday8, hbftbday9, hbftbday10, "
                        + "hbftbday11, hbftbday12, hbftbday13, hbftbday14, hbftbday15, hbftbday16, hbftbday17, hbftbday18, hbftbday19, hbftbday20, "
                        + "hbftbday21, hbftbday22, hbftbday23, hbftbday24, hbftbday25, hbftbday26, hbftbday27, hbftbday28, hbftbday29, hbftbday30, "
                        + "hbftbpct1, hbftbpct2, hbftbpct3, hbftbpct4, hbftbpct5, hbftbpct6, hbftbpct7, hbftbpct8, hbftbpct9, hbftbpct10, "
                        + "hbftbpct11, hbftbpct12, hbftbpct13, hbftbpct14, hbftbpct15, hbftbpct16, hbftbpct17, hbftbpct18, hbftbpct19, hbftbpct20, "
                        + "hbftbpct21, hbftbpct22, hbftbpct23, hbftbpct24, hbftbpct25, hbftbpct26, hbftbpct27, hbftbpct28, hbftbpct29, hbftbpct30, "
                        + "hbftbflg1, hbftbflg2, hbftbflg3, hbftbflg4, hbftbflg5, hbftbflg6, hbftbflg7, hbftbflg8, hbftbflg9, hbftbflg10, "
                        + "hbftbflg11, hbftbflg12, hbftbflg13, hbftbflg14, hbftbflg15, hbftbflg16, hbftbflg17, hbftbflg18, hbftbflg19, hbftbflg20, "
                        + "hbftbflg21, hbftbflg22, hbftbflg23, hbftbflg24, hbftbflg25, hbftbflg26, hbftbflg27, hbftbflg28, hbftbflg29, hbftbflg30, "
                        + "hbftbmod1, hbftbmod2, hbftbmod3, hbftbmod4, hbftbmod5, hbftbmod6, hbftbmod7, hbftbmod8, hbftbmod9, hbftbmod10, "
                        + "hbftbmod11, hbftbmod12, hbftbmod13, hbftbmod14, hbftbmod15, hbftbmod16, hbftbmod17, hbftbmod18, hbftbmod19, hbftbmod20, "
                        + "hbftbmod21, hbftbmod22, hbftbmod23, hbftbmod24, hbftbmod25, hbftbmod26, hbftbmod27, hbftbmod28, hbftbmod29, hbftbmod30, "
                        + "hbftwprd, hbftmoe, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", "
                        + curDate.get(Calendar.DATE) + ", 'TARTXTNEW', 'FAIZAL' "
                        + "from [DB] "
                        + "where "
                        + "hbftyy=" + productPOJO.getProductYear() + " and "
                        + "hbftpono=" + productPOJO.getProductId() + " and "
                        + "hbftcode='" + o[4].toString().trim() + "'";

                s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(qry.replace("[DB]", "idnhltpf.dbo.hltbft")).executeUpdate();
                    s.beginTransaction();
                    s.getTransaction().commit();
                } catch (Exception ex) {
                    log.error("renewProducts", ex);
                } finally {
                    s.close();
                }
            }
        } catch (Exception ex) {
            log.error("renewProducts", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void renewLabels() {
        String[] newProductNumber = lNewProductNumber.getValue().split("\\-");
        Calendar curDate = Calendar.getInstance();
        Calendar matureDate = Calendar.getInstance();
        Calendar effectiveDate = Calendar.getInstance();
        matureDate.setTime(dNewMatureDate.getValue());
        effectiveDate.setTime(dNewEffectiveDate.getValue());

        String qry = "";

        Session s = Libs.sfDB.openSession();
        try {
            qry = "select "
                    + "hlblyy, hlblbr, hlbldist, hlblpono, hlblcode "
                    + "from idnhltpf.dbo.hltlbl "
                    + "where "
                    + "hlblyy=" + productPOJO.getProductYear() + " and "
                    + "hlblpono=" + productPOJO.getProductId() + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();

            for (Object[] o : l) {
                qry = "insert into [DB] ( "
                        + "hlblid, hlblyy, hlblbr, hlbldist, hlblpono, hlblcode, hlblttl, hlblname, hlbladdr, hlblcity, "
                        + "hlblzip, hlblphone1, hlblphone2, hlblphone3, hlblbprod, hlbldprod, hlbluprod, "
                        + "hlblupdyy, hlblupdmm, hlblupddd, hlblprgid, hlblusrid "
                        + ") select "
                        + "hlblid, " + newProductNumber[0] + ", hlblbr, hlbldist, hlblpono, hlblcode, hlblttl, hlblname, hlbladdr, hlblcity, "
                        + "hlblzip, hlblphone1, hlblphone2, hlblphone3, hlblbprod, hlbldprod, hlbluprod, "
                        + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", " + curDate.get(Calendar.DATE) + ", 'HLT59E', 'FAIZAL' "
                        + "from [DB] "
                        + "where "
                        + "hlblyy=" + productPOJO.getProductYear() + " and "
                        + "hlblpono=" + productPOJO.getProductId() + " and "
                        + "hlblcode='" + o[4].toString().trim() + "'";

                s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(qry.replace("[DB]", "idnhltpf.dbo.hltlbl")).executeUpdate();
                    s.beginTransaction();
                    s.getTransaction().commit();
                } catch (Exception ex) {
                    log.error("renewLabels", ex);
                } finally {
                    s.close();
                }
            }
        } catch (Exception ex) {
            log.error("renewLabels", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void renewMembers() {
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hdt1idxno, a.hdt1seqno, "
                    + "b.hdt2edtyy, b.hdt2edtmm, b.hdt2edtdd, "
                    + "b.hdt2pedty1, b.hdt2pedtm1, b.hdt2pedtd1, b.hdt2pxdty1, b.hdt2pxdtm1, b.hdt2pxdtd1, " // 5
                    + "b.hdt2pedty2, b.hdt2pedtm2, b.hdt2pedtd2, b.hdt2pxdty2, b.hdt2pxdtm2, b.hdt2pxdtd2, " // 11
                    + "b.hdt2pedty3, b.hdt2pedtm3, b.hdt2pedtd3, b.hdt2pxdty3, b.hdt2pxdtm3, b.hdt2pxdtd3, " // 17
                    + "b.hdt2pedty4, b.hdt2pedtm4, b.hdt2pedtd4, b.hdt2pxdty4, b.hdt2pxdtm4, b.hdt2pxdtd4, " // 23
                    + "b.hdt2pedty5, b.hdt2pedtm5, b.hdt2pedtd5, b.hdt2pxdty5, b.hdt2pxdtm5, b.hdt2pxdtd5, " // 29
                    + "b.hdt2pedty6, b.hdt2pedtm6, b.hdt2pedtd6, b.hdt2pxdty6, b.hdt2pxdtm6, b.hdt2pxdtd6, " // 35
                    + "b.hdt2moe, " // 41
                    + "a.hdt1name, a.hdt1sex, a.hdt1bdtyy, a.hdt1bdtmm, a.hdt1bdtdd, a.hdt1mstat, "
                    + "b.hdt2sdtyy, b.hdt2sdtmm, b.hdt2sdtdd, "
                    + "c.hhdrname, " // 51
                    + "d.hempcnpol, d.hempcnid, a.hdt1ncard, "
                    + "b.hdt2plan1, d.hempmemo3, "
                    + "(convert(varchar,b.hdt2mdtyy)+'-'+convert(varchar,b.hdt2mdtmm)+'-'+convert(varchar,b.hdt2mdtdd)) as mdt "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hltdt2 b on a.hdt1yy=b.hdt2yy and a.hdt1br=b.hdt2br and a.hdt1dist=b.hdt2dist and a.hdt1pono=b.hdt2pono and a.hdt1idxno=b.hdt2idxno and a.hdt1seqno=b.hdt2seqno and a.hdt1ctr=b.hdt2ctr "
                    + "inner join idnhltpf.dbo.hlthdr c on c.hhdryy=a.hdt1yy and c.hhdrpono=a.hdt1pono "
                    + "inner join idnhltpf.dbo.hltemp d on d.hempyy=a.hdt1yy and d.hemppono=a.hdt1pono and d.hempidxno=a.hdt1idxno and d.hempseqno=a.hdt1seqno and d.hempctr=a.hdt1ctr "
                    + "where "
                    + "a.hdt1yy=" + productPOJO.getProductYear() + " and "
                    + "a.hdt1pono=" + productPOJO.getProductId() + " and "
                    + "a.hdt1ctr=0 ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                renewMember(o);
            };
        
        } catch (Exception ex) {
            log.error("renewMembers", ex);
        } finally {
            s.close();
        }
    }
    


    private void renewMember(Object[] o) {
        String[] newProductNumber = lNewProductNumber.getValue().split("\\-");
        Calendar curDate = Calendar.getInstance();
        Calendar matureDate = Calendar.getInstance();
        Calendar effectiveDate = Calendar.getInstance();
        matureDate.setTime(dNewMatureDate.getValue());
        effectiveDate.setTime(dNewEffectiveDate.getValue());

        try {
            Calendar calCurrent = Calendar.getInstance();
            Calendar calMature = Calendar.getInstance();
            calMature.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[57])));

            int diff = Libs.getDiffDays(calCurrent.getTime(), calMature.getTime());

//            if (diff<0) {
//                Renew HLTDT1
                String q = "insert into [DB] ( "
                        + "hdt1id, hdt1yy, hdt1br, hdt1dist, hdt1pono, hdt1idxno, hdt1seqno, hdt1ctr, hdt1ncard, hdt1name, hdt1bdtyy, hdt1bdtmm, hdt1bdtdd, "
                        + "hdt1ttl, hdt1code, hdt1age, hdt1bplace, hdt1sex, hdt1rel, hdt1mstat, hdt1occucd, hdt1occu, hdt1haddr, hdt1dep, hdt1nip1, hdt1nip2, hdt1updyy, hdt1updmm, "
                        + "hdt1upddd, hdt1prgid, hdt1usrid "
                        + ") select "
                        + "hdt1id, " + newProductNumber[0] + ", hdt1br, hdt1dist, hdt1pono, hdt1idxno, hdt1seqno, 0, hdt1ncard, hdt1name, hdt1bdtyy, hdt1bdtmm, hdt1bdtdd, "
                        + "hdt1ttl, hdt1code, hdt1age, hdt1bplace, hdt1sex, hdt1rel, hdt1mstat, hdt1occucd, hdt1occu, hdt1haddr, hdt1dep, hdt1nip1, hdt1nip2, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", "
                        + curDate.get(Calendar.DATE) + ", 'TARTXTNEW', 'FAIZAL' "
                        + "from [DB] "
                        + "where "
                        + "hdt1yy=" + productPOJO.getProductYear() + " and "
                        + "hdt1pono=" + productPOJO.getProductId() + " and "
                        + "hdt1idxno=" + o[0] + " and "
                        + "hdt1seqno='" + o[1] + "' and "
                        + "hdt1ctr=0";

                Session s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltdt1")).executeUpdate();
                    s.beginTransaction().commit();
                } catch (Exception ex) {
                    log.error("renewMember", ex);
                } finally {
                    s.close();
                }

//                Renew HLTDT2
                String[] plans = new String[6];
                String[] planx = new String[6];

                for (int i=0; i<6; i++) {
                    plans[i] = "0, 0, 0";
                    planx[i] = "0, 0, 0";

                    if (Integer.valueOf(o[(i*6)+5].toString())>1900) {
                        plans[i] = effectiveDate.get(Calendar.YEAR) + ", " + (effectiveDate.get(Calendar.MONTH)+1) + ", " + effectiveDate.get(Calendar.DATE);
                        planx[i] = matureDate.get(Calendar.YEAR) + ", " + (matureDate.get(Calendar.MONTH)+1) + ", " + matureDate.get(Calendar.DATE);
                    }
                }

                String newState = o[41].toString().trim();
                if (newState.equals("M")) newState = "";

                q = "insert into [DB] ( "
                        + "hdt2recid, hdt2yy, hdt2br, hdt2dist, hdt2pono, hdt2idxno, hdt2seqno, hdt2ctr, hdt2adtyy, hdt2adtmm, hdt2adtdd, hdt2sdtyy, hdt2sdtmm, hdt2sdtdd, "
                        + "hdt2edtyy, hdt2edtmm, hdt2edtdd, hdt2mdtyy, hdt2mdtmm, hdt2mdtdd, hdt2moe, hdt2xdtyy, hdt2xdtmm, hdt2xdtdd, hdt2vdtyy, hdt2vdtmm, hdt2vdtdd, "
                        + "hdt2ldtyy, hdt2ldtmm, hdt2ldtdd, "
                        + "hdt2plan1, hdt2loadp1, hdt2cprem1, hdt2pedty1, hdt2pedtm1, hdt2pedtd1, hdt2pxdty1, hdt2pxdtm1, hdt2pxdtd1, hdt2premi1, hdt2rfamt1, "
                        + "hdt2plan2, hdt2loadp2, hdt2cprem2, hdt2pedty2, hdt2pedtm2, hdt2pedtd2, hdt2pxdty2, hdt2pxdtm2, hdt2pxdtd2, hdt2premi2, hdt2rfamt2, "
                        + "hdt2plan3, hdt2loadp3, hdt2cprem3, hdt2pedty3, hdt2pedtm3, hdt2pedtd3, hdt2pxdty3, hdt2pxdtm3, hdt2pxdtd3, hdt2premi3, hdt2rfamt3, "
                        + "hdt2plan4, hdt2loadp4, hdt2cprem4, hdt2pedty4, hdt2pedtm4, hdt2pedtd4, hdt2pxdty4, hdt2pxdtm4, hdt2pxdtd4, hdt2premi4, hdt2rfamt4, "
                        + "hdt2plan5, hdt2loadp5, hdt2cprem5, hdt2pedty5, hdt2pedtm5, hdt2pedtd5, hdt2pxdty5, hdt2pxdtm5, hdt2pxdtd5, hdt2premi5, hdt2rfamt5, "
                        + "hdt2plan6, hdt2loadp6, hdt2cprem6, hdt2pedty6, hdt2pedtm6, hdt2pedtd6, hdt2pxdty6, hdt2pxdtm6, hdt2pxdtd6, hdt2premi6, hdt2rfamt6, "
                        + "hdt2lriyy, hdt2lrimm, hdt2pcuyy, hdt2pcumm, hdt2lclmn1, hdt2lclmn2, hdt2lclmn3, hdt2lclmn4, hdt2lclmn5, hdt2lclmn6, hdt2mflag, hdt2id, hdt2cof, hdt2cardf, "
                        + "hdt2lctr, hdt2pbr, hdt2pdist, hdt2ppono, hdt2pindex, hdt2pseq, hdt2pctr, hdt2nbr, hdt2ndist, hdt2npono, hdt2nindex, hdt2nseq, hdt2nctr, "
                        + "hdt2efdtyy, hdt2efdtmm, hdt2ltrnno, hdt2updyy, hdt2updmm, hdt2upddd, hdt2prgid, hdt2usrid "
                        + ") select "
                        + "hdt2recid, " + newProductNumber[0] + ", hdt2br, hdt2dist, hdt2pono, hdt2idxno, hdt2seqno, 0, hdt2adtyy, hdt2adtmm, hdt2adtdd, hdt2sdtyy, hdt2sdtmm, hdt2sdtdd, "
                        + effectiveDate.get(Calendar.YEAR) + ", " + (effectiveDate.get(Calendar.MONTH)+1) + ", " + effectiveDate.get(Calendar.DATE) + ", "
                        + matureDate.get(Calendar.YEAR) + ", " + (matureDate.get(Calendar.MONTH)+1) + ", " + matureDate.get(Calendar.DATE) + ", '" + newState + "', 1900, 1, 1, 1900, 1, 1, "
                        + "1900, 1, 1, "
                        + "hdt2plan1, hdt2loadp1, hdt2cprem1, " + plans[0] + ", " + planx[0] + ", hdt2premi1, hdt2rfamt1, "
                        + "hdt2plan2, hdt2loadp2, hdt2cprem2, " + plans[1] + ", " + planx[1] + ", hdt2premi2, hdt2rfamt2, "
                        + "hdt2plan3, hdt2loadp3, hdt2cprem3, " + plans[2] + ", " + planx[2] + ", hdt2premi3, hdt2rfamt3, "
                        + "hdt2plan4, hdt2loadp4, hdt2cprem4, " + plans[3] + ", " + planx[3] + ", hdt2premi4, hdt2rfamt4, "
                        + "hdt2plan5, hdt2loadp5, hdt2cprem5, " + plans[4] + ", " + planx[4] + ", hdt2premi5, hdt2rfamt5, "
                        + "hdt2plan6, hdt2loadp6, hdt2cprem6, " + plans[5] + ", " + planx[5] + ", hdt2premi6, hdt2rfamt6, "
                        + "(hdt2lriyy+1), hdt2lrimm, hdt2pcuyy, hdt2pcumm, 0, 0, 0, 0, 0, 0, hdt2mflag, hdt2id, hdt2cof, hdt2cardf, "
                        + "hdt2lctr, hdt2pbr, hdt2pdist, hdt2ppono, hdt2pindex, hdt2pseq, hdt2pctr, hdt2nbr, hdt2ndist, hdt2npono, hdt2nindex, hdt2nseq, hdt2nctr, "
                        + "hdt2efdtyy, hdt2efdtmm, hdt2ltrnno, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", "
                        + curDate.get(Calendar.DATE) + ", 'TARTXTNEW', 'FAIZAL' "
                        + "from [DB] "
                        + "where "
                        + "hdt2yy=" + productPOJO.getProductYear() + " and "
                        + "hdt2pono=" + productPOJO.getProductId() + " and "
                        + "hdt2idxno=" + o[0] + " and "
                        + "hdt2seqno='" + o[1] + "' and "
                        + "hdt2ctr=0";

                s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltdt2")).executeUpdate();
                    s.beginTransaction().commit();
                } catch (Exception ex) {
                    log.error("renewMember", ex);
                } finally {
                    s.close();
                }

//                Renew HLTEMP
                q = "insert into [DB] ( "
                        + "hempid, hempyy, hempbr, hempdist, hemppono, hempidxno, hempseqno, hempctr, hempcnpol, hempcnid, hempgrade, hempmemo1, hempmemo2, hempmemo3, "
                        + "hempwdtyy, hempwdtmm, hempwdtdd, hemptdtyy, hemptdtmm, hemptdtdd, hempstat, hempnik, hempupdyy, hempupdmm, hempupddd, hempprgid, hempusrid "
                        + ") select "
                        + "hempid, " + newProductNumber[0] + ", hempbr, hempdist, hemppono, hempidxno, hempseqno, 0, hempcnpol, hempcnid, hempgrade, hempmemo1, hempmemo2, hempmemo3, "
                        + "hempwdtyy, hempwdtmm, hempwdtdd, hemptdtyy, hemptdtmm, hemptdtdd, hempstat, hempnik, " + curDate.get(Calendar.YEAR) + ", " + (curDate.get(Calendar.MONTH)+1) + ", "
                        + curDate.get(Calendar.DATE) + ", 'TARTXTNEW', 'FAIZAL' "
                        + "from [DB] "
                        + "where "
                        + "hempyy=" + productPOJO.getProductYear() + " and "
                        + "hemppono=" + productPOJO.getProductId() + " and "
                        + "hempidxno=" + o[0] + " and "
                        + "hempseqno='" + o[1] + "' and "
                        + "hempctr=0";

                s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltemp")).executeUpdate();
                    s.beginTransaction().commit();
                } catch (Exception ex) {
                    log.error("renewMember", ex);
                } finally {
                    s.close();
                }
//            }
        } catch (Exception ex) {
            log.error("renewMember", ex);
        }
    }

}
