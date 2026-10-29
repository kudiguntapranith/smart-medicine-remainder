package com.smartmedicine.model;

/** Status shown to the user; derived from the stored status and the current time. */
public enum DisplayStatus {
    UPCOMING("Upcoming", "badge-upcoming"),
    DUE("Due", "badge-due"),
    TAKEN("Taken", "badge-taken"),
    MISSED("Missed", "badge-missed");

    private final String label;
    private final String cssClass;

    DisplayStatus(String label, String cssClass) { this.label = label; this.cssClass = cssClass; }

    public String label() { return label; }
    public String cssClass() { return cssClass; }
    public boolean isPending() { return this == UPCOMING || this == DUE; }
    /** History screen only distinguishes Taken / Missed / Pending. */
    public String historyLabel() { return isPending() ? "Pending" : label; }
    public String historyCssClass() { return isPending() ? "badge-pending" : cssClass; }
}
