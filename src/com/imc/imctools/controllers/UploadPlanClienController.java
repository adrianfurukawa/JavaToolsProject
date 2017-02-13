package com.imc.imctools.controllers;


import com.imc.imctools.pojo.ClientPOJO;
import com.imc.imctools.pojo.ICDPOJO;
import com.imc.imctools.pojo.ProductPOJO;
import com.imc.imctools.tools.Libs;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by faizal on 3/12/14.
 */
@SuppressWarnings("serial")
public class UploadPlanClienController extends Window {

	
    private Logger log = LoggerFactory.getLogger(UploadPlanClienController.class);
    private Bandbox bbClient;
    private Bandbox bbProduct;
    private Object[] ihm;
    private Listbox lb;
    private Listbox lbClients;
    private Listbox lbProducts;
    private Textbox tPolicyNumber;
    private Textbox tPolicyYear;
    private Paging pg;
    private Paging pgClients;
    private String whereClients;
    private String whereProducts;
    private List<ICDPOJO> tempproductPln = new ArrayList<ICDPOJO>();
    private List<ICDPOJO> productPln = new ArrayList<ICDPOJO>();
   
    public void onCreate() {
        initComponents();
        populateClients(0, pgClients.getPageSize());
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        lbClients = (Listbox) getFellow("lbClients");
        lbProducts = (Listbox) getFellow("lbProducts");
        pg = (Paging) getFellow("pg");
        pgClients = (Paging) getFellow("pgClients");
        tPolicyNumber = (Textbox) getFellow("tPolicyNumber");
        tPolicyYear = (Textbox) getFellow("tPolicyYear");
        bbClient = (Bandbox) getFellow("bbClient");
        bbProduct = (Bandbox) getFellow("bbProduct");
       // tbnAddPlan.setDisabled(false);
        
        pgClients.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateClients(evt.getActivePage()*pgClients.getPageSize(), pgClients.getPageSize());
            }
        });

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage() * pg.getPageSize(), pg.getPageSize());
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

            if (whereClients!=null) q3 = "where " + whereClients;

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
//pertama pilih clien
    public void clientSelected() {
        if (lbClients.getSelectedCount()==1) {
            ClientPOJO clientPOJO = lbClients.getSelectedItem().getValue();
            bbClient.setText(clientPOJO.getClientName());
            bbClient.setAttribute("e", clientPOJO);
            populateProducts();
            bbClient.close();
        }
    }
// pilih polis
    public void productSelected() {
        if (lbProducts.getSelectedCount()>0) {
            ProductPOJO productPOJO = lbProducts.getSelectedItem().getValue();
            bbProduct.setText(productPOJO.getProductName());
            bbProduct.setAttribute("e", productPOJO);

            tPolicyNumber.setText(productPOJO.getProductId());
            tPolicyYear.setText(productPOJO.getProductYear());
            //populateproductPln();
            populate(0, pg.getPageSize());

            bbProduct.close();
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

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select PlanIMC,PlanClien from idnhltpf.dbo.planclien "
                    + "where polis='" + tPolicyNumber.getText() + "' and tahun ='" + tPolicyYear.getText() + "';";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {

            Listitem li = new Listitem();
            li.setValue(o);

            li.appendChild(new Listcell(Libs.nn(o[0])));
            li.appendChild(new Listcell(Libs.nn(o[1])));

            lb.appendChild(li);
        }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateproductPln() {
        productPln.clear();

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select PlanIMC,PlanClien from idnhltpf.dbo.planclien "
                    + "where polis='" + tPolicyNumber.getText() + "' and tahun ='" + tPolicyYear.getText() + "';";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                Listitem li = new Listitem();
                ICDPOJO c = Libs.getICDById(Libs.nn(o[0]));
                
                productPln.add(c);
                
               
            }
            pg.setTotalSize(l.size());
        } catch (Exception ex) {
            log.error("populateproductPln", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    public void clear() {
        if (Messagebox.show("Do you want to clear the list?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
        	
        	Session s = Libs.sfDB.openSession();
           try{
        	   
        	s.beginTransaction();
        	
        	String qry = " delete idnhltpf.dbo.planclien where  "
                    +"tahun ='"+ tPolicyYear.getText() +"' and polis='"+ tPolicyNumber.getText() +"' ";
            //Messagebox.show("clear :" + qry);
            s.createSQLQuery(qry).executeUpdate();
            s.flush();
            s.clear();
            
            s.getTransaction().commit();
            } catch (Exception ex) {
                log.error("saveProcess", ex);
            } finally {
                if (s!=null && s.isOpen()) s.close();
            }
            
            lb.getItems().clear();
            productPln.clear();
            pg.setTotalSize(0);
        }
    }

    private void refineproductPln() {
        Map<String,ICDPOJO> productICDMap = new HashMap<String,ICDPOJO>();

        for (ICDPOJO icdPOJO : tempproductPln) {
            if (icdPOJO!=null && productICDMap.get(icdPOJO.getIcdCode())==null) {
                productPln.add(icdPOJO);
                productICDMap.put(icdPOJO.getIcdCode(), icdPOJO);
            }
        }
    }


    public void importExceptions(UploadEvent evt) {
        if (Messagebox.show("Do you want to upload from " + evt.getMedia().getName() + "?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
           // tempproductPln.clear();
            Session s = Libs.sfDB.openSession();
            try {
            	s.beginTransaction();
                POIFSFileSystem fs = new POIFSFileSystem(evt.getMedia().getStreamData());
                HSSFWorkbook wb = new HSSFWorkbook(fs);
                HSSFSheet sheet = wb.getSheetAt(0);
  
                for (int i=0; i< sheet.getLastRowNum()+1; i++) {
                    HSSFRow row = sheet.getRow(i);
                    
				                    for (int a=0; a< row.getFirstCellNum()+1; a++) {
				                    	                    
				                    HSSFCell colom1 = row.getCell(0);
				                    HSSFCell colom2 = row.getCell(1);
				
				                    String PlanImc = colom1.getStringCellValue().trim();
				                    String PlanClien = colom2.getStringCellValue().trim();
				                    
				                    
				                    
				                    String qry = "insert into idnhltpf.dbo.planclien "
				                            +"(tahun,polis,PlanIMC,PlanClien) " 
				                            +"values "
				                            +" (" 
				                            +"'"+ tPolicyYear.getText() +"',"
				                            +"'"+ tPolicyNumber.getText() +"',"
				                            + "'" + PlanImc + "' ,"
				                            + "'" + PlanClien + "'  "
				                            +")";
				                    s.createSQLQuery(qry).executeUpdate();
				                    s.flush();
				                    s.clear();
				                    
				             }
                    }
                s.getTransaction().commit();
            } catch (Exception ex) {
            	s.getTransaction().rollback();
                log.error("importExceptions", ex);
            }
        }
        populate(0, pg.getPageSize());
    }
    

    
    
    private boolean validate() {
        boolean valid = true;

        if (bbClient.getAttribute("e")==null) {
            valid = Libs.showErrorForm("Please select Client!");
        } else if (bbProduct.getAttribute("e")==null) {
            valid = Libs.showErrorForm("Please select Product!");
        } else if (lb.getItems().size()==0) {
            valid = Libs.showErrorForm("Please add Plan to the list!");
        }

        return valid;
    }

    public void save() {
        if (validate()) {
            if (Messagebox.show("Do you want to save this Plan Clien ?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
                if (saveProcess()) {
                    Messagebox.show("Plan Clien has been saved", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } else {
                    Messagebox.show("Error occured while uploading. Please see log file", "Error", Messagebox.OK, Messagebox.ERROR);
                }
            }
        }
    }

    private boolean saveProcess() {
        boolean result = false;

        Session s = Libs.sfDB.openSession();
        try {
            s.beginTransaction();
            
            String sql = "select count(*) from idnhltpf.dbo.planclien "
                    + "where tahun=" + tPolicyYear.getText() + " "
                    + "and polis=" + tPolicyNumber.getText() + ";";
            Messagebox.show("ada data tidak :" + sql);
            //s.createSQLQuery(sql).executeUpdate();
            List<Object[]> l = s.createSQLQuery(sql).list();
            if (l != null){
              String qry = "delete from idnhltpf.dbo.planclien "
                    + "where tahun=" + tPolicyYear.getText() + " "
                    + "and polis=" + tPolicyNumber.getText() + ";";
            
            Messagebox.show("del dulu sebelum save :" + qry);
            s.createSQLQuery(qry).executeUpdate();
            s.flush();
            s.clear();

            
           // List<Object[]> l = lb.addEventListener();
            for (Object[] o : l) {
            
            //for (ICDPOJO icdPOJO : productPln) {
                //String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

                qry = "insert into idnhltpf.dbo.planclien "
                        +"(tahun	polis	PlanIMC	PlanClien) " 
                        +"values "
                        +" (" 
                        +" tPolicyYear.getText() + ,"
                        +" tPolicyNumber.getText() + ,"
                        //+ "'" + icdPOJO.getIcdCode() + "' ,"
                        //+ "'" + icdPOJO.getIcdCode() + "'  "
                        +")";
                Messagebox.show("input :" + qry);
                s.createSQLQuery(qry).executeUpdate();
                s.flush();
                s.clear();
            

            }    //result = true;
            }

            s.getTransaction().commit();
        } catch (Exception ex) {
            log.error("saveProcess", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        return result;
    }


    public void PlnSelected() {
        if (lb.getSelectedCount()==1) {
            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(false);
            }
    }

    public void delete() {
    	 if (validate()) {
        if (Messagebox.show("Do you want to remove this Plan ?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
        	ihm = (lb.getSelectedItem().getValue());
        	//Messagebox.show("cek :" +Libs.nn(ihm[0]) + Libs.nn(ihm[1]) );
        	
        	Session s = Libs.sfDB.openSession();
        	s.beginTransaction();
        	String qry = " delete idnhltpf.dbo.planclien where  "
                    +"tahun ='"+ tPolicyYear.getText() +"' and polis='"+ tPolicyNumber.getText() +"' and PlanIMC ='"+ Libs.nn(ihm[0]) + "' and PlanClien='"+ Libs.nn(ihm[1]) +"' ";
            //Messagebox.show("delete :" + qry);
            s.createSQLQuery(qry).executeUpdate();
            s.flush();
            s.clear();
            s.getTransaction().commit();
            
        	productPln.remove(lb.getSelectedItem().getValue());
            lb.removeChild(lb.getSelectedItem());
            
            Messagebox.show("Plan Clien has been removed", "Information", Messagebox.OK, Messagebox.INFORMATION);
        }
    }
    
    }


}
