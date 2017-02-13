package com.imc.imctools.controllers;

import com.imc.imctools.pojo.EDCPOJO;
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
 * Created by faizal on 1/27/14.
 */
public class EDCsController extends Window {

    private Logger log = LoggerFactory.getLogger(EDCsController.class);
    private Listbox lb;
    private Paging pg;
    private String where;

    public void onCreate() {
        initComponents();
        populate(0, pg.getPageSize());
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s3 = Libs.sfEDC.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.tid, a.tid_sn, b.pronomor, b.proname ";
            String q2 = "from edc_prj.dbo.edc_terminal a "
                    + "left outer join edc_prj.dbo.edc_provider b on b.pronomor=a.pro_code ";
            String q3 = "";
            String q4 = "order by a.tid desc ";

            if (!Libs.nn(where).isEmpty()) q3 = "where " + where;

            Integer rc = (Integer) s3.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pg.setTotalSize(rc);

            List<Object[]> l = s3.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                EDCPOJO edcPOJO = new EDCPOJO();
                edcPOJO.setTid(Integer.valueOf(Libs.nn(o[0])));
                if (o[1]!=null) edcPOJO.setSn(Libs.nn(o[1]).trim());
                if (o[2]!=null) edcPOJO.setProviderCode(Integer.valueOf(Libs.nn(o[2])));

                Integer[] edcUsage = new Integer[] { 0, 0 };
                if (o[2]!=null) edcUsage = Libs.getEDCUsageCount(edcPOJO.getTid(), edcPOJO.getProviderCode());

                Listitem li = new Listitem();
                li.setValue(edcPOJO);
                li.appendChild(Libs.createNumericListcell(edcPOJO.getTid(), "#"));
                li.appendChild(new Listcell(Libs.nn(edcPOJO.getSn())));
                li.appendChild(new Listcell(Libs.nn(o[3]).trim()));
                li.appendChild(Libs.createNumericListcell(edcUsage[1], "#"));
                li.appendChild(Libs.createNumericListcell(edcUsage[0], "#"));
                lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s3.close();
        }
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "convert(varchar,a.tid) like '%" + val + "%' or "
                    + "convert(varchar,a.tid_sn) like '%" + val + "%' or "
                    + "b.proname like '%" + val + "%' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void edcSelected() {
        Window w = (Window) Executions.createComponents("views/EDCDetail.zul", this, null);
        w.setAttribute("edcPOJO", lb.getSelectedItem().getValue());
        w.doOverlapped();
    }

}
