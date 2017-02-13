package com.imc.imctools.controllers;

import com.imc.imctools.pojo.EDCPOJO;
import com.imc.imctools.pojo.ProviderPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.util.List;

/**
 * Created by faizal on 2/11/14.
 */
public class ProviderDetail1Controller extends Window {

    private Logger log = LoggerFactory.getLogger(ProviderDetail1Controller.class);
    private ProviderPOJO providerPOJO;
    private Listbox lbEDCList;
    private Listbox lbClaimHistory;
    private Paging pgEDCList;
    private Paging pgClaimHistory;

    public void onCreate() {
        providerPOJO = (ProviderPOJO) getAttribute("providerPOJO");
        initComponents();
        populate();
    }

    private void initComponents() {
        lbEDCList = (Listbox) getFellow("lbEDCList");
        lbClaimHistory = (Listbox) getFellow("lbClaimHistory");
        pgEDCList = (Paging) getFellow("pgEDCList");
        pgClaimHistory = (Paging) getFellow("pgClaimHistory");

        pgEDCList.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateEDCList(evt.getActivePage()*pgEDCList.getPageSize(), pgEDCList.getPageSize());
            }
        });
    }

    private void populate() {
        populateInformation();
        populateEDCList(0, pgEDCList.getPageSize());
        populateClaimHistory(0, pgClaimHistory.getPageSize());
    }

    private void populateInformation() {
        ((Label) getFellow("lProviderCode")).setValue(String.valueOf(providerPOJO.getProviderCode()));
        ((Label) getFellow("lName")).setValue(providerPOJO.getName());
    }

    private void populateEDCList(int offset, int limit) {
        lbEDCList.getItems().clear();
        Session s3 = Libs.sfEDC.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "tid, tid_sn ";
            String q2 = "from edc_prj.dbo.edc_terminal "
                    + "where "
                    + "pro_code=" + providerPOJO.getProviderCode();
            String q3 = "";
            String q4 = "order by tid asc ";

            Integer rc = (Integer) s3.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgEDCList.setTotalSize(rc);

            List<Object[]> l = s3.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                EDCPOJO edcPOJO = new EDCPOJO();
                edcPOJO.setTid(Integer.valueOf(Libs.nn(o[0])));
                edcPOJO.setSn(Libs.nn(o[1]));
                edcPOJO.setProviderCode(providerPOJO.getProviderCode());

                Integer[] edcUsage = new Integer[] { 0, 0 };
                edcUsage = Libs.getEDCUsageCount(edcPOJO.getTid(), edcPOJO.getProviderCode());

                Listitem li = new Listitem();
                li.setValue(edcPOJO);
                li.appendChild(new Listcell(String.valueOf(edcPOJO.getTid())));
                li.appendChild(new Listcell(edcPOJO.getSn()));
                li.appendChild(Libs.createNumericListcell(edcUsage[1], "#"));
                li.appendChild(Libs.createNumericListcell(edcUsage[0], "#"));
                lbEDCList.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateEDCList", ex);
        } finally {
            s3.close();
        }
    }

    private void populateClaimHistory(int offset, int limit) {
        lbClaimHistory.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.hclmtclaim, b.hdt1name, c.hhdrname, d.hinsname ";
            String q2 = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hltdt1 b on b.hdt1yy=a.hclmyy and b.hdt1pono=a.hclmpono and b.hdt1idxno=a.hclmidxno and b.hdt1seqno=a.hclmseqno and b.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hlthdr c on c.hhdryy=a.hclmyy and c.hhdrpono=a.hclmpono "
                    + "inner join idnhltpf.dbo.inslf d on d.hinsid=c.hhdrinsid ";
            String q3 = "where "
                    + "a.hclmnhoscd=" + providerPOJO.getProviderCode() + " ";
            String q4 = "";

            System.out.println(q1 + q2 + q3 + q4);

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgClaimHistory.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[1])));
                li.appendChild(new Listcell(Libs.nn(o[2])));
                li.appendChild(new Listcell(Libs.nn(o[3])));
                lbClaimHistory.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateClaimHistory", ex);
        } finally {
            s.close();
        }
    }

    public void edcSelected() {
        Window w = (Window) Executions.createComponents("views/EDCDetail.zul", this, null);
        w.setAttribute("edcPOJO", lbEDCList.getSelectedItem().getValue());
        w.doOverlapped();
    }

    public boolean validateDelete() {
        boolean result = true;

        Session s = Libs.sfDB.openSession();
        try {
            String q = "select count(*) "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmnhoscd=" + providerPOJO.getProviderCode() + " ";

            Integer rc = (Integer) s.createSQLQuery(q).uniqueResult();
            if (rc>0) {
                result = false;
                Messagebox.show("Cannot delete. This provider is used in " + rc + " claims!", "Error", Messagebox.OK, Messagebox.ERROR);
            }
        } catch (Exception ex) {
            log.error("validateDelete", ex);
        } finally {
            s.close();
        }

        return result;
    }

    public void deleteProvider() {
        if (validateDelete()) {
            if (Messagebox.show("Are you sure you want to delete this provider?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
                boolean result;

                String q = "delete from [DB] "
                        + "where "
                        + "hpronomor=" + providerPOJO.getProviderCode() + " ";

                result = Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.DB);

                q = "delete from edc_prj.dbo.edc_provider "
                        + "where "
                        + "pronomor=" + providerPOJO.getProviderCode() + " ";

                result = Libs.executeUpdate(q, Libs.EDC);

                if (result) {
                    Messagebox.show("Provider has been deleted");
                    ((ProvidersController) getAttribute("parent")).refresh();
                    detach();
                }
            }
        }
    }

}
