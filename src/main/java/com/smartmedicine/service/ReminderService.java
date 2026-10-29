package com.smartmedicine.service;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.util.Config;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Background scheduler (one daemon thread). Every 15 seconds it re-reads today's pending doses of ACTIVE medicines
 * into the {@link ReminderQueue}, fires alarms that are due, and marks overdue doses as MISSED. Because the queue is
 * rebuilt from the database, edited/deactivated medicines and taken doses are cancelled automatically; calling
 * {@link #reschedule()} makes this happen immediately. UI callbacks always run on the JavaFX Application Thread.
 * Reminders only work while the application is running.
 */
public final class ReminderService {
    private static final int TICK_SECONDS = 15;

    private final DoseService doseService;
    private final ReminderQueue queue = new ReminderQueue();
    private ScheduledExecutorService executor;
    private volatile long userId;
    private volatile Consumer<DoseRecord> alarmListener = d -> { };
    private volatile Runnable refreshListener = () -> { };
    private LocalDate lastDay;

    public ReminderService(DoseService doseService) { this.doseService = doseService; }

    public synchronized void start(long userId, Consumer<DoseRecord> onAlarm, Runnable onRefresh) {
        stop();
        this.userId = userId;
        this.alarmListener = onAlarm;
        this.refreshListener = onRefresh;
        this.lastDay = null;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-scheduler");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::tick, 0, TICK_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        queue.clear();
        queue.clearFired();
    }

    /** Re-reads the schedule right away (call after a medicine was added, edited, deactivated or a dose changed). */
    public synchronized void reschedule() {
        if (executor != null && !executor.isShutdown()) executor.execute(this::tick);
    }

    public List<DoseRecord> upcomingSnapshot() { return queue.snapshot(); }

    private void tick() {
        try {
            LocalDate today = LocalDate.now();
            if (!today.equals(lastDay)) {            // first tick, or a new day started while the app was open
                lastDay = today;
                queue.clearFired();
                doseService.purgeOldHistory();
            }
            doseService.ensureTodayDoses(userId);
            queue.rebuild(doseService.getPendingActiveToday(userId));
            List<DoseRecord> due = queue.pollDue(LocalDateTime.now(), Duration.ofMinutes(Config.GRACE_MINUTES));
            doseService.refreshStatuses(userId);
            Consumer<DoseRecord> alarm = alarmListener;
            Runnable refresh = refreshListener;
            for (DoseRecord d : due) Platform.runLater(() -> alarm.accept(d));
            Platform.runLater(refresh);
        } catch (Throwable t) {                        // never let the scheduled task die
            System.err.println("Reminder check failed: " + t.getMessage());
        }
    }
}
