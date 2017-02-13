package com.imc.imctools.controllers;

import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.*;

import java.util.List;

/**
 * Created by faizal on 1/28/14.
 */
public class UpdateBenefitDescriptionController extends Window {

    private Logger log = LoggerFactory.getLogger(UpdateBenefitDescriptionController.class);
    private Listbox lb;

    private class BenefitItemPOJO {

        private String benefitCode;
        private String description;
        private boolean status;

        public String getBenefitCode() {
            return benefitCode;
        }

        public void setBenefitCode(String benefitCode) {
            this.benefitCode = benefitCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }

    }

    public void onCreate() {
        initComponents();
        populate();
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
    }

    private void populate() {
        lb.getItems().clear();
        Session s3 = Libs.sfEDC.openSession();
        try {
            String q = "select "
                    + "bft_code, bft_desc, proc_id "
                    + "from edc_prj.dbo.edc_benefit ";

            List<Object[]> l = s3.createSQLQuery(q).list();
            for (Object[] o : l) {
                boolean status = false;
                if (Libs.nn(o[2]).equals("1")) status = true;

                BenefitItemPOJO bPOJO = new BenefitItemPOJO();
                bPOJO.setBenefitCode(Libs.nn(o[0]).trim());
                bPOJO.setDescription(Libs.nn(o[1]).trim());
                bPOJO.setStatus(status);

                Listitem li = new Listitem();
                li.setValue(bPOJO);
                li.appendChild(new Listcell(bPOJO.getBenefitCode()));
                li.appendChild(new Listcell(bPOJO.getDescription()));
                li.appendChild(Libs.createBooleanListcell(bPOJO.isStatus(), true));

                lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            s3.close();
        }
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            for (int i=0; i<lb.getItemCount(); i++) {
                BenefitItemPOJO bPOJO = lb.getItems().get(i).getValue();
                if (bPOJO.getBenefitCode().toLowerCase().contains(val.toLowerCase()) || bPOJO.getDescription().toLowerCase().contains(val.toLowerCase())) {
                    lb.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public void clear() {
        if (Messagebox.show("Do you want to reset all update status?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            Session s3 = Libs.sfEDC.openSession();
            try {
                String q = "update edc_prj.dbo.edc_benefit "
                        + "set proc_id=0 "
                        + "where "
                        + "proc_id=1";

                s3.createSQLQuery(q).executeUpdate();
                s3.beginTransaction().commit();

                populate();
                Messagebox.show("Update status have been reset", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (Exception ex) {
                log.error("clear", ex);
            } finally {
                s3.close();
            }
        }
    }

    public void save() {
        if (Messagebox.show("Do you want to save this update status?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
            String list = "";
            for (Listitem li : lb.getItems()) {
                BenefitItemPOJO bPOJO = li.getValue();
                Checkbox cb = (Checkbox) (li.getChildren().get(2)).getChildren().get(0);
                if (cb.isChecked()) list += "'" + bPOJO.getBenefitCode() + "',";
            }
            if (list.endsWith(",")) list = list.substring(0, list.length()-1);
            if (!list.isEmpty()) {
                Session s3 = Libs.sfEDC.openSession();
                try {
                    String q = "update edc_prj.dbo.edc_benefit "
                            + "set proc_id=1 "
                            + "where bft_code in (" + list + ") ";

                    s3.createSQLQuery(q).executeUpdate();
                    s3.beginTransaction().commit();
                    Messagebox.show("Update status has been saved", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } catch (Exception ex) {
                    log.error("save", ex);
                    Messagebox.show("Error when saving update status", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } finally {
                    s3.close();
                }
            }
        }
    }

}
