package com.eipna.centsation.data;

public enum DateFormat {
    EEEE_MMMM_DD_YYYY("Monday, December 31 2012", "EEEE, MMMM dd yyyy"),
    MM_DD_YYYY("12/31/2012", "MM/dd/yyyy"),
    DD_MM_YYYY("32/12/2012", "dd/MM/yyyy"),
    YYYY_DD_MM("2012/31/12", "yyyy/dd/MM"),
    YYYY_MM_DD("2012/12/31", "yyyy/MM/dd"),
    YYYY_MM_DD_ISO("2012-12-31", "yyyy-MM-dd");

    private static final DateFormat[] dateFormats;

    static {
        dateFormats = values();
    }

    public final String NAME;
    public final String PATTERN;

    DateFormat(String NAME, String PATTERN) {
        this.NAME = NAME;
        this.PATTERN = PATTERN;
    }

    public static String getNameByPattern(String pattern) {
        for (DateFormat dateFormat : dateFormats) {
            if (dateFormat.PATTERN.equals(pattern)) {
                return dateFormat.NAME;
            }
        }
        return DateFormat.YYYY_MM_DD_ISO.NAME;
    }

    public static String[] getNames() {
        String[] strings = new String[values().length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = dateFormats[i].NAME;
        }
        return strings;
    }

    public static String[] getPatterns() {
        String[] strings = new String[values().length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = dateFormats[i].PATTERN;
        }
        return strings;
    }
}