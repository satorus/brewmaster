-- V2: Seed data (development only)
-- Admin user: password is 'admin1234' (bcrypt strength 12)
INSERT INTO users (id, username, email, password, display_name, role)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@brewmaster.local',
    '$2a$12$bHyDJFSEt9jtqMbdI5l.4.j1H3gZJkBHQxf0ZAdB4yVYcVPWJA5Ay',
    'BrewMaster Admin',
    'ADMIN'
) ON CONFLICT DO NOTHING;
