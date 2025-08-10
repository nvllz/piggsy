package com.nvllz.piggsy.data.transaction;

public class Transaction {

    private int ID;
    private String savingID;
    private double amount;
    private String type;
    private long date;
    private String note;

    public Transaction() {
        this.ID = -1;
        this.savingID = null;
        this.amount = -1;
        this.type = null;
        this.date = -1;
        this.note = null;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setSavingID(String savingID) {
        this.savingID = savingID;
    }

    public String getSavingID() {
        return savingID;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}