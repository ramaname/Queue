-- Таблица пользователей
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица предметов
CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    total_labs INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица сессий (сдача конкретной лабы)
CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    lab_number INT NOT NULL,
    deadline_date DATE NOT NULL,
    unlock_at TIMESTAMP NOT NULL,
    window_minutes INT NOT NULL DEFAULT 10,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    current_booking_id BIGINT NULL,
    current_turn_started_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT fk_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_sessions_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица броней (кто занял очередь)
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    wanted_count INT NOT NULL,
    passes_done INT NOT NULL DEFAULT 0,
    skipped_at TIMESTAMP NULL,
    version INT NOT NULL DEFAULT 0, -- для оптимистичной блокировки
    CONSTRAINT fk_bookings_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uq_session_user (session_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Добавляем внешний ключ на текущую бронь в сессии (циклическая зависимость, поэтому ALTER)
ALTER TABLE sessions
ADD CONSTRAINT fk_sessions_current_booking
FOREIGN KEY (current_booking_id) REFERENCES bookings(id) ON DELETE SET NULL;

-- Таблица "кого ждём" (снапшот пользователей при открытии окна)
CREATE TABLE session_expected_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_expected_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_expected_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uq_session_expected_user (session_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица истории сдач лаб
CREATE TABLE lab_passes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    lab_number INT NOT NULL,
    passed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NULL,
    CONSTRAINT fk_passes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_passes_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_passes_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE SET NULL,
    UNIQUE KEY uq_user_subject_lab (user_id, subject_id, lab_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица аудита (лог событий сессии)
CREATE TABLE session_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    user_id BIGINT NULL,
    payload JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;