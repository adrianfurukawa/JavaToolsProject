package com.imc.imctools.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by faizal on 12/26/13.
 */
public class MemberPOJO {

    private Integer index;
    private String sequence;
    private String name;
    private PlanPOJO ip;
    private PlanPOJO op;
    private PlanPOJO mt;
    private PlanPOJO dt;
    private PlanPOJO gl;
    private String status;
    private CompanyPOJO companyPOJO;
    private Double basicSalary;
    private MemberPOJO spouse;
    private List<MemberPOJO> children = new ArrayList<MemberPOJO>();

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompanyPOJO getCompanyPOJO() {
        return companyPOJO;
    }

    public void setCompanyPOJO(CompanyPOJO companyPOJO) {
        this.companyPOJO = companyPOJO;
    }

    public PlanPOJO getIp() {
        return ip;
    }

    public void setIp(PlanPOJO ip) {
        this.ip = ip;
    }

    public PlanPOJO getOp() {
        return op;
    }

    public void setOp(PlanPOJO op) {
        this.op = op;
    }

    public PlanPOJO getMt() {
        return mt;
    }

    public void setMt(PlanPOJO mt) {
        this.mt = mt;
    }

    public PlanPOJO getDt() {
        return dt;
    }

    public void setDt(PlanPOJO dt) {
        this.dt = dt;
    }

    public PlanPOJO getGl() {
        return gl;
    }

    public void setGl(PlanPOJO gl) {
        this.gl = gl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public MemberPOJO getSpouse() {
        return spouse;
    }

    public void setSpouse(MemberPOJO spouse) {
        this.spouse = spouse;
    }

    public List<MemberPOJO> getChildren() {
        return children;
    }

    public void setChildren(List<MemberPOJO> children) {
        this.children = children;
    }

}
