package com.imc.imctools.controllers;

import com.imc.imctools.pojo.ProviderPOJO;
import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by faizal on 3/24/14.
 */
public class ProviderDetailController extends Window {

    private Logger log = LoggerFactory.getLogger(ProviderDetailController.class);
    private ProviderPOJO providerPOJO;
    private int mode;
    private Intbox iProviderId;
    private Textbox tProviderName;
    private Textbox tAddress1;
    private Textbox tAddress2;
    private Textbox tAddress3;
    private Textbox tPhoneArea1;
    private Textbox tPhoneArea2;
    private Textbox tPhoneArea3;
    private Textbox tPhone1;
    private Textbox tPhone2;
    private Textbox tPhone3;
    private Textbox tContactPerson1;
    private Textbox tContactPerson2;
    private Textbox tContactPerson3;
    private Textbox tPosition1;
    private Textbox tPosition2;
    private Textbox tPosition3;
    private Combobox cbProviderType;

    public void onCreate() {
        providerPOJO = (ProviderPOJO) getAttribute("providerPOJO");
        mode = (Integer) getAttribute("mode");
        
        initComponents();
        
        if (mode==1) populate();
    }

    private void initComponents() {
        iProviderId = (Intbox) getFellow("iProviderId");
        tProviderName = (Textbox) getFellow("tProviderName");
        tAddress1 = (Textbox) getFellow("tAddress1");
        tAddress2 = (Textbox) getFellow("tAddress2");
        tAddress3 = (Textbox) getFellow("tAddress3");
        tPhoneArea1 = (Textbox) getFellow("tPhoneArea1");
        tPhoneArea2 = (Textbox) getFellow("tPhoneArea2");
        tPhoneArea3 = (Textbox) getFellow("tPhoneArea3");
        tPhone1 = (Textbox) getFellow("tPhone1");
        tPhone2 = (Textbox) getFellow("tPhone2");
        tPhone3 = (Textbox) getFellow("tPhone3");
        tContactPerson1 = (Textbox) getFellow("tContactPerson1");
        tContactPerson2 = (Textbox) getFellow("tContactPerson2");
        tContactPerson3 = (Textbox) getFellow("tContactPerson3");
        tPosition1 = (Textbox) getFellow("tPosition1");
        tPosition2 = (Textbox) getFellow("tPosition2");
        tPosition3 = (Textbox) getFellow("tPosition3");
        cbProviderType = (Combobox) getFellow("cbProviderType");

        if (mode==0) {
//            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(true);
            iProviderId.setDisabled(false);
        } else {
//            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(false);
            iProviderId.setDisabled(true);
        }
    }

    private void populate() {
        iProviderId.setValue(providerPOJO.getProviderCode());
        tProviderName.setText(providerPOJO.getName());

        Object[] provider = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select * "
                    + "from idnhltpf.dbo.hltpro "
                    + "where "
                    + "hpronomor=" + providerPOJO.getProviderCode() + " ";

            provider = (Object[]) s.createSQLQuery(q).uniqueResult();
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s.close();
        }

        if (provider!=null) {
            tAddress1.setText(Libs.nn(provider[5]).trim());
            tAddress2.setText(Libs.nn(provider[6]).trim());
            tAddress3.setText(Libs.nn(provider[7]).trim());
            tPhoneArea1.setText(Libs.nn(provider[8]).trim());
            tPhone1.setText(Libs.nn(provider[9]).trim());
            tPhoneArea2.setText(Libs.nn(provider[10]).trim());
            tPhone2.setText(Libs.nn(provider[11]).trim());
            tPhoneArea3.setText(Libs.nn(provider[12]).trim());
            tPhone3.setText(Libs.nn(provider[13]).trim());
            tContactPerson1.setText(Libs.nn(provider[26]).trim());
            tContactPerson2.setText(Libs.nn(provider[27]).trim());
            tContactPerson3.setText(Libs.nn(provider[28]).trim());
            tPosition1.setText(Libs.nn(provider[29]).trim());
            tPosition2.setText(Libs.nn(provider[30]).trim());
            tPosition3.setText(Libs.nn(provider[31]).trim());

//            ((Toolbarbutton) getFellow("tbnDelete")).setDisabled(false);
            iProviderId.setDisabled(true);
        }
    }

    private void clear() {
        iProviderId.setValue(null);
        tProviderName.setValue(null);
        tAddress1.setValue(null);
        tAddress2.setValue(null);
        tAddress3.setValue(null);
        tPhoneArea1.setValue(null);
        tPhoneArea2.setValue(null);
        tPhoneArea3.setValue(null);
        tPhone1.setValue(null);
        tPhone2.setValue(null);
        tPhone3.setValue(null);
        tContactPerson1.setValue(null);
        tContactPerson2.setValue(null);
        tContactPerson3.setValue(null);
        tPosition1.setValue(null);
        tPosition2.setValue(null);
        tPosition3.setValue(null);
        cbProviderType.setSelectedIndex(0);
    }

    private void refreshParent() {
        ((ProvidersController) getAttribute("parent")).refresh();
    }

    public void newProvider() {
        if (Messagebox.show("Do you want to create new Provider?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            mode = 0;
            providerPOJO = null;
            clear();
            iProviderId.setDisabled(false);
        }
    }

//    public void delete() {
//        if (Messagebox.show("Do you want to delete this Provider?", "Warning", Messagebox.OK | Messagebox.CANCEL, Messagebox.EXCLAMATION, Messagebox.CANCEL)==Messagebox.OK) {
//            String q = "delete from [DB] "
//                    + "where "
//                    + "hpronomor=" + iProviderId.getValue() + " ";
//
//            boolean result = Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.DB);
//            if (result) {
//                Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.EDC);
//                Libs.executeUpdate(q.replace("[DB]", "idnhltpf.hltpro"), Libs.AS400);
//                refreshParent();
//                Messagebox.show("Provider has been deleted", "Information", Messagebox.OK, Messagebox.INFORMATION);
//                detach();
//            } else {
//                Messagebox.show("Error occured when deleting Provider", "Error", Messagebox.OK, Messagebox.ERROR);
//            }
//        }
//    }

    public void save() {
        if (Messagebox.show("Do you want to save this Provider?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            String cur = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String q = "";
            if (mode==0) {
                q = Libs.createInsertString("[DB]", new Object[] {
                        "", iProviderId.getValue(), "", "", tProviderName.getText(),
                        tAddress1.getText(), tAddress2.getText(), tAddress3.getText(),
                        tPhoneArea1.getText(), tPhone1.getText(), tPhoneArea2.getText(), tPhone2.getText(), tPhoneArea3.getText(), tPhone3.getText(),
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, "",
                        tContactPerson1.getText(), tContactPerson2.getText(), tContactPerson3.getText(),
                        tPosition1.getText(), tPosition2.getText(), tPosition3.getText(),
                        "", 0, 0, 0, cbProviderType.getText(), "",
                        cur.substring(0, 4), cur.substring(5, 7), cur.substring(8),
                        "HLT54EDC", Libs.getUsername(), "A"
                });
            } else {
                q = "update [DB] "
                        + "set "
                        + "hproname='" + tProviderName.getText() + "', "
                        + "hproaddr1='" + tAddress1.getText() + "', "
                        + "hproaddr2='" + tAddress2.getText() + "', "
                        + "hproaddr3='" + tAddress3.getText() + "', "
                        + "hproicode1='" + tPhoneArea1.getText() + "', "
                        + "hproicode2='" + tPhoneArea2.getText() + "', "
                        + "hproicode3='" + tPhoneArea3.getText() + "', "
                        + "hprophone1='" + tPhone1.getText() + "', "
                        + "hprophone2='" + tPhone2.getText() + "', "
                        + "hprophone3='" + tPhone3.getText() + "', "
                        + "hprocon1='" + tContactPerson1.getText() + "', "
                        + "hprocon2='" + tContactPerson2.getText() + "', "
                        + "hprocon3='" + tContactPerson3.getText() + "', "
                        + "hpropos1='" + tPosition1.getText() + "', "
                        + "hpropos2='" + tPosition2.getText() + "', "
                        + "hpropos3='" + tPosition3.getText() + "', "
                        + "hproclass='" + cbProviderType.getText() + "', "
                        + "hproupdyy=" + cur.substring(0, 4) + ", "
                        + "hproupdmm=" + cur.substring(5, 7) + ", "
                        + "hproupddd=" + cur.substring(8) + ", "
                        + "hprousrid='" + Libs.getUsername() + "', "
                        + "hprofunc='G' "
                        + "where "
                        + "hpronomor=" + iProviderId.getValue() + " ";
            }

            if (!q.isEmpty()) {
                boolean result = Libs.executeUpdate(q.replace("[DB]", "idnhltpf.dbo.hltpro"), Libs.DB);
                if (result) {
                    providerPOJO = new ProviderPOJO();
                    providerPOJO.setProviderCode(iProviderId.getValue());
                    providerPOJO.setName(tProviderName.getText());
                    providerPOJO.setAddress(tAddress1.getText() + tAddress2.getText() + tAddress3.getText());
                    populate();
                    refreshParent();
                    Messagebox.show("Provider has been saved", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } else {
                    Messagebox.show("Error occured when saving Provider", "Error", Messagebox.OK, Messagebox.ERROR);
                }
            }
        }
    }

}
