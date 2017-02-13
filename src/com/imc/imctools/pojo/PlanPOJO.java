package com.imc.imctools.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by faizal on 12/26/13.
 */
public class PlanPOJO {

    private String planCode;
    private Double limit;
    private List<BenefitPOJO> benefits = new ArrayList<BenefitPOJO>();

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public Double getLimit() {
        return limit;
    }

    public void setLimit(Double limit) {
        this.limit = limit;
    }

    public List<BenefitPOJO> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<BenefitPOJO> benefits) {
        this.benefits = benefits;
    }

}
