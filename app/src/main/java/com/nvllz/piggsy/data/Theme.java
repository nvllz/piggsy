package com.nvllz.piggsy.data;

public enum Theme {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    public static final Theme[] themes;
    public final String VALUE;

    static {
        themes = values();
    }

    Theme(String value) {
        this.VALUE = value;
    }

    public static String[] getValues() {
        String[] array = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            array[i] = values()[i].VALUE;
        }
        return array;
    }
}