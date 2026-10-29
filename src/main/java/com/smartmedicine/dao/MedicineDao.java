package com.smartmedicine.dao;

import com.smartmedicine.model.Frequency;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.MedicineSchedule;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CRUD for medicines and their schedules. Every medicine query is filtered by user_id. */
public class MedicineDao {
    private static final String SELECT_MED =
            "SELECT id, user_id, name, dosage, frequency, start_date, end_date, notes, active FROM medicines ";

    public long insert(Connection c, Medicine m) throws SQLException {
        String sql = "INSERT INTO medicines (user_id, name, dosage, frequency, start_date, end_date, notes, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.userId());
            ps.setString(2, m.name());
            ps.setString(3, m.dosage());
            ps.setString(4, m.frequency().name());
            ps.setObject(5, m.startDate());
            setDate(ps, 6, m.endDate());
            ps.setString(7, m.notes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public boolean update(Connection c, Medicine m) throws SQLException {
        String sql = "UPDATE medicines SET name=?, dosage=?, frequency=?, start_date=?, end_date=?, notes=? "
                + "WHERE id=? AND user_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.name());
            ps.setString(2, m.dosage());
            ps.setString(3, m.frequency().name());
            ps.setObject(4, m.startDate());
            setDate(ps, 5, m.endDate());
            ps.setString(6, m.notes());
            ps.setLong(7, m.id());
            ps.setLong(8, m.userId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setActive(Connection c, long userId, long medicineId, boolean active) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE medicines SET active=? WHERE id=? AND user_id=?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, medicineId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Medicine> findById(Connection c, long userId, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_MED + "WHERE id=? AND user_id=?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs, activeTimes(c, id)));
            }
        }
    }

    public List<Medicine> findAllByUser(Connection c, long userId) throws SQLException {
        Map<Long, List<LocalTime>> timesByMedicine = new HashMap<>();
        String timesSql = "SELECT s.medicine_id, s.reminder_time FROM medicine_schedules s "
                + "JOIN medicines m ON m.id = s.medicine_id WHERE m.user_id=? AND s.active=1 ORDER BY s.reminder_time";
        try (PreparedStatement ps = c.prepareStatement(timesSql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    timesByMedicine.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                            .add(rs.getObject(2, LocalTime.class));
                }
            }
        }
        List<Medicine> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_MED + "WHERE user_id=? ORDER BY active DESC, name")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs, timesByMedicine.getOrDefault(rs.getLong("id"), List.of())));
                }
            }
        }
        return result;
    }

    // ---------- schedules ----------

    public long insertSchedule(Connection c, long medicineId, LocalTime time, LocalDateTime createdAt) throws SQLException {
        String sql = "INSERT INTO medicine_schedules (medicine_id, reminder_time, active, created_at) VALUES (?, ?, 1, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, medicineId);
            ps.setObject(2, time);
            ps.setObject(3, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** All schedules of a medicine, including deactivated ones (their history rows still reference them). */
    public List<MedicineSchedule> findSchedules(Connection c, long medicineId) throws SQLException {
        String sql = "SELECT id, medicine_id, reminder_time, active, created_at FROM medicine_schedules "
                + "WHERE medicine_id=? ORDER BY reminder_time";
        List<MedicineSchedule> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MedicineSchedule(rs.getLong("id"), rs.getLong("medicine_id"),
                            rs.getObject("reminder_time", LocalTime.class), rs.getBoolean("active"),
                            rs.getObject("created_at", LocalDateTime.class)));
                }
            }
        }
        return list;
    }

    public void deactivateSchedule(Connection c, long scheduleId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE medicine_schedules SET active=0 WHERE id=?")) {
            ps.setLong(1, scheduleId);
            ps.executeUpdate();
        }
    }

    /** Re-enables a schedule and restarts its "no doses before" boundary at {@code now}. */
    public void reactivateSchedule(Connection c, long scheduleId, LocalDateTime now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE medicine_schedules SET active=1, created_at=? WHERE id=?")) {
            ps.setObject(1, now);
            ps.setLong(2, scheduleId);
            ps.executeUpdate();
        }
    }

    public void resetScheduleBoundary(Connection c, long medicineId, LocalDateTime now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE medicine_schedules SET created_at=? WHERE medicine_id=?")) {
            ps.setObject(1, now);
            ps.setLong(2, medicineId);
            ps.executeUpdate();
        }
    }

    // ---------- helpers ----------

    private List<LocalTime> activeTimes(Connection c, long medicineId) throws SQLException {
        List<LocalTime> times = new ArrayList<>();
        String sql = "SELECT reminder_time FROM medicine_schedules WHERE medicine_id=? AND active=1 ORDER BY reminder_time";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) times.add(rs.getObject(1, LocalTime.class));
            }
        }
        return times;
    }

    private static Medicine map(ResultSet rs, List<LocalTime> times) throws SQLException {
        return new Medicine(rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"), rs.getString("dosage"),
                Frequency.valueOf(rs.getString("frequency")), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), rs.getString("notes"), rs.getBoolean("active"),
                List.copyOf(times));
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(index, Types.DATE); else ps.setObject(index, d);
    }
}
