package com.imc.imctools.controllers;

import org.zkoss.zul.Listbox;
import org.zkoss.zul.Window;

/**
 * Created by faizal on 3/14/14.
 */
public class IMCBSEditorController extends Window {

    private Listbox lb;

    public void onCreate() {
        initComponents();
        populateScripts();
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
    }

    private void populateScripts() {
        lb.getItems().clear();
    }

}
