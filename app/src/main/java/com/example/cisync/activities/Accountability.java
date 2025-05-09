package com.example.cisync.activities;

public class Accountability {
    private String feeName;
    private String amount;
    private boolean isPaid;

    public Accountability(String feeName, String amount, boolean isPaid) {
        this.feeName = feeName;
        this.amount = amount;
        this.isPaid = isPaid;
    }

    public String getFeeName() {
        return feeName;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return isPaid;
    }
}