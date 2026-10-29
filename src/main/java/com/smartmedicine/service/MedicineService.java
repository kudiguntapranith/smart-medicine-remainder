package com.smartmedicine.service;

import com.smartmedicine.dao.HistoryDao;
import com.smartmedicine.dao.MedicineDao;
import com.smartmedicine.database.DatabaseManager;
import com.smartmedicine.model.Frequency;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.MedicineSchedule;
import com.smartmedicine.util.DateTimeUtil;
import com.smartmedicine.util.ValidationException;
import com.smartmedicine.util.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Medicine CRUD. "Delete" is a soft delete (deactivate) so the intake history is always kept. */
public class MedicineService {
    private final MedicineDao medicineDao = new MedicineDao();
    private final HistoryDao historyDao = new HistoryDao();
    private final DoseService doseService;
    private final Runnable afterChange;

    /** @param afterChange called after every committed change so the reminder queue can be rebuilt */
    public MedicineService(DoseService doseService, Runnable afterChange) {
        this.doseService = doseService;
        this.afterChange = afterChange;
    }

    /** Validates the form and returns the cleaned, sorted, de-duplicated reminder times. */
    public static List<LocalTime> validate(String name, String dosage, Frequency frequency, LocalDate start,
                                           LocalDate end, String notes, List<LocalTime> times) {
        Validator.requireText(name, "Medicine name", 100);
        Validator.requireText(dosage, "Dosage", 50);
        if (frequency == null) throw new ValidationException("Please select a frequency.");
        if (start == null) throw new ValidationException("Start date is required.");
        if (end != null && end.isBefore(start)) throw new ValidationException("End date must not be before the start date.");
        if (notes != null && notes.length() > 500) throw new ValidationException("Notes must be at most 500 characters.");
        if (times == null || times.isEmpty()) throw new ValidationException("Add at least one reminder time.");
        List<LocalTime> clean = List.copyOf(new TreeSet<>(times));
        if (clean.size() != times.size()) throw new ValidationException("Reminder times must be different from each other.");
        if (frequency.expectedTimes() > 0 && clean.size() != frequency.expectedTimes())
            throw new ValidationException(frequency + " needs exactly " + frequency.expectedTimes()
                    + " reminder time(s); you entered " + clean.size() + ". Choose 'Custom' for a different number.");
        return clean;
    }

    public long add(long userId, String name, String dosage, Frequency frequency, LocalDate start, LocalDate end,
                    String notes, List<LocalTime> times) {
        List<LocalTime> t = validate(name, dosage, frequency, start, end, notes, times);
        long id = DatabaseManager.inTransaction(c -> {
            long medicineId = medicineDao.insert(c, new Medicine(0, userId, name.trim(), dosage.trim(), frequency,
                    start, end, clean(notes), true, t));
            LocalDateTime now = DateTimeUtil.nowMinutes();
            for (LocalTime lt : t) medicineDao.insertSchedule(c, medicineId, lt, now);
            doseService.generateForDate(c, userId, LocalDate.now());
            return medicineId;
        });
        afterChange.run();
        return id;
    }

    public void update(long userId, long medicineId, String name, String dosage, Frequency frequency, LocalDate start,
                       LocalDate end, String notes, List<LocalTime> times) {
        List<LocalTime> t = validate(name, dosage, frequency, start, end, notes, times);
        DatabaseManager.inTransaction(c -> {
            Medicine existing = medicineDao.findById(c, userId, medicineId)
                    .orElseThrow(() -> new ValidationException("Medicine not found."));
            medicineDao.update(c, new Medicine(medicineId, userId, name.trim(), dosage.trim(), frequency, start, end,
                    clean(notes), existing.active(), t));
            LocalDateTime now = DateTimeUtil.nowMinutes();
            Map<LocalTime, MedicineSchedule> current = new HashMap<>();
            for (MedicineSchedule s : medicineDao.findSchedules(c, medicineId)) current.put(s.reminderTime(), s);
            for (MedicineSchedule s : current.values()) {
                if (s.active() && !t.contains(s.reminderTime())) medicineDao.deactivateSchedule(c, s.id());
            }
            for (LocalTime lt : t) {
                MedicineSchedule s = current.get(lt);
                if (s == null) medicineDao.insertSchedule(c, medicineId, lt, now);
                else if (!s.active()) medicineDao.reactivateSchedule(c, s.id(), now);
            }
            // Upcoming (not yet due) pending doses are rebuilt from the new schedule; past doses stay untouched.
            historyDao.deletePendingFuture(c, userId, medicineId, LocalDateTime.now());
            doseService.generateForDate(c, userId, LocalDate.now());
            return null;
        });
        afterChange.run();
    }

    /** Stops reminders and removes upcoming pending doses. Past history is kept. */
    public void deactivate(long userId, long medicineId) {
        DatabaseManager.inTransaction(c -> {
            if (!medicineDao.setActive(c, userId, medicineId, false)) throw new ValidationException("Medicine not found.");
            historyDao.deletePendingFuture(c, userId, medicineId, LocalDateTime.now());
            return null;
        });
        afterChange.run();
    }

    public void reactivate(long userId, long medicineId) {
        DatabaseManager.inTransaction(c -> {
            if (!medicineDao.setActive(c, userId, medicineId, true)) throw new ValidationException("Medicine not found.");
            medicineDao.resetScheduleBoundary(c, medicineId, DateTimeUtil.nowMinutes());
            doseService.generateForDate(c, userId, LocalDate.now());
            return null;
        });
        afterChange.run();
    }

    public List<Medicine> list(long userId) {
        return DatabaseManager.withConnection(c -> medicineDao.findAllByUser(c, userId));
    }

    private static String clean(String notes) {
        return (notes == null || notes.isBlank()) ? null : notes.trim();
    }
}
