package com.eipna.centsation.data.saving;


import java.util.Comparator;

public class Saving {

    private String ID;
    private String name;
    private double currentSaving;
    private double goal;
    private String description;
    private int isArchived;
    private long deadline;

    public static int IS_ARCHIVE = 1;
    public static int NOT_ARCHIVE = 0;

    public Saving() {
        this.ID = null;
        this.name = null;
        this.currentSaving = 0.0;
        this.goal = 0.0;
        this.description = null;
        this.isArchived = 0;
        this.deadline = 0;
    }

    public static final Comparator<Saving> SORT_NAME = Comparator.comparing(firstSaving -> firstSaving.getName().toLowerCase());

    public static final Comparator<Saving> SORT_VALUE = Comparator.comparingDouble(Saving::getCurrentSaving);

    public static final Comparator<Saving> SORT_GOAL = Comparator.comparingDouble(Saving::getGoal);

    public static final Comparator<Saving> SORT_DEADLINE = Comparator.comparingLong(Saving::getDeadline);

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCurrentSaving() {
        return currentSaving;
    }

    public void setCurrentSaving(double currentSaving) {
        this.currentSaving = currentSaving;
    }

    public double getGoal() {
        return goal;
    }

    public void setGoal(double goal) {
        this.goal = goal;
    }

    public int getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(int isArchived) {
        this.isArchived = isArchived;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }
}