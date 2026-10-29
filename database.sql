-- Smart Medicine Reminder - MySQL 8.x schema
CREATE DATABASE IF NOT EXISTS smart_medicine_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_medicine_db;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(30)  NOT NULL,
  full_name     VARCHAR(80)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,          -- PBKDF2 hash, never plain text
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicines (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT       NOT NULL,
  name       VARCHAR(100) NOT NULL,
  dosage     VARCHAR(50)  NOT NULL,
  frequency  ENUM('ONCE_DAILY','TWICE_DAILY','THREE_TIMES_DAILY','CUSTOM') NOT NULL,
  start_date DATE         NOT NULL,
  end_date   DATE         NULL,                 -- NULL = ongoing
  notes      VARCHAR(500) NULL,
  active     TINYINT(1)   NOT NULL DEFAULT 1,   -- "delete" in the app = active 0 (history is kept)
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_medicines_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_medicine_dates CHECK (end_date IS NULL OR end_date >= start_date),
  INDEX idx_medicines_user_active (user_id, active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicine_schedules (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  medicine_id   BIGINT     NOT NULL,
  reminder_time TIME       NOT NULL,
  active        TINYINT(1) NOT NULL DEFAULT 1,
  created_at    DATETIME   NOT NULL,            -- doses before this moment are never generated
  CONSTRAINT fk_schedules_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  CONSTRAINT uq_schedule_time UNIQUE (medicine_id, reminder_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicine_history (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT       NOT NULL,
  medicine_id    BIGINT       NOT NULL,
  schedule_id    BIGINT       NULL,
  medicine_name  VARCHAR(100) NOT NULL,         -- snapshot, so history survives edits
  dosage         VARCHAR(50)  NOT NULL,
  scheduled_date DATE         NOT NULL,
  scheduled_time TIME         NOT NULL,
  status         ENUM('PENDING','TAKEN','MISSED') NOT NULL DEFAULT 'PENDING',
  taken_at       DATETIME     NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_history_user     FOREIGN KEY (user_id)     REFERENCES users(id)              ON DELETE CASCADE,
  CONSTRAINT fk_history_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id)          ON DELETE RESTRICT,
  CONSTRAINT fk_history_schedule FOREIGN KEY (schedule_id) REFERENCES medicine_schedules(id) ON DELETE SET NULL,
  CONSTRAINT uq_dose UNIQUE (schedule_id, scheduled_date),   -- one record per schedule per day: no duplicates
  INDEX idx_history_user_date (user_id, scheduled_date),
  INDEX idx_history_user_status (user_id, status, scheduled_date)
) ENGINE=InnoDB;
