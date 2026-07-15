-- ============================================================
-- V2__seed_data.sql
-- Seed data cho employee, project, allocation để chạy demo và test
-- ============================================================

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

-- Seed Allocations
-- Nguyen Van A (EMP001): 60% on PRJ001, 40% on PRJ002 -> 100% total (0% available)
INSERT INTO allocation (employee_id, project_id, allocation_percent, role_in_project, start_date, end_date) VALUES
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 60, 'Backend Dev', '2025-01-01', '2025-12-31'),
((SELECT employee_id FROM employee WHERE employee_code = 'EMP001'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ002'), 40, 'Backend Lead', '2025-02-01', '2025-06-30');

-- Tran Thi B (EMP002): 80% on PRJ001 -> 80% total (20% available)
INSERT INTO allocation (employee_id, project_id, allocation_percent, role_in_project, start_date, end_date) VALUES
((SELECT employee_id FROM employee WHERE employee_code = 'EMP002'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 80, 'Fullstack Dev', '2025-01-01', '2025-12-31');

-- Le Van C (EMP003): 40% on PRJ001 -> 40% total (60% available)
INSERT INTO allocation (employee_id, project_id, allocation_percent, role_in_project, start_date, end_date) VALUES
((SELECT employee_id FROM employee WHERE employee_code = 'EMP003'),
 (SELECT project_id FROM project WHERE project_code = 'PRJ001'), 40, 'Frontend Dev', '2025-01-01', '2025-12-31');
