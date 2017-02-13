package com.imc.imctools.controllers;

import com.imc.imctools.pojo.EDCPOJO;
import com.imc.imctools.pojo.ProviderPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.util.List;

/**
 * Created by faizal on 2/11/14.
 */
public class EDCDeploymentController extends Window {

    private Logger log = LoggerFactory.getLogger(EDCDeploymentController.class);
    private EDCPOJO edcPOJO;
    private Label lTID;
    private Label lSN;
    private Label lCurrentProvider;
    private Bandbox bbNewProvider;
    private Toolbarbutton btnDeploy;
    private Textbox tNotes;
    private Listbox lb;
    private Paging pg;
    private String where;

    public void onCreate() {
        edcPOJO = (EDCPOJO) getAttribute("edcPOJO");
        initComponents();

        populateInformation();
        populate(0, pg.getPageSize());
    }

    private void initComponents() {
        btnDeploy = (Toolbarbutton) getFellow("btnDeploy");
        lTID = (Label) getFellow("lTID");
        lSN = (Label) getFellow("lSN");
        lCurrentProvider = (Label) getFellow("lCurrentProvider");
        bbNewProvider = (Bandbox) getFellow("bbNewProvider");
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");
        tNotes = (Textbox) getFellow("tNotes");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });
    }

    private void populateInformation() {
        lTID.setValue(String.valueOf(edcPOJO.getTid()));
        lSN.setValue(edcPOJO.getSn());
        lCurrentProvider.setValue(Libs.getProviderById(edcPOJO.getProviderCode()).getName());
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "hpronomor, hproname ";
            String q2 = "from idnhltpf.dbo.hltpro ";
            String q3 = "";
            String q4 = "order by hproname asc ";

            if (where!=null) q3 += "where (" + where + ") ";

            Integer rc = (Integer) s.createSQLQuery(q0+ q2 + q3).uniqueResult();
            pg.setTotalSize(rc);

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

    public void providerSelected() {
        if (lb.getSelectedCount()>0) {
            ProviderPOJO providerPOJO = lb.getSelectedItem().getValue();
            bbNewProvider.setText(providerPOJO.getName());
            bbNewProvider.setAttribute("e", providerPOJO);
            bbNewProvider.close();
            btnDeploy.setDisabled(false);
        }
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "hproname like '%" + val + "%' or "
                    + "convert(varchar,hpronomor) like '%" + val + "%' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void deploy() {
        if (Messagebox.show("Proceed deployment of this EDC?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            Session s3 = Libs.sfEDC.openSession();
            try {
                ProviderPOJO providerPOJO = (ProviderPOJO) bbNewProvider.getAttribute("e");
                String q = "update edc_prj.dbo.edc_terminal "
                        + "set pro_code=" + providerPOJO.getProviderCode() + ", flg='1', ket='" + tNotes.getText().replace("'", "''") + "' "
                        + "where "
                        + "tid=" + edcPOJO.getTid() + " "
                        + "and tid_sn='" + edcPOJO.getSn() + "' ";

                s3.createSQLQuery(q).executeUpdate();
                s3.beginTransaction().commit();

                Messagebox.show("EDC have been deployed", "Information", Messagebox.OK, Messagebox.INFORMATION);
                setAttribute("newProviderPOJO", bbNewProvider.getAttribute("e"));
                detach();
            } catch (Exception ex) {
                log.error("deploy", ex);
            } finally {
                s3.close();
            }
        }
    }

}
