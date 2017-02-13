package com.imc.imctools.controllers;

import com.imc.imctools.pojo.*;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by faizal on 1/20/14.
 */
public class ProductsController extends Window {

    private Logger log = LoggerFactory.getLogger(ProductsController.class);
    private Listbox lb;
    private Paging pg;
    private ClientPOJO clientPOJO;
    private String where;

    public void onCreate() {
        clientPOJO = (ClientPOJO) getAttribute("clientPOJO");

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
        System.out.println(clientPOJO);

        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "hhdrpono, hhdryy, hhdrname, "
                    + "(convert(varchar,hhdrsdtyy)+'-'+convert(varchar,hhdrsdtmm)+'-'+convert(varchar,hhdrsdtdd)) as sdt, "
                    + "(convert(varchar,hhdredtyy)+'-'+convert(varchar,hhdredtmm)+'-'+convert(varchar,hhdredtdd)) as edt, "
                    + "(convert(varchar,hhdrmdtyy)+'-'+convert(varchar,hhdrmdtmm)+'-'+convert(varchar,hhdrmdtdd)) as mdt, "
                    + "(convert(varchar,hhdradtyy)+'-'+convert(varchar,hhdradtmm)+'-'+convert(varchar,hhdradtdd)) as adt, "
                    + "hhdrinsid ";
            String q2 = "from "
                    + "idnhltpf.dbo.hlthdr ";
            String q3 = "where "
                    + "hhdrpono not in (99998, 99999) ";
            String q4 = "order by hhdryy desc, hhdrname asc ";

            if (clientPOJO!=null) {
                q3 = "where hhdrinsid='" + clientPOJO.getClientId() + "' ";
            }

            if (where!=null) {
                if (!q3.isEmpty()) q3 += "and (" + where + ") ";
                else q3 += "where (" + where + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pg.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ProductPOJO e = new ProductPOJO();
                e.setClientPOJO(clientPOJO!=null ? clientPOJO : Libs.getClientById(Libs.nn(o[7])));
                e.setProductId(Libs.nn(o[0]));
                e.setProductYear(Libs.nn(o[1]));
                e.setProductName(Libs.nn(o[2]));
                e.setStartingDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[3])));
                e.setEffectiveDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[4])));
                e.setMatureDate(new SimpleDateFormat("yyyy-MM-dd").parse(Libs.nn(o[5])));

                Listitem li = new Listitem();
                li.setValue(e);

                li.appendChild(new Listcell(e.getProductId()));
                li.appendChild(new Listcell(e.getProductYear()));
                li.appendChild(new Listcell(e.getProductName()));
                li.appendChild(new Listcell(new SimpleDateFormat("yyyy-MM-dd").format(e.getStartingDate())));
                li.appendChild(new Listcell(new SimpleDateFormat("yyyy-MM-dd").format(e.getEffectiveDate())));
                li.appendChild(new Listcell(new SimpleDateFormat("yyyy-MM-dd").format(e.getMatureDate())));
                li.appendChild(new Listcell(Libs.nn(o[6]).trim()));

                lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s.close();
        }
    }

    public void productSelected() {
        if (lb.getSelectedCount()>0) {
            Window w = (Window) Executions.createComponents("views/ProductDetail.zul", this, null);
            w.setAttribute("productPOJO", lb.getSelectedItem().getValue());
            w.doModal();
        }
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "hhdrname like '%" + val + "%' or "
                    + "hhdrpono like '%" + val + "%' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

}
