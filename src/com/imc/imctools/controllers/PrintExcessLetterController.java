package com.imc.imctools.controllers;

import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by faizal on 3/18/14.
 */
public class PrintExcessLetterController extends Window {

    private Logger log = LoggerFactory.getLogger(PrintExcessLetterController.class);
    private Datebox dReceiptDate;
    private Textbox tClaimNumber;
    private Textbox tDestination;
    private Textbox tNotes;
    private Intbox iIndex;
    private Intbox iCount;
    private Combobox cbSequence;
    private Doublebox dbExcessAmount;
    private Label lReceiptDate;
    private Label lDestination;
    private Label lExcessAmount;
    private Label lNotes;
    private Label lIndexSequence;
    private Object[] claim;
    private List<Object[]> claims;

    public void onCreate() {
        initComponents();
    }

    private void initComponents() {
        dReceiptDate = (Datebox) getFellow("dReceiptDate");
        tClaimNumber = (Textbox) getFellow("tClaimNumber");
        tDestination = (Textbox) getFellow("tDestination");
        tNotes = (Textbox) getFellow("tNotes");
        iIndex = (Intbox) getFellow("iIndex");
        iCount = (Intbox) getFellow("iCount");
        cbSequence = (Combobox) getFellow("cbSequence");
        dbExcessAmount = (Doublebox) getFellow("dbExcessAmount");
        lReceiptDate = (Label) getFellow("lReceiptDate");
        lDestination = (Label) getFellow("lDestination");
        lExcessAmount = (Label) getFellow("lExcessAmount");
        lNotes = (Label) getFellow("lNotes");
        lIndexSequence = (Label) getFellow("lIndexSequence");
    }

    public void loadClaim() {
        tClaimNumber.setText(tClaimNumber.getText().toUpperCase());

        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "hclmyy, hclmpono, hclmidxno, hclmseqno, "
                    + "hclmtclaim, "
                    + "(" + Libs.runningAddFields("hclmcamt", 30) + ") as proposed, "
                    + "(" + Libs.runningAddFields("hclmaamt", 30) + ") as approved, "
                    + "hhdrname, "
                    + "hclmsinyy, hclmsinmm, hclmsindd, "
                    + "hclmrdatey, hclmrdatem, hclmrdated, "
                    + "hmem2data1, hmem2data2, hmem2data3, hmem2data4, "
                    + "hclmcount "
                    + "from idnhltpf.dbo.hltclm "
                    + "inner join idnhltpf.dbo.hlthdr on hhdryy=hclmyy and hhdrpono=hclmpono "
                    + "left outer join idnhltpf.dbo.hltmemo2 on hmem2yy=hclmyy and hmem2pono=hclmpono and hmem2idxno=hclmidxno and hmem2seqno=hclmseqno and hmem2count=hclmcount and hmem2claim=hclmtclaim "
                    + "where "
                    + "hclmcno='" + tClaimNumber.getText() + "' ";

            claims = s.createSQLQuery(q).list();
            if (claims.size()==1) {
                claim = (Object[]) s.createSQLQuery(q).uniqueResult();
                iIndex.setValue(Integer.valueOf(Libs.nn(claim[2])));
                cbSequence.setText(Libs.nn(claim[3]));
                iCount.setValue(Integer.valueOf(Libs.nn(claim[18])));
                populate();
            } else if (claims.size()>1) {
                clear();
                lIndexSequence.setStyle("font-weight:bold;");
                iIndex.setDisabled(false);
                iCount.setDisabled(false);
                cbSequence.setDisabled(false);
            } else {
                iIndex.setValue(null);
                cbSequence.setText(null);
                iCount.setValue(null);
                iIndex.setDisabled(true);
                cbSequence.setDisabled(true);
                iCount.setDisabled(true);
                clear();
                Messagebox.show("Claim " + tClaimNumber.getText() + " does not exist!", "Error", Messagebox.OK, Messagebox.ERROR);
            }
        } catch (Exception ex) {
            log.error("loadClaim", ex);
        } finally {
            s.close();
        }
    }

    public void indexSequenceSelected() {
        boolean valid = false;

        for (Object[] o : claims) {
            if (Libs.nn(o[2]).equals(String.valueOf(iIndex.getText())) && Libs.nn(o[3]).equals(cbSequence.getText()) && Libs.nn(o[18]).equals(String.valueOf(iCount.getText()))) {
                valid = true;
                claim = o;
            }
        }

        if (valid) {
            populate();
        } else {
            clear();
            Messagebox.show("Claim " + tClaimNumber.getText() + " for index " + iIndex.getValue() + "-" + cbSequence.getText() + " Count " + iCount.getValue() + " does not exist!", "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private void populate() {
        double excessAmount = Double.valueOf(Libs.nn(claim[5])) - Double.valueOf(Libs.nn(claim[6]));
        String sinDate = Libs.nn(claim[8]) + "-" + Libs.nn(claim[9]) + "-" + Libs.nn(claim[10]);
        String receiptDate = Libs.nn(claim[11]) + "-" + Libs.nn(claim[12]) + "-" + Libs.nn(claim[13]);
        String memo = Libs.nn(claim[14]) + Libs.nn(claim[15]) + Libs.nn(claim[16]) + Libs.nn(claim[17]);

        dReceiptDate.setDisabled(false);
        tDestination.setDisabled(false);
        tNotes.setDisabled(false);
        dbExcessAmount.setDisabled(false);
        lReceiptDate.setStyle("font-weight:bold;");
        lDestination.setStyle("font-weight:bold;");
        lExcessAmount.setStyle("font-weight:bold;");
        lNotes.setStyle("font-weight:bold;");

        dbExcessAmount.setValue(excessAmount);
        if (Libs.nn(claim[4]).equals("I") || Libs.nn(claim[4]).equals("R")) {
            try {
                dReceiptDate.setValue(new SimpleDateFormat("yyyy-MM-dd").parse(sinDate));
            } catch (Exception ex) {}
        } else {
            try {
                dReceiptDate.setValue(new SimpleDateFormat("yyyy-MM-dd").parse(receiptDate));
            } catch (Exception ex) {}
        }
        tDestination.setText(Libs.nn(claim[7]));
        tNotes.setText(memo);
        ((Toolbarbutton) getFellow("tbnPrint")).setDisabled(false);
    }

    private void clear() {
        claim = null;

        dReceiptDate.setDisabled(true);
        tDestination.setDisabled(true);
        tNotes.setDisabled(true);
        dbExcessAmount.setDisabled(true);
        lReceiptDate.setStyle("font-style:italic;");
        lDestination.setStyle("font-style:italic;");
        lExcessAmount.setStyle("font-style:italic;");
        lNotes.setStyle("font-style:italic;");

        dbExcessAmount.setText(null);
        dReceiptDate.setText(null);
        tDestination.setText(null);
        tNotes.setText(null);

        ((Toolbarbutton) getFellow("tbnPrint")).setDisabled(true);
    }

    public void print() {
        Map params = new HashMap();

        params.put("hid_number", tClaimNumber.getText());
        params.put("policy_number", Libs.nn(claim[0]) + "-1-0-" + Libs.nn(claim[1]));
        params.put("policy_index", Libs.nn(claim[2]) + "-" + Libs.nn(claim[3]));
        params.put("employee_name", Libs.getMemberNameByIndex(Integer.valueOf(Libs.nn(claim[0])), Integer.valueOf(Libs.nn(claim[1])), Integer.valueOf(Libs.nn(claim[2])), "A"));
        params.put("member_name", Libs.getMemberNameByIndex(Integer.valueOf(Libs.nn(claim[0])), Integer.valueOf(Libs.nn(claim[1])), Integer.valueOf(Libs.nn(claim[2])), Libs.nn(claim[3])));
        params.put("unit_head_claim", "dr. Lynda Tri Hayuningtyas");
        params.put("amount", dbExcessAmount.getValue());
        params.put("aso_name", tDestination.getText());
        params.put("notes", tNotes.getText());
        params.put("receipt_date", dReceiptDate.getValue());

        Libs.printReport("ExcessStatementSheet", params);
    }

}
