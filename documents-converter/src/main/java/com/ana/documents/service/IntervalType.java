package com.ana.documents.service;

public enum IntervalType {

    FACTURA("9350", "9351", "9352", "9353", "9354", "9355", "9356", "9357", "9358"),
    NOTA_DE_CREDITO("9359");

    private String[] numbers;

    private IntervalType(String... numbers) {
        this.numbers = numbers;
    }

    public String[] numbers() {
        return this.numbers;
    }
}
