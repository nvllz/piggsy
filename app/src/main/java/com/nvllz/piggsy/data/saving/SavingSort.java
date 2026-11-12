package com.nvllz.piggsy.data.saving;


public enum SavingSort {

    NAME("sort_name"),
    VALUE("sort_value"),
    GOAL("sort_goal"),
    DEADLINE("sort_deadline"),
    CREATION("sort_creation"),
    ASCENDING("sort_ascending"),
    DESCENDING("sort_descending");

    private static final SavingSort[] sorts;
    public final String SORT;

    static {
        sorts = values();
    }

    SavingSort(String sort) {
        this.SORT = sort;
    }
}