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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by faizal on 2/18/14.
 */
public class DeleteMemberController extends Window {

    private Logger log = LoggerFactory.getLogger(DeleteMemberController.class);
    private Row rowEffectiveDate;
    private Combobox cbEffectiveTime;
    private Datebox dEffectiveDate;
    private Listbox lbClients;
    private Listbox lbProducts;
    private Listbox lbMembers;
    private Paging pgClients;
    private Paging pgMembers;
    private Bandbox bbClient;
    private Bandbox bbProduct;
    private Bandbox bbMember;
    private String whereClients;
    private String whereProducts;
    private String whereMembers;
    private Textbox tCurrentIndex;
    private Textbox tCurrentSequence;

    public void onCreate() {
        initComponents();
    }

    private void initComponents() {
        rowEffectiveDate = (Row) getFellow("rowEffectiveDate");
        cbEffectiveTime = (Combobox) getFellow("cbEffectiveTime");
        dEffectiveDate = (Datebox) getFellow("dEffectiveDate");
        lbClients = (Listbox) getFellow("lbClients");
        lbProducts = (Listbox) getFellow("lbProducts");
        lbMembers = (Listbox) getFellow("lbMembers");
        pgClients = (Paging) getFellow("pgClients");
        pgMembers = (Paging) getFellow("pgMembers");
        bbClient = (Bandbox) getFellow("bbClient");
        bbProduct = (Bandbox) getFellow("bbProduct");
        bbMember = (Bandbox) getFellow("bbMember");
        tCurrentIndex = (Textbox) getFellow("tCurrentIndex");
        tCurrentSequence = (Textbox) getFellow("tCurrentSequence");

        cbEffectiveTime.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (cbEffectiveTime.getSelectedIndex()==0) {
                    rowEffectiveDate.setVisible(true);
                } else {
                    rowEffectiveDate.setVisible(false);
                }
            }
        });

        dEffectiveDate.setValue(new Date());
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

    private void populateMembers(int offset, int limit) {
        ProductPOJO productPOJO = (ProductPOJO) bbProduct.getAttribute("e");
        lbMembers.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String q0 = "select count(*) ";
            String q1 = "select "
                    + "hdt1idxno, hdt1seqno, hdt1name ";
            String q2 = "from "
                    + "idnhltpf.dbo.hltdt1 ";
            String q3 = "";
            String q4 = "order by hdt1name asc ";

            if (productPOJO!=null) {
                q3 = "where "
                        + "hdt1yy=" + productPOJO.getProductYear() + " "
                        + "and hdt1pono=" + productPOJO.getProductId() + " "
                        + "and hdt1ctr=0 ";
            }

            if (!Libs.nn(whereMembers).isEmpty()) {
                if (!q3.isEmpty()) q3 += "and (" + whereMembers + ") ";
                else q3 += "where (" + whereMembers + ") ";
            }

            Integer rc = (Integer) s.createSQLQuery(q0 + q2 + q3).uniqueResult();
            pgMembers.setTotalSize(rc);

            List<Object[]> l = s.createSQLQuery(q1 + q2 + q3 + q4).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                MemberPOJO e = new MemberPOJO();
                e.setIndex(Integer.valueOf(Libs.nn(o[0])));
                e.setSequence(Libs.nn(o[1]));
                e.setName(Libs.nn(o[2]).trim());

                Listitem li = new Listitem();
                li.setValue(e);

                li.appendChild(new Listcell(Libs.nn(e.getIndex())));
                li.appendChild(new Listcell(e.getSequence()));
                li.appendChild(new Listcell(e.getName()));

                lbMembers.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateMembers", ex);
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
            populateMembers(0, pgMembers.getPageSize());
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

    public void quickSearchMembers() {
        Textbox tQuickSearchMembers = (Textbox) getFellow("tQuickSearchMembers");

        if (!tQuickSearchMembers.getText().isEmpty()) {
            whereMembers = "hdt1name like '%" + tQuickSearchMembers.getText() + "%' or "
                    + "(convert(varchar,hdt1idxno)+'-'+hdt1seqno)='" + tQuickSearchMembers.getText() + "' ";
            populateMembers(0, pgMembers.getPageSize());
        } else {
            refreshMembers();
        }
    }

    public void refreshMembers() {
        whereMembers = null;
        populateMembers(0, pgMembers.getPageSize());
    }

    public void memberSelected() {
        if (lbMembers.getSelectedCount()==1) {
            MemberPOJO memberPOJO = lbMembers.getSelectedItem().getValue();
            tCurrentIndex.setText(Libs.nn(memberPOJO.getIndex()));
            tCurrentSequence.setText(memberPOJO.getSequence());
            bbMember.setText(memberPOJO.getName());
            bbMember.setAttribute("e", memberPOJO);
            bbMember.close();
        }
    }

    public void deleteMember() {
        if (Messagebox.show("Are you sure you want to delete this member?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            ProductPOJO productPOJO = (ProductPOJO) bbProduct.getAttribute("e");
            MemberPOJO memberPOJO = (MemberPOJO) bbMember.getAttribute("e");

            if (cbEffectiveTime.getSelectedIndex()==0) {
                Session s = Libs.sfDB.openSession();
                try {
                    String q = "insert into imcs.dbo.advanced_delete "
                            + "values ("
                            + productPOJO.getProductYear() + ", "
                            + productPOJO.getProductId() + ", "
                            + memberPOJO.getIndex() + ", "
                            + "'" + memberPOJO.getSequence() + "', "
                            + "'" + dEffectiveDate.getText() + "') ";

                    s.createSQLQuery(q).executeUpdate();
                    s.beginTransaction().commit();

                    Messagebox.show("Member will be deleted on " + dEffectiveDate.getText(), "Information", Messagebox.OK, Messagebox.INFORMATION);
                } catch (Exception ex) {
                    log.error("deleteMember", ex);
                } finally {
                    s.close();
                }
            } else {
                Session s = Libs.sfDB.openSession();
                try {
                    String q = "update [DB] "
                            + "set "
                            + "hdt2moe='U' "
                            + "where "
                            + "hdt2yy=" + productPOJO.getProductYear() + " "
                            + "and hdt2pono=" + productPOJO.getProductId() + " "
                            + "and hdt2idxno=" + memberPOJO.getIndex() + " "
                            + "and hdt2seqno='" + memberPOJO.getSequence() + "' "
                            + "and hdt2ctr=0 ";

                    s.createSQLQuery(q.replace("[DB]", "idnhltpf.dbo.hltdt2")).executeUpdate();
                    s.beginTransaction().commit();

                    Messagebox.show("Member has been deleted", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } catch (Exception ex) {
                    log.error("deleteMember", ex);
                } finally {
                    s.close();
                }
            }
        }
    }

}
