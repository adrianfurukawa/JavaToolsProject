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
 * Created by faizal on 1/28/14.
 */
public class EDCDetailController extends Window {

    private Logger log = LoggerFactory.getLogger(EDCDetailController.class);
    private EDCPOJO edcPOJO;
    private Listbox lbClaimHistory;
    private Listbox lbDeploymentHistory;
    private Paging pgClaimHistory;
    private Paging pgDeploymentHistory;

    public void onCreate() {
        if (getAttribute("edcPOJO")!=null) edcPOJO = (EDCPOJO) getAttribute("edcPOJO");
        initComponents();
        populate();
    }

    private void initComponents() {
        lbClaimHistory = (Listbox) getFellow("lbClaimHistory");
        lbDeploymentHistory = (Listbox) getFellow("lbDeploymentHistory");
        pgClaimHistory = (Paging) getFellow("pgClaimHistory");
        pgDeploymentHistory = (Paging) getFellow("pgDeploymentHistory");

        pgClaimHistory.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateClaimHistory(evt.getActivePage()*pgClaimHistory.getPageSize(), pgClaimHistory.getPageSize());
            }
        });

        pgDeploymentHistory.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateDeploymentHistory(evt.getActivePage() * pgDeploymentHistory.getPageSize(), pgDeploymentHistory.getPageSize());
            }
        });
    }

    private void populate() {
        populateInformation();
        populateClaimHistory(0, pgClaimHistory.getPageSize());
        populateDeploymentHistory(0, pgDeploymentHistory.getPageSize());
    }

    private void populateInformation() {
        ((Label) getFellow("lTID")).setValue(String.valueOf(edcPOJO.getTid()));
        ((Label) getFellow("lSN")).setValue(edcPOJO.getSn());
        ((Label) getFellow("lProvider")).setValue(Libs.getProviderById(edcPOJO.getProviderCode()).getName());
    }

    private void populateClaimHistory(int offset, int limit) {
        lbClaimHistory.getItems().clear();
        Session s3 = Libs.sfEDC.openSession();
        try {
            String q = "select "
                    + "a.transaction_id, a.request_date, c.proname "
                    + "from edc_prj.dbo.ms_log_transaction a "
                    + "inner join edc_prj.dbo.request_ref# b on b.ref#=a.transaction_id "
                    + "inner join edc_prj.dbo.edc_provider c on c.pronomor=b.provider_code "
                    + "where tid=" + edcPOJO.getTid() + " "
                    + "order by request_date desc ";

            List<Object[]> l = s3.createSQLQuery(q).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.appendChild(new Listcell(Libs.nn(o[0]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[1]).trim().substring(0, 10)));
                li.appendChild(new Listcell(Libs.nn(o[2]).trim()));
                lbClaimHistory.appendChild(li);
            }

        } catch (Exception ex) {
            log.error("populateClaimHistory", ex);
        } finally {
            s3.close();
        }
    }

    private void populateDeploymentHistory(int offset, int limit) {
        lbDeploymentHistory.getItems().clear();
        Session s3 = Libs.sfEDC.openSession();
        try {
            String q = "select "
                    + "c.proname, count(*) from edc_prj.dbo.ms_log_transaction a "
                    + "inner join edc_prj.dbo.request_ref# b on b.ref#=a.transaction_id "
                    + "inner join edc_prj.dbo.edc_provider c on c.pronomor=b.provider_code "
                    + "where tid=" + edcPOJO.getTid() + " "
                    + "group by c.proname ";

            List<Object[]> l = s3.createSQLQuery(q).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                li.appendChild(new Listcell(Libs.nn(o[0]).trim()));
                li.appendChild(Libs.createNumericListcell(Integer.valueOf(Libs.nn(o[1])), "#"));
                lbDeploymentHistory.appendChild(li);
            }

        } catch (Exception ex) {
            log.error("populateDeploymentHistory", ex);
        } finally {
            s3.close();
        }
    }

    public void deploy() {
        Window w = (Window) Executions.createComponents("views/EDCDeployment.zul", this, null);
        w.setAttribute("edcPOJO", edcPOJO);
        w.doModal();

        ProviderPOJO providerPOJO = (ProviderPOJO) w.getAttribute("newProviderPOJO");
        if (providerPOJO!=null) {
            edcPOJO.setProviderCode(providerPOJO.getProviderCode());
            populate();
        }
    }

    public void withdraw() {
        if (Messagebox.show("Are you sure you want to withdraw this EDC?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            Session s3 = Libs.sfEDC.openSession();
            try {
                String q = "update edc_prj.dbo.edc_terminal "
                        + "set pro_code=9999999, flg='0' "
                        + "where "
                        + "tid=" + edcPOJO.getTid() + " "
                        + "and tid_sn='" + edcPOJO.getSn() + "' ";

                s3.createSQLQuery(q).executeUpdate();
                s3.beginTransaction().commit();
                Messagebox.show("EDC has been withdrawn", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (Exception ex) {
                log.error("withdraw", ex);
            } finally {
                s3.close();
            }
        }
    }

}
