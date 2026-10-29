package com.smartmedicine.service;

import com.smartmedicine.model.User;

/** Simple holder of the shared services and the logged-in user (the current session). */
public final class AppContext {
    private static final DoseService DOSES = new DoseService();
    private static final ReminderService REMINDER = new ReminderService(DOSES);
    private static final MedicineService MEDICINES = new MedicineService(DOSES, REMINDER::reschedule);
    private static final AuthService AUTH = new AuthService();
    private static User currentUser;

    private AppContext() { }

    public static DoseService doses() { return DOSES; }
    public static ReminderService reminder() { return REMINDER; }
    public static MedicineService medicines() { return MEDICINES; }
    public static AuthService auth() { return AUTH; }
    public static User user() { return currentUser; }
    public static void setUser(User user) { currentUser = user; }
}
