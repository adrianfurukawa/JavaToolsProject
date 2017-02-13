package com.imc.imctools.pojo;

import java.util.Date;

/**
 * Created by faizal on 1/20/14.
 */
public class ProductPOJO {

    private ClientPOJO clientPOJO;
    private String productId;
    private String productYear;
    private String productName;
    private Date startingDate;
    private Date effectiveDate;
    private Date matureDate;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductYear() {
        return productYear;
    }

    public void setProductYear(String productYear) {
        this.productYear = productYear;
    }

    public ClientPOJO getClientPOJO() {
        return clientPOJO;
    }

    public void setClientPOJO(ClientPOJO clientPOJO) {
        this.clientPOJO = clientPOJO;
    }

    public Date getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(Date startingDate) {
        this.startingDate = startingDate;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getMatureDate() {
        return matureDate;
    }

    public void setMatureDate(Date matureDate) {
        this.matureDate = matureDate;
    }

}
