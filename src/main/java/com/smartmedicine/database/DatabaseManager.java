package com.smartmedicine.database;

import com.smartmedicine.util.Config;
import com.smartmedicine.util.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens JDBC connections and runs units of work (optionally inside a transaction). */
public final class DatabaseManager {

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    private DatabaseManager() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Config.dbUrl(), Config.dbUser(), Config.dbPassword());
    }

    public static <R> R withConnection(SqlFunction<Connection, R> work) {
        try (Connection c = getConnection()) {
            return work.apply(c);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Commits if the work succeeds, rolls back on any failure. */
    public static <R> R inTransaction(SqlFunction<Connection, R> work) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                R result = work.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private static DataAccessException translate(SQLException e) {
        return new DataAccessException("Database error: " + e.getMessage()
                + " (check that MySQL is running and your configuration is correct)", e);
    }
}
