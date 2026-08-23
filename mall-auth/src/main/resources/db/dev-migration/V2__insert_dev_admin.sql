INSERT INTO ums_member (username, password, role, status)
SELECT 'dev-admin',
       '$2y$10$WrClHbZgrPty9l7KeChhmeZN5Am93VnRsbrVVcdcHQjMQQvho5IEG',
       'ADMIN',
       1
    WHERE NOT EXISTS (
    SELECT 1
    FROM ums_member
    WHERE username = 'dev-admin'
);