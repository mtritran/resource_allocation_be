CREATE TABLE skill (
    skill_id    BIGSERIAL PRIMARY KEY,
    skill_name  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE employee_skill (
    employee_id BIGINT NOT NULL REFERENCES employee(employee_id) ON DELETE CASCADE,
    skill_id    BIGINT NOT NULL REFERENCES skill(skill_id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, skill_id)
);

ALTER TABLE allocation ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Clear old data
DELETE FROM employee_skill;
DELETE FROM allocation;
DELETE FROM skill;
DELETE FROM employee;
DELETE FROM project;

-- Seed Employees
INSERT INTO employee (employee_code, full_name, email, role, department) VALUES
('EMP001', 'Nguyen Van A', 'a@company.com', 'Java Developer', 'Delivery'),
('EMP002', 'Tran Thi B', 'b@company.com', 'Java Developer', 'Delivery'),
('EMP003', 'Le Van C', 'c@company.com', 'React Developer', 'Delivery'),
('EMP004', 'Pham Van D', 'd@company.com', 'DevOps Engineer', 'Cloud');

-- Seed Projects
INSERT INTO project (project_code, project_name, customer, start_date, end_date, status) VALUES
('PRJ001', 'E-Commerce Platform', 'Retail Corp', '2025-01-01', '2025-12-31', 'ACTIVE'),
('PRJ002', 'Internal Dashboard', 'Internal', '2025-02-01', '2025-06-30', 'PLANNING'),
('PRJ003', 'Legacy Upgrade', 'Finance Inc', '2024-01-01', '2024-12-31', 'COMPLETED');

-- Seed Skills
INSERT INTO skill (skill_name) VALUES
('Java'),
('Spring Boot'),
('React'),
('TypeScript'),
('Docker'),
('PostgreSQL');

-- Seed Employee Skills
INSERT INTO employee_skill (employee_id, skill_id) VALUES
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'), (SELECT skill_id FROM skill WHERE skill_name = 'Java')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'), (SELECT skill_id FROM skill WHERE skill_name = 'Spring Boot')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'), (SELECT skill_id FROM skill WHERE skill_name = 'PostgreSQL')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP002'), (SELECT skill_id FROM skill WHERE skill_name = 'Java')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP002'), (SELECT skill_id FROM skill WHERE skill_name = 'Docker')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP003'), (SELECT skill_id FROM skill WHERE skill_name = 'React')),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP003'), (SELECT skill_id FROM skill WHERE skill_name = 'TypeScript'));

-- Seed Allocations
INSERT INTO allocation (employee_id, project_id, allocation_percent, role_in_project, start_date, end_date, status) VALUES
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 60, 'Backend Dev', '2025-01-01', '2025-12-31', 'ACTIVE'),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ002'), 40, 'Backend Lead', '2025-02-01', '2025-06-30', 'PENDING'),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP002'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 80, 'Fullstack Dev', '2025-01-01', '2025-12-31', 'ACTIVE'),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP003'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 40, 'Frontend Dev', '2025-01-01', '2025-12-31', 'ENDED');
