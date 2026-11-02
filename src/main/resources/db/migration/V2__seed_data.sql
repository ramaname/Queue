-- Добавляем 5 предметов
INSERT INTO subjects (code, name, total_labs) VALUES
('STSR', 'СТСР', 7),
('SITAIRIS', 'СиТАиРИС', 6),
('VYAP', 'ВЯП', 8),
('IPPRPO', 'ИППРПО', 6),
('EVT', 'ЭВТ', 8);

-- Добавляем аккаунт администратора.
-- Логин (student_id): ADMIN
-- Пароль: admin123 (захэширован через BCrypt)
INSERT INTO users (full_name, student_id, password_hash, role)
VALUES ('Главный Администратор', 'ADMIN', '$2a$10$wT09v.8wXWq6y/5qXWq6y.Wq6yWq6yWq6yWq6yWq6yWq6yWq6y', 'ADMIN');