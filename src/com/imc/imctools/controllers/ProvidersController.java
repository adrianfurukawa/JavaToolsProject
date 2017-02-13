package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ProviderPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.event.PagingEvent;
import java.util.List;

/**
 * Created by faizal on 1/27/14.
 */
public class ProvidersController extends Window {

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
                populate(evt.getActivePage() * pg.getPageSize(), pg.getPageSize());
            }
        });
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "a.hpronomor, a.hproname, "
                    + "(a.hproaddr1 + a.hproaddr2 + a.hproaddr3) as addr ";
            String q2 = "from idnhltpf.dbo.hltpro a ";
            String q3 = "";
            String q4 = "order by a.hproname asc; ";

            if (!Libs.nn(where).isEmpty()) q3 = "where " + where;

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pg.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ProviderPOJO providerPOJO = new ProviderPOJO();
                providerPOJO.setProviderCode(Integer.valueOf(Libs.nn(o[0])));
                providerPOJO.setName(Libs.nn(o[1]).trim());
                providerPOJO.setAddress(Libs.nn(o[2]).trim());

                int numberOfEDC = Libs.getNumberOfEDC(providerPOJO.getProviderCode());

                Listitem li = new Listitem();
                li.setValue(providerPOJO);
                li.appendChild(Libs.createNumericListcell(providerPOJO.getProviderCode(), "#"));
                li.appendChild(new Listcell(providerPOJO.getName()));
                li.appendChild(Libs.createNumericListcell(numberOfEDC, "#"));
                li.appendChild(new Listcell(providerPOJO.getAddress()));
                li.appendChild(new Listcell(""));
                li.appendChild(new Listcell(""));
                li.appendChild(new Listcell(""));
                li.appendChild(new Listcell(""));
                lb.appendChild(li);
            }

            ((Toolbarbutton) getFellow("tbnEdit")).setDisabled(true);
//            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(true);
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s.close();
        }
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "convert(varchar,a.hpronomor) like '%" + val + "%' or "
                    + "a.hproname like '%" + val + "%' or "
                    + "a.hproaddr1 like '%" + val + "%' or "
                    + "a.hproaddr2 like '%" + val + "%' or "
                    + "a.hproaddr3 like '%" + val + "%' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void providerSelected() {
        if (lb.getSelectedCount()==1) {
            ((Toolbarbutton) getFellow("tbnEdit")).setDisabled(false);
//            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(false);
        }
    }

    public void providerDoubleClicked() {
        edit();
    }

    public void edit() {
        Window w = (Window) Executions.createComponents("views/ProviderDetail.zul", this, null);
        w.setAttribute("providerPOJO", lb.getSelectedItem().getValue());
        w.setAttribute("parent", this);
        w.setAttribute("mode", 1);
        w.doModal();
    }

    public void newProvider() {
        if (Messagebox.show("Do you want to create new Provider?", "Confirmation", org.zkoss.zhtml.Messagebox.OK | org.zkoss.zhtml.Messagebox.CANCEL, org.zkoss.zhtml.Messagebox.QUESTION, org.zkoss.zhtml.Messagebox.CANCEL)== org.zkoss.zhtml.Messagebox.OK) {
            Window w = (Window) Executions.createComponents("views/ProviderDetail.zul", this, null);
            w.setAttribute("parent", this);
            w.setAttribute("mode", 0);
            w.doModal();
        }
    }

//    public void delete() {
//        if (Messagebox.show("Do you want to delete this Provider?", "Warning", org.zkoss.zhtml.Messagebox.OK | org.zkoss.zhtml.Messagebox.CANCEL, org.zkoss.zhtml.Messagebox.EXCLAMATION, org.zkoss.zhtml.Messagebox.CANCEL)== org.zkoss.zhtml.Messagebox.OK) {
//
//            ProviderPOJO providerPOJO = lb.getSelectedItem().getValue();
//            String q = "delete from [DB] "
//                    + "where "
//                    + "hpronomor=" + providerPOJO.getProviderCode() + " ";
//
//            boolean result = Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.DB);
//            if (result) {
//                Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.EDC);
//                Libs.executeUpdate(q.replace("[DB]", "idnhltpf.hltpro"), Libs.AS400);
//                refresh();
//                Messagebox.show("Provider has been deleted", "Information", org.zkoss.zhtml.Messagebox.OK, org.zkoss.zhtml.Messagebox.INFORMATION);
//            } else {
//                Messagebox.show("Error occured when deleting Provider", "Error", org.zkoss.zhtml.Messagebox.OK, org.zkoss.zhtml.Messagebox.ERROR);
//            }
//        }
//    }

}
