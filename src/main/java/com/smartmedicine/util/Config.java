package com.smartmedicine.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Central configuration. Order: environment variable, then config.properties, then default. */
public final class Config {
    /** A PENDING dose becomes MISSED once this many minutes have passed after its scheduled time. */
    public static final int GRACE_MINUTES = 60;
    /** History window (days, including today). Older records are purged. */
    public static final int HISTORY_DAYS = 30;

    private static final Properties FILE = load();

    private Config() { }

    private static Properties load() {
        Properties p = new Properties();
        Path f = Path.of("config.properties");
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read config.properties: " + e.getMessage());
            }
        }
        return p;
    }

    private static String get(String env, String prop, String def) {
        String v = System.getenv(env);
        if (v != null && !v.isBlank()) return v;
        v = FILE.getProperty(prop);
        return (v != null && !v.isBlank()) ? v : def;
    }

    public static String dbUrl() {
        return get("SMARTMED_DB_URL", "db.url",
                "jdbc:mysql://localhost:3306/smart_medicine_db?useSSL=false&allowPublicKeyRetrieval=true");
    }
    public static String dbUser() { return get("SMARTMED_DB_USER", "db.user", "root"); }
    public static String dbPassword() { return get("SMARTMED_DB_PASSWORD", "db.password", ""); }
}
