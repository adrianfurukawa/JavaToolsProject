package com.imc.imctools.controllers;

import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import java.util.List;

/**
 * Created by faizal on 11/26/13.
 */
public class IndexController extends Window {

    private Logger log = LoggerFactory.getLogger(IndexController.class);
    private Textbox tUsername;
    private Textbox tPassword;

    public void onCreate() {
        initComponents();
    }

    private void initComponents() {
        tUsername = (Textbox) getFellow("tUsername");
        tPassword = (Textbox) getFellow("tPassword");

        tUsername.setFocus(true);
    }

    public void login() {
        boolean valid = false;

        Session s = Libs.sfEDC.openSession();
        try {
            String qry = "select * "
                    + "from EDC_PRJ.dbo.ms_user "
                    + "where userid='" + tUsername.getText() + "' and password='" + tPassword.getText() + "';";
            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) valid = true;
        } catch (Exception ex) {
            log.error("login", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        if (valid) {
            Executions.getCurrent().getSession().setAttribute("user", tUsername.getText());
            Executions.sendRedirect("main.zul");
        } else {
            Messagebox.show("Invalid username/password combination!", "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

}
