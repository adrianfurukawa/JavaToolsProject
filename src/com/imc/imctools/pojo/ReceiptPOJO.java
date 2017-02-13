package com.imc.imctools.pojo;

/**
 * Created by faizal on 12/18/13.
 */
public class ReceiptPOJO {

    private String internalNumber;
    private String receiptNumber;
    private String date;
    private String providerCompanyName;
    private String inputDate;
    private String proposed;

    public String getInternalNumber() {
        return internalNumber;
    }

    public void setInternalNumber(String internalNumber) {
        this.internalNumber = internalNumber;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProviderCompanyName() {
        return providerCompanyName;
    }

    public void setProviderCompanyName(String providerCompanyName) {
        this.providerCompanyName = providerCompanyName;
    }

    public String getInputDate() {
        return inputDate;
    }

    public void setInputDate(String inputDate) {
        this.inputDate = inputDate;
    }

    public String getProposed() {
        return proposed;
    }

    public void setProposed(String proposed) {
        this.proposed = proposed;
    }

}
