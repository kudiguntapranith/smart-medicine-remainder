package com.smartmedicine.dao;

import com.smartmedicine.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {
    public record StoredUser(User user, String passwordHash) { }

    public Optional<StoredUser> findByUsername(Connection c, String username) throws SQLException {
        String sql = "SELECT id, username, full_name, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new StoredUser(
                        new User(rs.getLong("id"), rs.getString("username"), rs.getString("full_name")),
                        rs.getString("password_hash")));
            }
        }
    }

    public long insert(Connection c, String username, String fullName, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, full_name, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}
