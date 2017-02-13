package com.imc.imctools.controllers;

import com.imc.imctools.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by faizal on 1/21/14.
 */
public class FixNoDataController extends Window {

    private Logger log = LoggerFactory.getLogger(FixNoDataController.class);
    private Textbox tClaimNumber;
    private int fixes = 0;

    public void onCreate() {
        initComponents();
    }

    private void initComponents() {
        tClaimNumber = (Textbox) getFellow("tClaimNumber");
    }

    public void fixNoData() {
        String claimNumber = tClaimNumber.getText();
        if (claimNumber.isEmpty()) Messagebox.show("Please input Claim Number!", "Error", Messagebox.OK, Messagebox.ERROR);
        else {
//            Check from AS400
            if (!checkClaimNumberFromAS400(claimNumber)) Messagebox.show("Claim does not exist in AS400!", "Error", Messagebox.OK, Messagebox.ERROR);
            else {
//                Check from DB
                List<Object[]> replicatedClaims = checkClaimNumberFromDB(claimNumber);
                if (replicatedClaims==null || replicatedClaims.size()==0) {
//                    Claims have not been replicated
                    if (Messagebox.show("The Claim has not been replicated. Do you want to replicate now?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION, Messagebox.CANCEL)==Messagebox.OK) {
                        replicatedClaims = replicateClaim(claimNumber);
                    }
                }
//                Check for consistency in each claims
                if (replicatedClaims!=null) {
                    for (Object[] o : replicatedClaims) {
                        checkProvider(o);
                        checkMember(o);
                    }
                }

                if (fixes>0) {
                    Messagebox.show("Analysis completed. Claim has been fixed", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } else {
                    Messagebox.show("Analysis completed. Nothing was fixed", "Warning", Messagebox.OK, Messagebox.EXCLAMATION);
                }
            }
        }
    }

    private boolean checkClaimNumberFromAS400(String claimNumber) {
        boolean result = false;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select count(*) "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmcno='" + claimNumber + "' ";

            Integer rc = (Integer) s.createSQLQuery(q).uniqueResult();
            if (rc>0) result = true;
        } catch (Exception ex) {
            log.error("checkClaimNumberFromAS400", ex);
        } finally {
            s.close();
        }
        return result;
    }

    private List<Object[]> checkClaimNumberFromDB(String claimNumber) {
        List<Object[]> result = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "hclmyy, hclmpono, hclmidxno, hclmseqno, hclmnhoscd "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmcno='" + claimNumber + "' ";

            result = s.createSQLQuery(q).list();
        } catch (Exception ex) {
            log.error("checkClaimNumberFromDB", ex);
        } finally {
            s.close();
        }
        return result;
    }

    private List<Object[]> replicateClaim(String claimNumber) {
        List<Object[]> result = null;

        List<Object[]> claims = null;
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "HCLMRECID, HCLMNOMOR, "
                    + "HCLMYY, HCLMBR, HCLMDIST, HCLMPONO, "
                    + "HCLMIDXNO, HCLMSEQNO, HCLMCOUNT, HCLMTCLAIM, "
                    + "HCLMONFLAG, HCLMCNO, HCLMHOSCD, HCLMNHOSCD, "
                    + "HCLMDISCD1, HCLMDISCD2, HCLMDISCD3, "
                    + "HCLMPCODE1, HCLMPCODE2, "
                    + "HCLMCDATEY, HCLMCDATEM, HCLMCDATED, "
                    + "HCLMPDATEY, HCLMPDATEM, HCLMPDATED, "
                    + "HCLMRDATEY, HCLMRDATEM, HCLMRDATED, HCLMSCHED, "
                    + "HCLMSINYY, HCLMSINMM, HCLMSINDD, "
                    + "HCLMSOUTYY, HCLMSOUTMM, HCLMSOUTDD, "
                    + Libs.runningFields("HCLMCDAY", 30) + ", "
                    + Libs.runningFields("HCLMADAY", 30) + ", "
                    + Libs.runningFields("HCLMCAMT", 30) + ", "
                    + Libs.runningFields("HCLMAAMT", 30) + ", "
                    + Libs.runningFields("HCLMREF", 30) + ", "
                    + "HCLMRCVDTY, HCLMRCVDTM, HCLMRCVDTD, "
                    + "HCLMUPDDD, HCLMUPDMM, HCLMUPDYY, "
                    + "HCLMPRGID, HCLMUSRID, HCLMFUNC "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmcno='" + claimNumber + "' ";

            claims = s.createSQLQuery(qry).list();
        } catch (Exception ex) {
            log.error("replicateClaim", ex);
        } finally {
            s.close();
        }

        if (claims!=null) {
            result = new ArrayList<Object[]>();

            for (Object[] o : claims) {
                String qry = "insert into idnhltpf.dbo.hltclm ("
                        + "HCLMRECID, HCLMNOMOR, "
                        + "HCLMYY, HCLMBR, HCLMDIST, HCLMPONO, "
                        + "HCLMIDXNO, HCLMSEQNO, HCLMCOUNT, HCLMTCLAIM, "
                        + "HCLMONFLAG, HCLMCNO, HCLMHOSCD, HCLMNHOSCD, "
                        + "HCLMDISCD1, HCLMDISCD2, HCLMDISCD3, "
                        + "HCLMPCODE1, HCLMPCODE2, "
                        + "HCLMCDATEY, HCLMCDATEM, HCLMCDATED, "
                        + "HCLMPDATEY, HCLMPDATEM, HCLMPDATED, "
                        + "HCLMRDATEY, HCLMRDATEM, HCLMRDATED, HCLMSCHED, "
                        + "HCLMSINYY, HCLMSINMM, HCLMSINDD, "
                        + "HCLMSOUTYY, HCLMSOUTMM, HCLMSOUTDD, "
                        + Libs.runningFields("HCLMCDAY", 30) + ", "
                        + Libs.runningFields("HCLMADAY", 30) + ", "
                        + Libs.runningFields("HCLMCAMT", 30) + ", "
                        + Libs.runningFields("HCLMAAMT", 30) + ", "
                        + Libs.runningFields("HCLMREF", 30) + ", "
                        + "HCLMRCVDTY, HCLMRCVDTM, HCLMRCVDTD, "
                        + "HCLMUPDDD, HCLMUPDMM, HCLMUPDYY, "
                        + "HCLMPRGID, HCLMUSRID, HCLMFUNC "
                        + ") values ("
                        + "'" + o[0] + "'," + o[1] + ","
                        + o[2] + "," + o[3] + "," + o[4] + "," + o[5] + ","
                        + o[6] + ",'" + o[7] + "'," + o[8] + ",'" + o[9] + "',"
                        + "'" + o[10] + "','" + o[11] + "','" + o[12] + "'," + o[13] + ","
                        + "'" + o[14] + "','" + o[15] + "','" + o[16] + "',"
                        + "'" + o[17] + "','" + o[18] + "',"
                        + o[19] + "," + o[20] + "," + o[21] + ","
                        + o[22] + "," + o[23] + "," + o[24] + ","
                        + o[25] + "," + o[26] + "," + o[27] + "," + o[28] + ","
                        + o[29] + "," + o[30] + "," + o[31] + ","
                        + o[32] + "," + o[33] + "," + o[34] + ",";

                for (int i=0; i<30; i++) qry += o[35+i] + ",";
                for (int i=0; i<30; i++) qry += o[65+i] + ",";
                for (int i=0; i<30; i++) qry += o[95+i] + ",";
                for (int i=0; i<30; i++) qry += o[125+i] + ",";
                for (int i=0; i<30; i++) qry += "'" + o[155+i] + "',";

                qry += o[185] + "," + o[186] + "," + o[187] + ","
                        + o[188] + "," + o[189] + "," + o[190] + ","
                        + "'" + o[191] + "','" + o[192] + "','" + o[193] + "'"
                        + ");";

                s = Libs.sfDB.openSession();
                try {
                    Object[] r = new Object[] { o[2], o[5], o[6], o[7], o[13] };
                    result.add(r);

                    s.createSQLQuery(qry).executeUpdate();
                    s.beginTransaction().commit();
                    replicateMemo(o);

                    fixes++;
                } catch (Exception ex) {
                    log.error("replicateClaim", ex);
                } finally {
                    if (s!=null && s.isOpen()) s.close();
                }
            }
        }

        return result;
    }

    private void replicateMemo(Object[] e) {
        List<Object[]> l = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "hmem2id, "
                    + "hmem2yy, hmem2br, hmem2dist, hmem2pono, "
                    + "hmem2idxno, hmem2seqno, "
                    + "hmem2claim, hmem2count, "
                    + "hmem2data1, hmem2data2, hmem2data3, hmem2data4, "
                    + "hmem2updyy, hmem2updmm, hmem2upddd, "
                    + "hmem2prgid, hmem2usrid "
                    + "from idnhltpf.dbo.hltmemo2 "
                    + "where "
                    + "hmem2yy=" + e[2] + " "
                    + "and hmem2pono=" + e[5] + " "
                    + "and hmem2idxno=" + e[6] + " "
                    + "and hmem2seqno='" + e[7] + "' "
                    + "and hmem2count=" + e[8] + " "
                    + "and hmem2claim='" + e[9] + "' ";

            l = s.createSQLQuery(q).list();
        } catch (Exception ex) {
            log.error("replicateMemo", ex);
        } finally {
            s.close();
        }

        if (l!=null) {
            for (Object[] o : l) {
                String q = "insert into idnhltpf.dbo.hltdt2 ("
                        + "hmem2id, "
                        + "hmem2yy, hmem2br, hmem2dist, hmem2pono, "
                        + "hmem2idxno, hmem2seqno, "
                        + "hmem2claim, hmem2count, "
                        + "hmem2data1, hmem2data2, hmem2data3, hmem2data4, "
                        + "hmem2updyy, hmem2updmm, hmem2upddd, "
                        + "hmem2prgid, hmem2usrid "
                        + ") values ("
                        + "'" + o[0] + "', "
                        + o[1] + ", " + o[2] + ", " + o[3] + ", " + o[4] + ", "
                        + o[5] + ", '" + o[6] + "', "
                        + "'" + o[7] + "', " + o[8] + ", "
                        + "'" + o[9] + "', '" + o[10] + "', '" + o[11] + "', '" + o[12] + "', "
                        + o[13] + ", " + o[14] + ", " + o[15] + ", "
                        + "'" + o[16] + "', '" + o[17] + "'"
                        + ") ";

                s = Libs.sfDB.openSession();
                try {
                    s.createSQLQuery(q).executeUpdate();
                    s.beginTransaction().commit();
                } catch (Exception ex) {
                    log.error("replicateMemo", ex);
                } finally {
                    s.close();
                }
            }
        }
    }

    private void checkProvider(Object[] e) {}

    private void checkMember(Object[] o) {}

}
