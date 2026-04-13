INSERT INTO users (id, name, email, password)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Ankit Kumar', 'ankit@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiMujinGZ8SGvtQvV4H14R/2uOeGjnK'),
    ('22222222-2222-2222-2222-222222222222', 'Priya Sharma', 'priya@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiMujinGZ8SGvtQvV4H14R/2uOeGjnK');

INSERT INTO projects (id, name, description, owner_id)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'TaskFlow Launch', 'Seeded project for smoke testing and demos.', '11111111-1111-1111-1111-111111111111');

INSERT INTO project_members (project_id, user_id, role)
VALUES
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'EDITOR');

INSERT INTO tasks (id, title, description, status, priority, project_id, assignee_id, creator_id, due_date, version)
VALUES
    ('44444444-4444-4444-4444-444444444444', 'Draft API contract', 'Finalize response contracts for the first sprint.', 'TODO', 'HIGH', '33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', CURRENT_DATE + INTERVAL '3 day', 0),
    ('55555555-5555-5555-5555-555555555555', 'Wire Redis cache', 'Add cache-aside reads for project details.', 'IN_PROGRESS', 'CRITICAL', '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', CURRENT_DATE + INTERVAL '5 day', 0),
    ('66666666-6666-6666-6666-666666666666', 'Publish kickoff notes', 'Share architecture notes with the team.', 'DONE', 'MEDIUM', '33333333-3333-3333-3333-333333333333', NULL, '11111111-1111-1111-1111-111111111111', CURRENT_DATE + INTERVAL '1 day', 0);

