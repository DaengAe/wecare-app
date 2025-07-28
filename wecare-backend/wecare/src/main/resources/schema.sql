-- Test용 DB 및 User 생성 쿼리
-- CREATE DATABASE IF NOT EXISTS wecare_test;
-- CREATE USER IF NOT EXISTS 'wecare_test'@'%' IDENTIFIED BY 'wecareTest';
-- GRANT ALL PRIVILEGES ON wecare_test.* TO 'wecare_test'@'%';
-- USE wecare_test;

-- 1. 사용자 테이블
CREATE TABLE IF NOT EXISTS members (
                                       id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                       birth_date DATE NOT NULL,
                                       created_at DATETIME(6),
    gender     ENUM('FEMALE', 'MALE') NOT NULL,
    name       VARCHAR(50) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    phone      VARCHAR(13) NOT NULL,
    role       ENUM('DEPENDENT', 'GUARDIAN') NOT NULL,
    updated_at DATETIME(6),
    username   VARCHAR(50) NOT NULL UNIQUE
    );

-- 2. 보호자-피보호자 연결 테이블
CREATE TABLE IF NOT EXISTS invitations (
                                           dependent_id BIGINT NOT NULL,
                                           guardian_id  BIGINT NOT NULL,
                                           created_at   DATETIME(6),
    is_active    TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (dependent_id, guardian_id),
    FOREIGN KEY (dependent_id) REFERENCES members (id) ON DELETE CASCADE,
    FOREIGN KEY (guardian_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 3. 루틴 테이블
CREATE TABLE IF NOT EXISTS routine (
                                       id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                       completed    TINYINT(1) NOT NULL,
    created_at   DATETIME(6),
    description  VARCHAR(1000),
    end_time     DATETIME(6),
    is_repeat    TINYINT(1) NOT NULL,
    start_time   DATETIME(6) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    type         ENUM('ACTIVITY', 'CUSTOM', 'MEDICATION', 'SUPPLEMENT') NOT NULL,
    updated_at   DATETIME(6),
    dependent_id BIGINT NOT NULL,
    guardian_id  BIGINT NOT NULL,
    FOREIGN KEY (dependent_id) REFERENCES members (id) ON DELETE CASCADE,
    FOREIGN KEY (guardian_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 4. 루틴 알람 설정 테이블
CREATE TABLE IF NOT EXISTS routine_alarm_setting (
                                                     id                     BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                                     alert_before_end_min   INT,
                                                     alert_before_start_min INT,
                                                     repeat_interval_min    INT,
                                                     routine_id             BIGINT UNIQUE,
                                                     FOREIGN KEY (routine_id) REFERENCES routine (id) ON DELETE CASCADE
    );

-- 5. 루틴 반복 요일 테이블
CREATE TABLE IF NOT EXISTS routine_repeat_days (
                                                   routine_id BIGINT NOT NULL,
                                                   day        ENUM('DAILY','MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
    PRIMARY KEY (routine_id, day),
    FOREIGN KEY (routine_id) REFERENCES routine (id) ON DELETE CASCADE
    );

-- 6. 루틴 체크 테이블
CREATE TABLE IF NOT EXISTS routine_checks (
                                              id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              routine_id BIGINT NOT NULL,
                                              member_id  BIGINT NOT NULL,
                                              checked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_routine_checks_routine FOREIGN KEY (routine_id) REFERENCES routine (id) ON DELETE CASCADE,
    CONSTRAINT fk_routine_checks_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 7. 일정 테이블
CREATE TABLE IF NOT EXISTS schedules (
                                         id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         member_id        BIGINT NOT NULL,
                                         title            VARCHAR(100) NOT NULL,
    description      TEXT,
    date             DATETIME NOT NULL,
    time             TIME,
    is_shared        BOOLEAN DEFAULT TRUE,
    is_alert_enabled BOOLEAN DEFAULT FALSE,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 8. 건강 리포트 테이블
CREATE TABLE IF NOT EXISTS health_reports (
                                              id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              member_id           BIGINT NOT NULL,
                                              report_date         DATETIME NOT NULL,
                                              health_score_daily  INT,
                                              health_score_weekly INT,
                                              created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_health_reports_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 9. 처방전 테이블
CREATE TABLE IF NOT EXISTS prescriptions (
                                             id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             member_id  BIGINT NOT NULL,
                                             type       ENUM('MEDICATION', 'SUPPLEMENT') NOT NULL,
    name       VARCHAR(100) NOT NULL,
    method     TEXT,
    start_date DATETIME,
    end_date   DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptions_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 10. 만보계 테이블
CREATE TABLE IF NOT EXISTS pedometers (
                                          id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          member_id  BIGINT NOT NULL,
                                          date       DATETIME NOT NULL,
                                          step_count INT DEFAULT 0,
                                          distance   FLOAT DEFAULT 0,
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_pedometers_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );

-- 11. 알림 테이블
CREATE TABLE IF NOT EXISTS alerts (
                                      id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      member_id   BIGINT NOT NULL,
                                      alert_type  ENUM('ROUTINE', 'SCHEDULE', 'CUSTOM') NOT NULL,
    title       VARCHAR(100),
    content     TEXT,
    alert_time  DATETIME NOT NULL,
    repeat_flag BOOLEAN DEFAULT FALSE,
    sound_type  ENUM('DEFAULT', 'CUSTOM') DEFAULT 'DEFAULT',
    voice_path  VARCHAR(255),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
    );
