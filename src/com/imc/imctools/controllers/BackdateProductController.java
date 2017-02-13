package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ClientPOJO;
import com.imc.imctools.pojo.MemberPOJO;
import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by faizal on 2/13/14.
 */
@SuppressWarnings("serial")
public class BackdateProductController extends Window {

    private Logger log = LoggerFactory.getLogger(BackdateProductController.class);
    private Listbox lbClients;
    private Listbox lbProducts;
    private Paging pgClients;
    private Bandbox bbClient;
    private Bandbox bbProduct;
    private Datebox dCurrentStartDate;
    private Datebox dNewStartDate;
    private String whereClients;
    private String whereProducts;

    public void onCreate() {
        initComponents();
        populateClients(0, pgClients.getPageSize());
    }

    private void initComponents() {
        lbClients = (Listbox) getFellow("lbClients");
        lbProducts = (Listbox) getFellow("lbProducts");
        pgClients = (Paging) getFellow("pgClients");
        bbClient = (Bandbox) getFellow("bbClient");
        bbProduct = (Bandbox) getFellow("bbProduct");
        dCurrentStartDate = (Datebox) getFellow("dCurrentStartDate");
        dNewStartDate = (Datebox) getFellow("dNewStartDate");

        pgClients.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateClients(evt.getActivePage()*pgClients.getPageSize(), pgClients.getPageSize());
            }
        });
    }

    private void populateClients(int offset, int limit) {
        lbClients.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "hinsid, hinsname, hinsdesc1 ";
            String q2 = "from idnhltpf.dbo.hltins ";
            String q3 = "";
            String q4 = "order by hinsname asc ";

            if (!Libs.nn(whereClients).isEmpty()) {
                if (q3.isEmpty()) q3 += "where (" + whereClients + ") ";
                else q3 += "and (" + whereClients + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgClients.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                ClientPOJO e = new ClientPOJO();
                e.setClientId(Libs.nn(o[0]).trim());
                e.setClientName(Libs.nn(o[1]).trim());

                Listitem li = new Listitem();
                li.setValue(e);
                li.appendChild(new Listcell(e.getClientId()));
                li.appendChild(new Listcell(e.getClientName()));
                li.appendChild(new Listcell(Libs.nn(o[2]).trim()));
                lbClients.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateClients", ex);
        } finally {
            s.close();
        }
    }

    private void populateProducts() {
        ClientPOJO clientPOJO = (ClientPOJO) bbClient.getAttribute("e");
        lbProducts.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
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

            if (clientPOJO!=null) {
                q3 = "where hhdrinsid='" + clientPOJO.getClientId() + "' ";
            }

            if (whereProducts!=null) {
                if (!q3.isEmpty()) q3 += "and (" + whereProducts + ") ";
                else q3 += "where (" + whereProducts + ") ";
            }

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).list();
            for (Object[] o : l) {
                ProductPOJO e = new ProductPOJO();
                e.setClientPOJO(clientPOJO);
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

    public void clientSelected() {
        if (lbClients.getSelectedCount()==1) {
            ClientPOJO clientPOJO = lbClients.getSelectedItem().getValue();
            bbClient.setText(clientPOJO.getClientName());
            bbClient.setAttribute("e", clientPOJO);
            populateProducts();
            bbClient.close();
        }
    }

    public void quickSearchClients() {
        Textbox tQuickSearchClients = (Textbox) getFellow("tQuickSearchClients");

        if (!tQuickSearchClients.getText().isEmpty()) {
            whereClients = "hinsname like '%" + tQuickSearchClients.getText() + "%' ";
            populateClients(0, pgClients.getPageSize());
            pgClients.setActivePage(0);
        } else {
            refreshClients();
        }
    }

    public void refreshClients() {
        whereClients = null;
        populateClients(0, pgClients.getPageSize());
    }

    public void productSelected() {
        if (lbProducts.getSelectedCount()==1) {
            ProductPOJO productPOJO = lbProducts.getSelectedItem().getValue();
            bbProduct.setText(productPOJO.getProductName());
            bbProduct.setAttribute("e", productPOJO);

            Session s = Libs.sfDB.openSession();
            try {
                String q = "select "
                        + "hhdrsdtyy, hhdrsdtmm, hhdrsdtdd "
                        + "from idnhltpf.dbo.hlthdr "
                        + "where "
                        + "hhdryy=" + productPOJO.getProductYear() + " "
                        + "and hhdrpono=" + productPOJO.getProductId() + " ";

                Object[] o = (Object[]) s.createSQLQuery(q).uniqueResult();
                if (o!=null) {
                    String sdt = Libs.nn(o[0]) + "-" + Libs.nn(o[1]) + "-" + Libs.nn(o[2]);
                    dCurrentStartDate.setValue(new SimpleDateFormat("yyyy-MM-dd").parse(sdt));
                    dNewStartDate.setValue(new SimpleDateFormat("yyyy-MM-dd").parse(sdt));
                }
            } catch (Exception ex) {
                log.error("productSelected", ex);
            } finally {
                s.close();
            }

            bbProduct.close();
        }
    }

    public void quickSearchProducts() {
        Textbox tQuickSearchProducts = (Textbox) getFellow("tQuickSearchProducts");

        if (!tQuickSearchProducts.getText().isEmpty()) {
            whereProducts = "hhdrname like '%" + tQuickSearchProducts.getText() + "%' or "
                    + "convert(varchar,hhdrpono) like '%" + tQuickSearchProducts.getText() + "%' ";
            populateProducts();
        } else {
            refreshProducts();
        }
    }

    public void refreshProducts() {
        whereProducts = null;
        populateProducts();
    }

    public void backdateProduct() {
        if (dNewStartDate.getText().isEmpty()) {
            Messagebox.show("Please input value to New Start Date", "Error", Messagebox.OK, Messagebox.ERROR);
        } else {
            if (Messagebox.show("Product and Dummy start date will be changed. Proceed?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
                ProductPOJO productPOJO = (ProductPOJO) bbProduct.getAttribute("e");
                String sdt = new SimpleDateFormat("yyyy-MM-dd").format(dNewStartDate.getValue());
                String q = "update [DB] "
                        + "set "
                        + "hhdrsdtyy=" + sdt.substring(0, 4) + ", "
                        + "hhdrsdtmm=" + sdt.substring(5, 7) + ", "
                        + "hhdrsdtdd=" + sdt.substring(8) + " "
                        + "where "
                        + "hhdryy=" + productPOJO.getProductYear() + " "
                        + "and hhdrpono=" + productPOJO.getProductId() + " ";

                if (Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hlthdr"), Libs.DB)) {
                    q = "update [DB] "
                            + "set "
                            + "hdt2sdtyy=" + sdt.substring(0, 4) + ", "
                            + "hdt2sdtmm=" + sdt.substring(5, 7) + ", "
                            + "hdt2sdtdd=" + sdt.substring(8) + " "
                            + "where "
                            + "hdt2yy=" + productPOJO.getProductYear() + " "
                            + "and hdt2pono=" + productPOJO.getProductId() + " "
                            + "and hdt2idxno>90000 "
                            + "and hdt2ctr=0 ";

                    if (Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltdt2"), Libs.DB)) {
                        Messagebox.show("Product Start Date has been changed", "Information", Messagebox.OK, Messagebox.INFORMATION);
                    }
                }
            }
        }
    }

}
