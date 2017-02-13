package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ReceiptPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by faizal on 12/18/13.
 */
public class ReceiptListController extends Window {

    private static Logger log = LoggerFactory.getLogger(ReceiptListController.class);
    private Listbox lbProvider;
    private Paging pgProvider;
    private String where;

    public void onCreate() {
        initComponents();
        populateProvider();
    }

    private void initComponents() {
        lbProvider = (Listbox) getFellow("lbProvider");
        pgProvider = (Paging) getFellow("pgProvider");

        pgProvider.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateProvider(evt.getActivePage()*pgProvider.getPageSize(), pgProvider.getPageSize());
            }
        });
    }

    private void populateProvider() {
        populateProvider(0, pgProvider.getPageSize());
    }

    private void populateProvider(int offset, int limit) {
        lbProvider.getItems().clear();

        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";

            String q1 = "select "
                    + "a.nointernal, a.nokwitansi, a.tglkwitansi, a.nmprov, "
                    + "(select count(*) from aso.dbo.preterimaprov_dtlins b where b.nointernal=a.nointernal) as insurances, "
                    + "(select top 1 c.tipe from aso.dbo.preterimaprov_dtlins c where c.nointernal=a.nointernal) as claim_type, "
                    + "a.tglinput, a.totaltagihan ";

            String q2 = "from aso.dbo.preterimaprov a ";

            String q3 = "where "
                    + "a.tgladmprov > '2011-01-01' "
                    + "and a.tgladmprov < {fn now()} "
                    + "[where] ";
//                    + "and a.nointernal=131200225 or a.nointernal=131200133 or a.nointernal=131100492 or a.nointernal=131100827 or a.nointernal=131200749 ";

            String q4 = "order by a.tgladmprov desc ";

            if (where!=null) {
                q3 = q3.replace("[where]", "and (" + where + ") ");
            } else {
                q3 = q3.replace("[where]", "");
            }

            Integer i = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgProvider.setTotalSize(i);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ReceiptPOJO receiptPOJO = new ReceiptPOJO();
                receiptPOJO.setInternalNumber(Libs.nn(o[0]));
                receiptPOJO.setReceiptNumber(Libs.nn(o[1]).trim());
                receiptPOJO.setDate(Libs.nn(o[2]).substring(0, 10));
                receiptPOJO.setProviderCompanyName(Libs.nn(o[3]).trim());
                receiptPOJO.setInputDate(Libs.nn(o[6]).substring(0, 10));

                int days = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(receiptPOJO.getInputDate()));
                Double[] result = processed(receiptPOJO.getInternalNumber(), Libs.nn(o[5]));

                Listitem li = new Listitem();
                li.setValue(receiptPOJO);

                li.appendChild(new Listcell(receiptPOJO.getInternalNumber()));
                li.appendChild(new Listcell(receiptPOJO.getReceiptNumber()));
                li.appendChild(new Listcell(receiptPOJO.getDate()));

                if (result[1]==0 && result[0]>0) {
                    li.appendChild(new Listcell("Done"));
                } else {
                    li.appendChild(Libs.createNumericListcell(Double.valueOf(days), "###"));
                }

                li.appendChild(new Listcell(receiptPOJO.getProviderCompanyName()));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[7])), "#,###.00"));
                li.appendChild(Libs.createNumericListcell(result[2], "#,###.00"));
                li.appendChild(new Listcell(Libs.nn(o[4])));
                li.appendChild(new Listcell(Libs.nn(o[5])));

                li.appendChild(Libs.createBooleanListcell(result[0]>0 ? true : false));
                li.appendChild(Libs.createProgressListcell(result[0]>0 ? Double.valueOf((((result[0]-result[1])*100)/result[0])).intValue() : 0));

                lbProvider.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateProvider", ex);
        } finally {
            s.close();
        }
    }

    public static Double[] processed(String internalNumber, String claimType) {
        Double[] result = new Double[] { 0D, 0D, 0D };
        Session s = Libs.sfDB.openSession();

        int i = 0;
        int unpaid = 0;
        double paidAmount = 0;

        try {
            String qry = "select "
                    + "a.no_hid, b.hovctyp, b.hovccqno, b.hovcoutno, b.dibayar "
                    + "from aso.dbo.pre_" + claimType.toLowerCase() + "_provider a "
                    + "left outer join idnhltpf.dbo.fin_paid b on b.hclmcno='IDN/' + a.no_hid "
                    + "where "
                    + "a.nointernal=" + internalNumber + " "
                    + "and a.flg=1 and b.updateas400=1 ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                if (o[1]==null) {
                    unpaid++;
                } else {
                    paidAmount += Double.valueOf(Libs.nn(o[4]));
                }
                i++;
            }

            result[0] = Double.valueOf(i);
            result[1] = Double.valueOf(unpaid);
            result[2] = paidAmount;
        } catch (Exception ex) {
            log.error("processed", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearchProvider")).getText();
        if (!val.isEmpty()) {

            where = "a.nmprov like '%" + val + "%' "
                    + "or a.nokwitansi like '%" + val + "%' "
                    + "or a.nointernal like '%" + val + "%' ";

            populateProvider();
        } else refresh();
    }

    public void refresh() {
        where = null;
        populateProvider();
    }

}
