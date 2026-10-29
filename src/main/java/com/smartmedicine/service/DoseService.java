package com.smartmedicine.service;

import com.smartmedicine.dao.HistoryDao;
import com.smartmedicine.dao.MedicineDao;
import com.smartmedicine.database.DatabaseManager;
import com.smartmedicine.model.DisplayStatus;
import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.MedicineSchedule;
import com.smartmedicine.util.Config;
import com.smartmedicine.util.ValidationException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Dose generation, status changes, history queries and retention. Each dose is its own record. */
public class DoseService {
    private final MedicineDao medicineDao = new MedicineDao();
    private final HistoryDao historyDao = new HistoryDao();

    /**
     * Creates the missing dose records of one day for all active medicines of the user. Safe to call repeatedly:
     * the UNIQUE(schedule_id, scheduled_date) key plus ON DUPLICATE KEY prevents duplicates. A dose earlier than
     * the schedule's creation time is never generated, so adding a medicine at 3 PM does not create a "missed" 8 AM dose.
     */
    public int generateForDate(Connection c, long userId, LocalDate date) throws SQLException {
        int created = 0;
        for (Medicine m : medicineDao.findAllByUser(c, userId)) {
            if (!m.isScheduledOn(date)) continue;
            for (MedicineSchedule s : medicineDao.findSchedules(c, m.id())) {
                if (!s.active()) continue;
                if (date.atTime(s.reminderTime()).isBefore(s.createdAt())) continue;
                created += historyDao.insertIfAbsent(c, userId, m, s, date);
            }
        }
        return created;
    }

    public void ensureTodayDoses(long userId) {
        DatabaseManager.inTransaction(c -> generateForDate(c, userId, LocalDate.now()));
    }

    /** Persists MISSED for pending doses older than the grace period. */
    public void refreshStatuses(long userId) {
        DatabaseManager.withConnection(c -> markOverdue(c, userId));
    }

    private int markOverdue(Connection c, long userId) throws SQLException {
        return historyDao.markOverdueMissed(c, userId, LocalDateTime.now().minusMinutes(Config.GRACE_MINUTES));
    }

    public List<DoseRecord> getTodayDoses(long userId) {
        return DatabaseManager.inTransaction(c -> {
            generateForDate(c, userId, LocalDate.now());
            markOverdue(c, userId);
            return historyDao.findByDate(c, userId, LocalDate.now());
        });
    }

    public List<DoseRecord> getPendingActiveToday(long userId) {
        return DatabaseManager.withConnection(c -> historyDao.findPendingActiveByDate(c, userId, LocalDate.now()));
    }

    public void markTaken(long userId, long doseId) {
        boolean ok = DatabaseManager.withConnection(c ->
                historyDao.updateStatus(c, userId, doseId, DoseStatus.TAKEN, LocalDateTime.now()));
        if (!ok) throw new ValidationException("Dose not found.");
    }

    public void markMissed(long userId, long doseId) {
        DatabaseManager.withConnection(c -> {
            DoseRecord d = historyDao.findById(c, userId, doseId)
                    .orElseThrow(() -> new ValidationException("Dose not found."));
            if (d.scheduledDateTime().isAfter(LocalDateTime.now()))
                throw new ValidationException("An upcoming dose cannot be marked as missed yet.");
            return historyDao.updateStatus(c, userId, doseId, DoseStatus.MISSED, null);
        });
    }

    /** History of the last 30 days (today included), optionally filtered. Range is clamped to that window. */
    public List<DoseRecord> getHistory(long userId, LocalDate from, LocalDate to, Long medicineId, DoseStatus status) {
        LocalDate today = LocalDate.now();
        LocalDate earliest = today.minusDays(Config.HISTORY_DAYS - 1L);
        LocalDate f = (from == null || from.isBefore(earliest)) ? earliest : from;
        LocalDate t = (to == null || to.isAfter(today)) ? today : to;
        if (f.isAfter(t)) throw new ValidationException("The 'from' date must not be after the 'to' date.");
        return DatabaseManager.withConnection(c -> {
            markOverdue(c, userId);
            return historyDao.findRange(c, userId, f, t, medicineId, status);
        });
    }

    /** Retention policy: only the last 30 days (today-29 .. today) are kept; older records are deleted. */
    public int purgeOldHistory() {
        LocalDate oldestKept = LocalDate.now().minusDays(Config.HISTORY_DAYS - 1L);
        return DatabaseManager.withConnection(c -> historyDao.purgeOlderThan(c, oldestKept));
    }

    /** Taken / missed / pending counts per day, newest day first. */
    public static List<DaySummary> summarize(List<DoseRecord> doses, LocalDateTime now, int graceMinutes) {
        Map<LocalDate, int[]> byDay = new TreeMap<>(Comparator.reverseOrder());
        for (DoseRecord d : doses) {
            int[] counts = byDay.computeIfAbsent(d.scheduledDate(), k -> new int[3]);
            DisplayStatus s = DoseStatusCalculator.effective(d, now, graceMinutes);
            if (s == DisplayStatus.TAKEN) counts[0]++;
            else if (s == DisplayStatus.MISSED) counts[1]++;
            else counts[2]++;
        }
        List<DaySummary> result = new ArrayList<>();
        byDay.forEach((day, c) -> result.add(new DaySummary(day, c[0], c[1], c[2])));
        return result;
    }
}
