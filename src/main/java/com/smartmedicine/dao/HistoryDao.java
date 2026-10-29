package com.smartmedicine.dao;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.MedicineSchedule;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Individual dose records (medicine_history). Every query is filtered by user_id. */
public class HistoryDao {
    private static final String SELECT =
            "SELECT id, user_id, medicine_id, schedule_id, medicine_name, dosage, scheduled_date, scheduled_time, "
                    + "status, taken_at FROM medicine_history ";

    /** Inserts the dose unless (schedule, date) already exists. Returns 1 if inserted, 0 if it already existed. */
    public int insertIfAbsent(Connection c, long userId, Medicine m, MedicineSchedule s, LocalDate date) throws SQLException {
        String sql = "INSERT INTO medicine_history (user_id, medicine_id, schedule_id, medicine_name, dosage, "
                + "scheduled_date, scheduled_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING') "
                + "ON DUPLICATE KEY UPDATE id = id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, m.id());
            ps.setLong(3, s.id());
            ps.setString(4, m.name());
            ps.setString(5, m.dosage());
            ps.setObject(6, date);
            ps.setObject(7, s.reminderTime());
            return ps.executeUpdate() == 1 ? 1 : 0;
        }
    }

    public Optional<DoseRecord> findById(Connection c, long userId, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id=? AND user_id=?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<DoseRecord> findByDate(Connection c, long userId, LocalDate date) throws SQLException {
        List<DoseRecord> list = findRange(c, userId, date, date, null, null);
        list.sort(Comparator.comparing(DoseRecord::scheduledTime).thenComparingLong(DoseRecord::id));
        return list;
    }

    /** Newest first. medicineId and status are optional filters (null = all). */
    public List<DoseRecord> findRange(Connection c, long userId, LocalDate from, LocalDate to,
                                      Long medicineId, DoseStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + "WHERE user_id=? AND scheduled_date BETWEEN ? AND ?");
        if (medicineId != null) sql.append(" AND medicine_id=?");
        if (status != null) sql.append(" AND status=?");
        sql.append(" ORDER BY scheduled_date DESC, scheduled_time DESC, id DESC");
        List<DoseRecord> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setLong(i++, userId);
            ps.setObject(i++, from);
            ps.setObject(i++, to);
            if (medicineId != null) ps.setLong(i++, medicineId);
            if (status != null) ps.setString(i, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Pending doses of ACTIVE medicines on a date; used to build the reminder queue. */
    public List<DoseRecord> findPendingActiveByDate(Connection c, long userId, LocalDate date) throws SQLException {
        String sql = "SELECT h.id, h.user_id, h.medicine_id, h.schedule_id, h.medicine_name, h.dosage, h.scheduled_date, "
                + "h.scheduled_time, h.status, h.taken_at FROM medicine_history h "
                + "JOIN medicines m ON m.id = h.medicine_id "
                + "WHERE h.user_id=? AND h.scheduled_date=? AND h.status='PENDING' AND m.active=1 "
                + "ORDER BY h.scheduled_time";
        List<DoseRecord> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean updateStatus(Connection c, long userId, long doseId, DoseStatus status, LocalDateTime takenAt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE medicine_history SET status=?, taken_at=? WHERE id=? AND user_id=?")) {
            ps.setString(1, status.name());
            ps.setObject(2, takenAt);
            ps.setLong(3, doseId);
            ps.setLong(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** PENDING doses scheduled before {@code cutoff} (= now - grace period) become MISSED. Future doses are untouched. */
    public int markOverdueMissed(Connection c, long userId, LocalDateTime cutoff) throws SQLException {
        String sql = "UPDATE medicine_history SET status='MISSED' WHERE user_id=? AND status='PENDING' "
                + "AND TIMESTAMP(scheduled_date, scheduled_time) < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, cutoff);
            return ps.executeUpdate();
        }
    }

    /** Removes not-yet-due pending doses of a medicine (used when it is edited or deactivated). */
    public int deletePendingFuture(Connection c, long userId, long medicineId, LocalDateTime now) throws SQLException {
        String sql = "DELETE FROM medicine_history WHERE user_id=? AND medicine_id=? AND status='PENDING' "
                + "AND TIMESTAMP(scheduled_date, scheduled_time) > ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, medicineId);
            ps.setObject(3, now);
            return ps.executeUpdate();
        }
    }

    /** Retention policy: delete records scheduled before {@code oldestKept}. */
    public int purgeOlderThan(Connection c, LocalDate oldestKept) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM medicine_history WHERE scheduled_date < ?")) {
            ps.setObject(1, oldestKept);
            return ps.executeUpdate();
        }
    }

    private static DoseRecord map(ResultSet rs) throws SQLException {
        long sid = rs.getLong("schedule_id");
        Long scheduleId = rs.wasNull() ? null : sid;
        return new DoseRecord(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("medicine_id"), scheduleId,
                rs.getString("medicine_name"), rs.getString("dosage"), rs.getObject("scheduled_date", LocalDate.class),
                rs.getObject("scheduled_time", LocalTime.class), DoseStatus.valueOf(rs.getString("status")),
                rs.getObject("taken_at", LocalDateTime.class));
    }
}
