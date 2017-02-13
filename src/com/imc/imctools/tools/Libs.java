package com.imc.imctools.tools;

import com.imc.imctools.pojo.*;
import com.imc.imctools.structures.Rule;
import com.imc.imctools.structures.Section;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.commons.digester3.Digester;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Calendar;

/**
 * Created by faizal on 11/27/13.
 */
public class Libs {

    private static Logger log = LoggerFactory.getLogger(Libs.class);
    public static Properties config;
    public static SessionFactory sfDB;
    public static SessionFactory sfEDC;
    public static String[] romanMonth = new String[] { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII" };
    public static String[] months = new String[] { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };

    public static final int DB = 1;
    public static final int AS400 = 2;
    public static final int EDC = 3;

    public static String nn(Object o) {
        if (o==null) return ""; else return o.toString();
    }

    public static Listcell createBooleanListcell(boolean value) {
        Listcell lc = new Listcell();
        final Checkbox cbx = new Checkbox();
        cbx.setChecked(value);

        cbx.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                cbx.setChecked(!cbx.isChecked());
            }
        });

        lc.appendChild(cbx);
        lc.setStyle("text-align:center;");
        return lc;
    }

    public static Listcell createBooleanListcell(boolean value, boolean editable) {
        Listcell lc = new Listcell();
        final Checkbox cbx = new Checkbox();
        cbx.setChecked(value);

        if (!editable) {
            cbx.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    cbx.setChecked(!cbx.isChecked());
                }
            });
        }

        lc.appendChild(cbx);
        lc.setStyle("text-align:center;");
        return lc;
    }

    public static Listcell createProgressListcell(int value) {
        Progressmeter p = new Progressmeter();
        p.setValue(value);
        Listcell lc = new Listcell();
        lc.appendChild(p);
        p.setWidth("99%");
        return lc;
    }

    public static Listcell createNumericListcell(Double d, String format) {
        Listcell lc = new Listcell(new DecimalFormat(format).format(d));
        lc.setStyle("text-align:right;");
        return lc;
    }

    public static Listcell createNumericListcell(Integer i, String format) {
        Listcell lc = new Listcell(new DecimalFormat(format).format(i));
        lc.setStyle("text-align:right;");
        return lc;
    }

    public static int getDiffDays(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);

        long d1ms = cal1.getTimeInMillis();
        long d2ms = cal2.getTimeInMillis();

        return (int) (Math.ceil((d2ms-d1ms) / (float)(24*3600*1000)));
    }

    public static void createRow(HSSFRow row, Object[] value, CellStyle style) {
        for (int i=0; i<value.length; i++) {
            HSSFCell cell = row.createCell(i);
            if (style!=null) cell.setCellStyle(style);

            if (value[i] instanceof Double || value[i] instanceof Integer || value[i] instanceof BigDecimal || value[i] instanceof Short) {
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(Double.valueOf(value[i].toString()));
            } else {
                cell.setCellType(Cell.CELL_TYPE_STRING);
                cell.setCellValue(value[i].toString());
            }
        }
    }

    public static String runningFields(String fieldName, int count) {
        String result = "";
        for (int i=0; i<count; i++) {
            result += fieldName + (i+1) + ", ";
        }
        if (result.endsWith(", ")) result = result.substring(0, result.length()-2);
        return result;
    }

    public static String runningAddFields(String fieldName, int count) {
        String result = "";
        for (int i=0; i<count; i++) {
            result += fieldName + (i+1) + "+";
        }
        if (result.endsWith("+")) result = result.substring(0, result.length()-1);
        return result;
    }

    public static String getShortClaimType(String claimType) {
        if (claimType.toLowerCase().equals("inpatient")) return "I";
        if (claimType.toLowerCase().equals("outpatient")) return "O";
        if (claimType.toLowerCase().equals("maternity")) return "R";
        if (claimType.toLowerCase().equals("dental")) return "D";
        if (claimType.toLowerCase().equals("glasses")) return "G";
        return "";
    }

    public static List<String> getPlanItemPosition(CompanyPOJO companyPOJO, String plan) {
        List<String> planItemPosition = new ArrayList<String>();

        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + runningFields("hbftbcd", 30) + " "
                    + "from idnhltpf.dbo.hltbft "
                    + "where "
                    + "hbftyy=" + companyPOJO.getYear() + " "
                    + "and hbftpono=" + companyPOJO.getPolicyNumber() + " "
                    + "and hbftcode='" + plan + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                for (int i=0; i<30; i++) {
                    String item = nn(o[i]);
                    planItemPosition.add(item);
                }
            }
        } catch (Exception ex) {
            log.error("getPlanItemPosition", ex);
        } finally {
            s.close();
        }

        return planItemPosition;
    }

    public static List<String> getPlanItemPosition(ProductPOJO productPOJO, String plan) {
        List<String> planItemPosition = new ArrayList<String>();

        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + runningFields("hbftbcd", 30) + " "
                    + "from idnhltpf.dbo.hltbft "
                    + "where "
                    + "hbftyy=" + productPOJO.getProductYear() + " "
                    + "and hbftpono=" + productPOJO.getProductId() + " "
                    + "and hbftcode='" + plan + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                for (int i=0; i<30; i++) {
                    String item = nn(o[i]);
                    planItemPosition.add(item);
                }
            }
        } catch (Exception ex) {
            log.error("getPlanItemPosition", ex);
        } finally {
            s.close();
        }

        return planItemPosition;
    }

    public static Rule loadRule(CompanyPOJO companyPOJO) {
        Rule rule = null;
        try {
            Digester d = new Digester();
            d.addObjectCreate("rule", Rule.class);
            d.addObjectCreate("rule/section", Section.class);
            d.addSetProperties("rule/section");
            d.addBeanPropertySetter("rule/section", "contents");
            d.addSetNext("rule/section", "addSection");

            File fileRule = new File(Executions.getCurrent().getSession().getWebApp().getRealPath("rules/" + companyPOJO.getYear() + "-1-0-" + companyPOJO.getPolicyNumber() + ".rule.xml"));
            rule = d.parse(new FileInputStream(fileRule));
        } catch (Exception ex) {
            log.error("loadRule", ex);
        }
        return rule;
    }

//    ***************

    public static CompanyPOJO getCompanyByPolicyNumber(int year, int policyNumber) {
        CompanyPOJO companyPOJO = null;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "hhdryy, hhdrpono, hhdrname "
                    + "from idnhltpf.dbo.hlthdr "
                    + "where "
                    + "hhdryy=" + year + " "
                    + "and hhdrpono=" + policyNumber + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                companyPOJO = new CompanyPOJO();
                companyPOJO.setYear(Integer.valueOf(nn(o[0])));
                companyPOJO.setPolicyNumber(Integer.valueOf(nn(o[1])));
                companyPOJO.setName(nn(o[2]));
            }
        } catch (Exception ex) {
            log.error("getCompanyByPolicyNumber", ex);
        } finally {
            s.close();
        }
        return companyPOJO;
    }

    public static ClientPOJO getClientById(String id) {
        ClientPOJO clientPOJO = null;
        Session s = sfDB.openSession();
        try {
            String q = "select "
                    + "hinsid, hinsname "
                    + "from idnhltpf.dbo.inslf "
                    + "where "
                    + "hinsid='" + id + "' ";

            Object[] o = (Object[]) s.createSQLQuery(q).uniqueResult();
            if (o!=null) {
                clientPOJO = new ClientPOJO();
                clientPOJO.setClientId(id);
                clientPOJO.setClientName(nn(o[1]));
            }
        } catch (Exception ex) {
            log.error("getClientById", ex);
        } finally {
            s.close();
        }
        return clientPOJO;
    }

    public static ProviderPOJO getProviderById(int id) {
        ProviderPOJO providerPOJO = null;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "hpronomor, hproname "
                    + "from idnhltpf.dbo.hltpro "
                    + "where "
                    + "hpronomor=" + id + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                providerPOJO = new ProviderPOJO();
                providerPOJO.setProviderCode(Integer.valueOf(nn(o[0])));
                providerPOJO.setName(nn(o[1]));
            }
        } catch (Exception ex) {
            log.error("getProviderById", ex);
        } finally {
            s.close();
        }
        return providerPOJO;
    }

    public static String getMemberNameByIndex(int productYear, int productId, int index, String sequence) {
        String result = "";
        Session s = sfDB.openSession();
        try {
            String q = "select hdt1name "
                    + "from idnhltpf.dbo.hltdt1 "
                    + "where "
                    + "hdt1yy=" + productYear + " "
                    + "and hdt1pono=" + productId + " "
                    + "and hdt1idxno=" + index + " "
                    + "and hdt1seqno='" + sequence + "' "
                    + "and hdt1ctr=0 ";

            result = nn(s.createSQLQuery(q).uniqueResult());
        } catch (Exception ex) {
            log.error("getMemberNameByIndex", ex);
        } finally {
            s.close();
        }

        return result;
    }

    public static MemberPOJO getMemberByIndex(CompanyPOJO companyPOJO, int index, String sequence) {
        MemberPOJO memberPOJO = null;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hdt1name, a.hdt1ncard, "
                    + "b.hdt2plan1, b.hdt2plan2, b.hdt2plan3, b.hdt2plan4, b.hdt2plan5, "
                    + "b.hdt2moe, "
                    + "a.hdt1yy, a.hdt1pono, c.hhdrname, "
                    + "a.hdt1idxno, a.hdt1seqno "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hltdt2 b on b.hdt2yy=a.hdt1yy and b.hdt2pono=a.hdt1pono and b.hdt2idxno=a.hdt1idxno and b.hdt2seqno=a.hdt1seqno and b.hdt2ctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hlthdr c on c.hhdryy=a.hdt1yy and c.hhdrpono=a.hdt1pono "
                    + "where "
                    + "a.hdt1yy=" + companyPOJO.getYear() + " "
                    + "and a.hdt1pono=" + companyPOJO.getPolicyNumber() + " "
                    + "and a.hdt1idxno=" + index + " "
                    + "and a.hdt1seqno='" + sequence + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                memberPOJO = new MemberPOJO();
                memberPOJO.setName(nn(o[0]));
                memberPOJO.setCompanyPOJO(companyPOJO);
                memberPOJO.setIndex(Integer.valueOf(Libs.nn(o[11])));
                memberPOJO.setSequence(Libs.nn(o[12]));
                memberPOJO.setIp(getPlanById(companyPOJO, nn(o[2])));
                memberPOJO.setOp(getPlanById(companyPOJO, nn(o[3])));
                memberPOJO.setMt(getPlanById(companyPOJO, nn(o[4])));
                memberPOJO.setDt(getPlanById(companyPOJO, nn(o[5])));
                memberPOJO.setGl(getPlanById(companyPOJO, nn(o[6])));
                memberPOJO.setStatus(nn(o[7]));
                memberPOJO.setBasicSalary(getBasicSalary(memberPOJO));
            }
        } catch (Exception ex) {
            log.error("getMemberById", ex);
        } finally {
            s.close();
        }
        return memberPOJO;
    }

    public static PlanPOJO getPlanById(CompanyPOJO companyPOJO, String id) {
        PlanPOJO planPOJO = null;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "hbftcode, hbftlmtamt, "
                    + runningFields("hbftbcd", 30) + ", "
                    + runningFields("hbftbpln", 30) + " "
                    + "from idnhltpf.dbo.hltbft "
                    + "where "
                    + "hbftyy=" + companyPOJO.getYear() + " "
                    + "and hbftpono=" + companyPOJO.getPolicyNumber() + " "
                    + "and hbftcode='" + id + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                planPOJO = new PlanPOJO();
                planPOJO.setPlanCode(nn(o[0]));
                planPOJO.setLimit(Double.valueOf(nn(o[1])));

                for (int i=0; i<30; i++) {
                    BenefitPOJO benefitPOJO = new BenefitPOJO();

                    benefitPOJO.setBenefitCode(nn(o[i+2]));
                    benefitPOJO.setLimit(Double.valueOf(nn(o[i + 32])));
                    planPOJO.getBenefits().add(benefitPOJO);
                }
            }
        } catch (Exception ex) {
            log.error("getPlanById", ex);
        } finally {
            s.close();
        }
        return planPOJO;
    }

    public static PlanPOJO getChangePlanById(CompanyPOJO companyPOJO, String id) {
        PlanPOJO planPOJO = null;
        Session s = sfEDC.openSession();
        try {
            String qry = "select "
                    + "plan1, plan2 "
                    + "from edc_prj.dbo.changeplan "
                    + "where "
                    + "no_polis=" + companyPOJO.getPolicyNumber() + " "
                    + "and plan1='" + id + "' "
                    + "and flg='1' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                planPOJO = getPlanById(companyPOJO, nn(o[1]).trim());
            }
        } catch (Exception ex) {
            log.error("getChangePlanById", ex);
        } finally {
            s.close();
        }
        return planPOJO;
    }

    public static Map<String,String> getBenefitDescriptionMap(String ids) {
        Map<String,String> result = new HashMap<String,String>();
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "hrefcode1+hrefcode2 as code, "
                    + "hrefabbr, hrefdesc21+hrefdesc22 as descr "
                    + "from idnhltpf.dbo.hltref "
                    + "where "
                    + "hrefcode1+hrefcode2 in (" + ids + ") ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                result.put(nn(o[0]), nn(o[2]).trim());
            }
        } catch (Exception ex) {
            log.error("getBenefitDescriptionMap", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public static ICDPOJO getICDById(String id) {
        ICDPOJO icdPOJO = null;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "icd_code, description "
                    + "from imcs.dbo.icds "
                    + "where "
                    + "icd_code='" + id + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                icdPOJO = new ICDPOJO();
                icdPOJO.setIcdCode(nn(o[0]).trim());
                icdPOJO.setDescription(nn(o[1]).trim());
            }
        } catch (Exception ex) {
            log.error("getICDById", ex);
        } finally {
            s.close();
        }
        return icdPOJO;
    }

    public static List<ICDPOJO> getICDGroupById(String id) {
        List<ICDPOJO> result = new ArrayList<ICDPOJO>();
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "icd_code, description "
                    + "from imcs.dbo.icds "
                    + "where "
                    + "icd_code like '" + id + "%' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                ICDPOJO icdPOJO = new ICDPOJO();
                icdPOJO.setIcdCode(nn(o[0]).trim());
                icdPOJO.setDescription(nn(o[1]).trim());

                result.add(icdPOJO);
            }
        } catch (Exception ex) {
            log.error("getICDGroupById", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public static List<ICDPOJO> getICDByRangeId(String start, String end) {
        List<ICDPOJO> result = new ArrayList<ICDPOJO>();
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "icd_code, description "
                    + "from imcs.dbo.icds "
                    + "where "
                    + "icd_code between '" + start + "' and '" + end + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                ICDPOJO icdPOJO = new ICDPOJO();
                icdPOJO.setIcdCode(nn(o[0]).trim());
                icdPOJO.setDescription(nn(o[1]).trim());

                result.add(icdPOJO);
            }
        } catch (Exception ex) {
            log.error("getICDById", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public static double[] getTotalBenefitsAmount(Listbox lb) {
        double totalProposed = 0D;
        double totalApproved = 0D;

        for (Listitem li : lb.getItems()) {
            double proposed = ((Doublebox) ((Listcell) li.getChildren().get(4)).getChildren().get(0)).getValue();
            double approved = ((Doublebox) ((Listcell) li.getChildren().get(6)).getChildren().get(0)).getValue();

            totalProposed += proposed;
            totalApproved += approved;
        }

        return new double[] { totalProposed, totalApproved };
    }

    public static double[] getTotalBenefitsAmountMinusLI(Listbox lb, Listitem liRow) {
        double totalProposed = 0D;
        double totalApproved = 0D;

        for (Listitem li : lb.getItems()) {
            if (!li.equals(liRow)) {
                double proposed = ((Doublebox) ((Listcell) li.getChildren().get(4)).getChildren().get(0)).getValue();
                double approved = ((Doublebox) ((Listcell) li.getChildren().get(6)).getChildren().get(0)).getValue();

                totalProposed += proposed;
                totalApproved += approved;
            }
        }

        return new double[] { totalProposed, totalApproved };
    }

    public static double[] getTotalBenefitsAmountHistory(MemberPOJO memberPOJO) {
        Session s = sfDB.openSession();
        double totalProposed = 0;
        double totalApproved = 0;
        try {
            String qry = "select "
                    + "sum(" + runningAddFields("hclmcamt", 30) + ") as proposed, "
                    + "sum(" + runningAddFields("hclmaamt", 30) + ") as approved "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmyy=" + memberPOJO.getCompanyPOJO().getYear() + " "
                    + "and hclmpono=" + memberPOJO.getCompanyPOJO().getPolicyNumber() + " "
                    + "and hclmidxno=" + memberPOJO.getIndex() + " "
                    + "and hclmseqno='" + memberPOJO.getSequence() + "' "
                    + "and hclmrecid<>'C' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                if (nn(o[0]).isEmpty()) o[0] = 0;
                if (nn(o[1]).isEmpty()) o[1] = 0;

                totalProposed = Double.valueOf(nn(o[0]));
                totalApproved = Double.valueOf(nn(o[1]));
            }
        } catch (Exception ex) {
            log.error("getTotalBenefitsAmountHistory", ex);
        } finally {
            s.close();
        }

        return new double[] { totalProposed, totalApproved };
    }

    public static double[] getTotalBenefitsAmountHistoryClaimType(MemberPOJO memberPOJO, String claimType) {
        Session s = sfDB.openSession();
        double totalProposed = 0;
        double totalApproved = 0;
        try {
            String qry = "select "
                    + "sum(" + runningAddFields("hclmcamt", 30) + ") as proposed, "
                    + "sum(" + runningAddFields("hclmaamt", 30) + ") as approved "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmyy=" + memberPOJO.getCompanyPOJO().getYear() + " "
                    + "and hclmpono=" + memberPOJO.getCompanyPOJO().getPolicyNumber() + " "
                    + "and hclmidxno=" + memberPOJO.getIndex() + " "
                    + "and hclmseqno='" + memberPOJO.getSequence() + "' "
                    + "and hclmrecid<>'C' "
                    + "and hclmtclaim='" + claimType + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                if (nn(o[0]).isEmpty()) o[0] = 0;
                if (nn(o[1]).isEmpty()) o[1] = 0;

                totalProposed = Double.valueOf(nn(o[0]));
                totalApproved = Double.valueOf(nn(o[1]));
            }
        } catch (Exception ex) {
            log.error("getTotalBenefitsAmountHistory", ex);
        } finally {
            s.close();
        }

        return new double[] { totalProposed, totalApproved };
    }

    public static double[] getBenefitValues(Listbox lb, String benefitCode) {
        double[] result = new double[] { 0, 0 };

        for (Listitem li : lb.getItems()) {
            BenefitPOJO benefitPOJO = (BenefitPOJO) li.getValue();

            if (benefitPOJO.getBenefitCode().equals(benefitCode)) {
                double proposed = ((Doublebox) ((Listcell) li.getChildren().get(4)).getChildren().get(0)).getValue();
                double approved = ((Doublebox) ((Listcell) li.getChildren().get(6)).getChildren().get(0)).getValue();

                result[0] = proposed;
                result[1] = approved;
            }
        }

        return result;
    }

    public static Double getBasicSalary(MemberPOJO memberPOJO) {
        Double result = 0D;
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "salary "
                    + "from imcs.dbo.basic_salaries "
                    + "where "
                    + "hdt1yy=" + memberPOJO.getCompanyPOJO().getYear() + " "
                    + "and hdt1pono=" + memberPOJO.getCompanyPOJO().getPolicyNumber() + " "
                    + "and hdt1idxno=" + memberPOJO.getIndex() + " "
                    + "and hdt1seqno='" + memberPOJO.getSequence() + "' ";

            result = ((BigDecimal) s.createSQLQuery(qry).uniqueResult()).doubleValue();
        } catch (Exception ex) {
            log.error("getBasicSalary", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public static void populateMemberFamily(MemberPOJO memberPOJO) {
        Session s = sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hdt1name, a.hdt1ncard, "
                    + "b.hdt2plan1, b.hdt2plan2, b.hdt2plan3, b.hdt2plan4, b.hdt2plan5, "
                    + "b.hdt2moe, "
                    + "a.hdt1yy, a.hdt1pono, c.hhdrname, "
                    + "a.hdt1idxno, a.hdt1seqno "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hltdt2 b on b.hdt2yy=a.hdt1yy and b.hdt2pono=a.hdt1pono and b.hdt2idxno=a.hdt1idxno and b.hdt2seqno=a.hdt1seqno and b.hdt2ctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hlthdr c on c.hhdryy=a.hdt1yy and c.hhdrpono=a.hdt1pono "
                    + "where "
                    + "a.hdt1yy=" + memberPOJO.getCompanyPOJO().getYear() + " "
                    + "and a.hdt1pono=" + memberPOJO.getCompanyPOJO().getPolicyNumber() + " "
                    + "and a.hdt1idxno=" + memberPOJO.getIndex() + " ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                String sequence = nn(o[12]);
                if (!sequence.equals(memberPOJO.getSequence())) {
                    MemberPOJO familyMemberPOJO = new MemberPOJO();
                    familyMemberPOJO = new MemberPOJO();
                    familyMemberPOJO.setName(nn(o[0]));
                    familyMemberPOJO.setCompanyPOJO(memberPOJO.getCompanyPOJO());
                    familyMemberPOJO.setIndex(Integer.valueOf(Libs.nn(o[11])));
                    familyMemberPOJO.setSequence(Libs.nn(o[12]));
                    familyMemberPOJO.setIp(getPlanById(memberPOJO.getCompanyPOJO(), nn(o[2])));
                    familyMemberPOJO.setOp(getPlanById(memberPOJO.getCompanyPOJO(), nn(o[3])));
                    familyMemberPOJO.setMt(getPlanById(memberPOJO.getCompanyPOJO(), nn(o[4])));
                    familyMemberPOJO.setDt(getPlanById(memberPOJO.getCompanyPOJO(), nn(o[5])));
                    familyMemberPOJO.setGl(getPlanById(memberPOJO.getCompanyPOJO(), nn(o[6])));
                    familyMemberPOJO.setStatus(nn(o[7]));
                    familyMemberPOJO.setBasicSalary(memberPOJO.getBasicSalary());

                    if (sequence.equals("B") || sequence.equals("C")) {
                        memberPOJO.setSpouse(familyMemberPOJO);
                    } else {
                        memberPOJO.getChildren().add(familyMemberPOJO);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("populateMemberFamily", ex);
        } finally {
            s.close();
        }
    }

    public static boolean checkICDException(CompanyPOJO companyPOJO, String ICD) {
        boolean result = false;
        Session s = sfEDC.openSession();
        try {
            String qry = "select count(*) "
                    + "from edc_prj.dbo.edc_endorsmentbypolis "
                    + "where "
                    + "thnpolis=" + companyPOJO.getYear() + " "
                    + "and nopolis=" + companyPOJO.getPolicyNumber() + " "
                    + "and endorsmentid='" + ICD + "' "
                    + "and flg='1' ";

            Integer cnt = (Integer) s.createSQLQuery(qry).uniqueResult();
            if (cnt>0) result = true;
        } catch (Exception ex) {
            log.error("checkICDException", ex);
        } finally {
            s.close();
        }
        return result;
    }

    public static Object getCellAt(HSSFRow row, int col) {
        Object o = null;
        HSSFCell cell = row.getCell(col);
        if (cell.getCellType()==Cell.CELL_TYPE_STRING) o = cell.getStringCellValue();
        if (cell.getCellType()==Cell.CELL_TYPE_NUMERIC) o = cell.getNumericCellValue();
        return o;
    }

    public static void populateCombobox(Combobox cb, String[] items) {
        cb.getItems().clear();
        for (String s : items) {
            cb.appendItem(s);
        }
    }

    public static String loadReportGenerator(File f) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            while (br.ready()) {
                result += br.readLine();
            }
        } catch (Exception ex) {
            log.error("loadReportGenerator", ex);
        }
        return result;
    }

    public static Cell createCell(org.apache.poi.ss.usermodel.Row row, int cnt, Object value) {
        Cell cell = row.createCell(cnt);

        try {
            double d = (Double) value;
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellValue(d);
        } catch (Exception ex) {
            cell.setCellValue(Libs.nn(value));
        }

        return cell;
    }

    public static void createCell(XSSFRow row, int cnt, Object value) {
        XSSFCell cell = row.createCell(cnt);

        try {
            double d = (Double) value;
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellValue(d);
        } catch (Exception ex) {
            cell.setCellValue(Libs.nn(value));
        }
    }

    public static int getNumberOfEDC(int providerCode) {
        int result = 0;
        Session s3 = sfEDC.openSession();
        try {
            String q = "select count(*) "
                    + "from edc_prj.dbo.edc_terminal "
                    + "where "
                    + "pro_code=" + providerCode + " ";

            result = (Integer) s3.createSQLQuery(q).uniqueResult();
        } catch (Exception ex) {
            log.error("getNumberOfEDC", ex);
        } finally {
            s3.close();
        }
        return result;
    }

    public static Integer[] getEDCUsageCount(int tid, int providerCode) {
        Integer[] result = new Integer[] { 0, 0 };

        Session s3 = sfEDC.openSession();
        try {
            String q = "select "
                    + "a.tid, b.provider_code from edc_prj.dbo.ms_log_transaction a "
                    + "inner join edc_prj.dbo.request_ref# b on b.ref#=a.transaction_id "
                    + "where tid=" + tid;

            List<Object[]> l = s3.createSQLQuery(q).list();
            result[0] = l.size();

            for (Object[] o : l) {
                if (Integer.valueOf(Libs.nn(o[1])).intValue()==providerCode) result[1]++;
            }
        } catch (Exception ex) {
            log.error("getEDCUsageCount", ex);
        } finally {
            s3.close();
        }

        return result;
    }

    public static BankAccountPOJO getBankAccount(int policyYear, int policyNumber, int idx) {
        BankAccountPOJO bankAccountPOJO = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "no_peserta, acc_no, acc_name, acc_bank, "
                    + "acc_bank_address1 + acc_bank_address2, bi_code, email "
                    + "from idnhltpf.dbo.account_member "
                    + "where "
                    + "thn_polis=" + policyYear + " "
                    + "and no_polis=" + policyNumber + " "
                    + "and idx_polis=" + idx + " ";

            //Messagebox.show(q);
            
            List<Object[]> l = s.createSQLQuery(q).list();
            if (l.size()>0) {
                Object[] o = l.get(0);
                bankAccountPOJO = new BankAccountPOJO();
                bankAccountPOJO.setPolicyYear(policyYear);
                bankAccountPOJO.setPolicyNumber(policyNumber);
                bankAccountPOJO.setIdx(idx);
                bankAccountPOJO.setCardNumber(Libs.nn(o[0]).trim());
                bankAccountPOJO.setAccountNumber(Libs.nn(o[1]).trim());
                bankAccountPOJO.setAccountName(Libs.nn(o[2]).trim());
                bankAccountPOJO.setBank(Libs.nn(o[3]).trim());
                bankAccountPOJO.setAddress(Libs.nn(o[4]).trim());
                bankAccountPOJO.setBiCode(Libs.nn(o[5]).trim());
                bankAccountPOJO.setEmail(Libs.nn(o[6]).trim());
            }
        } catch (Exception ex) {
            log.error("getBankAccount", ex);
        } finally {
            s.close();
        }

        return bankAccountPOJO;
    }

    public static BankAccountPOJO getProviderBankAccount(int providerCode) {
        BankAccountPOJO bankAccountPOJO = null;
        Session s = Libs.sfDB.openSession();
        try {
            String q = "select "
                    + "hprobaccno, hprobaccnm, "
                    + "(hprobaddr1 + hprobaddr2 + hprobaddr3) as address, "
                    + "hprobother, hprobicd, hprobbank, HPROBOTHER "
                    + "from idnhltpf.dbo.hltprobank "
                    + "where "
                    + "hprobnomor=" + providerCode + " ";

            List<Object[]> l = s.createSQLQuery(q).list();
            if (l.size()>0) {
                Object[] o = l.get(0);
                bankAccountPOJO = new BankAccountPOJO();
                bankAccountPOJO.setAccountNumber(Libs.nn(o[0]).trim());
                bankAccountPOJO.setAccountName(Libs.nn(o[1]).trim());
                bankAccountPOJO.setBank(Libs.nn(o[5]).trim());
                bankAccountPOJO.setAddress(Libs.nn(o[2]).trim());
                bankAccountPOJO.setBiCode(Libs.nn(o[4]).trim());
                bankAccountPOJO.setEmail(Libs.nn(o[3]).trim());
            }
        } catch (Exception ex) {
            log.error("getProviderBankAccount", ex);
        } finally {
            s.close();
        }

        return bankAccountPOJO;
    }

    public static boolean executeUpdate(String q, int table) {
        boolean result = false;
        switch (table) {
            case DB:
                Session s = sfDB.openSession();
                try {
                    s.createSQLQuery(q).executeUpdate();
                    s.beginTransaction().commit();
                    s.flush();
                    result = true;
                } catch (Exception ex) {
                    log.error("executeUpdate", ex);
                } finally {
                    s.close();
                }
                break;
            case EDC:
                Session s3 = sfEDC.openSession();
                try {
                    s3.createSQLQuery(q).executeUpdate();
                    s3.beginTransaction().commit();
                    s3.flush();
                    result = true;
                } catch (Exception ex) {
                    log.error("executeUpdate", ex);
                } finally {
                    s3.close();
                }
        }

        return result;
    }

    public static String fixDate(String date) {
        String[] dateSeg = date.split("\\-");

        dateSeg[1] = "0" + dateSeg[1];
        dateSeg[1] = dateSeg[1].substring(dateSeg[1].length()-2);

        dateSeg[2] = "0" + dateSeg[2];
        dateSeg[2] = dateSeg[2].substring(dateSeg[2].length()-2);

        return dateSeg[0] + dateSeg[1] + dateSeg[2];
    }

    public static boolean showErrorForm(String message) {
        Messagebox.show(message, "Error", Messagebox.OK, Messagebox.ERROR);
        return false;
    }

    public static String getUsername() {
        return nn(Executions.getCurrent().getSession().getAttribute("user")).toUpperCase();
    }

    public static void printReport(String reportName, Map parameters) {
        try {
        	//Messagebox.show("ambil report");
            InputStream input = new FileInputStream(new File(Executions.getCurrent().getSession().getWebApp().getRealPath("/resources/reports/" + reportName + ".jrxml")));
            JasperDesign jasperDesign = JRXmlLoader.load(input);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            OutputStream output = new FileOutputStream(new File(Libs.config.get("temp_dir") + File.separator + reportName + ".pdf"));
            JasperExportManager.exportReportToPdfStream(jasperPrint, output);
            output.close();

            File f = new File(Libs.config.get("temp_dir") + File.separator + reportName + ".pdf");
            InputStream is = new FileInputStream(f);
            Filedownload.save(is, "application/pdf", reportName + ".pdf");
            f.delete();
        } catch(Exception ex) {
            log.error("printReport", ex);
        }
    }

    public static String createInsertString(String table, Object[] values) {
        String q = "insert into " + table + " values (";
        for (Object o : values) {
            if (o instanceof String) q += "'" + o + "', ";
            else q += o + ", ";
        }
        if (q.endsWith(", ")) q = q.substring(0, q.length()-2);
        q += ") ";
        return q;
    }

    public static String createCardNumber(String policy, String index, String sequence) {
        String result = "";
        index = "00000" + index;
        result = "10201" + policy + index.substring(index.length()-5) + ((byte) sequence.charAt(0)-64);
        return result;
    };

    public static boolean useAS400() {
        if (Libs.nn(Libs.config.get("use_as400")).equals("true")) return true; else return false;
    }

}
