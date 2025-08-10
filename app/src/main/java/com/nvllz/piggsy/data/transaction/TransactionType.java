package com.nvllz.piggsy.data.transaction;

public enum TransactionType {

    CREATED("Created"),
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw");

    private static final TransactionType[] types;
    public final String VALUE;

    static {
        types = values();
    }

    TransactionType(String value) {
        this.VALUE = value;
    }
}