-- ============================================================
-- V1__init_schema.sql
-- Migration cơ bản: tạo toàn bộ schema theo database-schema.md
-- ============================================================

CREATE TABLE employee (
    employee_id     BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(20)  NOT NULL UNIQUE,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    role            VARCHAR(50)  NOT NULL,
    department      VARCHAR(50),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE project (
    project_id      BIGSERIAL PRIMARY KEY,
    project_code    VARCHAR(20)  NOT NULL UNIQUE,
    project_name    VARCHAR(200) NOT NULL,
    customer        VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_status CHECK (status IN ('PLANNING', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_project_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE allocation (
    allocation_id       BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT      NOT NULL REFERENCES employee(employee_id),
    project_id          BIGINT      NOT NULL REFERENCES project(project_id),
    allocation_percent  INTEGER     NOT NULL,
    role_in_project     VARCHAR(100),
    start_date          DATE,
    end_date            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_allocation_percent CHECK (allocation_percent > 0 AND allocation_percent <= 100),
    CONSTRAINT chk_allocation_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_allocation_status CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED'))
);

CREATE TABLE skill (
    skill_id    BIGSERIAL PRIMARY KEY,
    skill_name  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE employee_skill (
    employee_id BIGINT NOT NULL REFERENCES employee(employee_id) ON DELETE CASCADE,
    skill_id    BIGINT NOT NULL REFERENCES skill(skill_id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, skill_id)
);

CREATE UNIQUE INDEX idx_employee_code ON employee(employee_code);
CREATE UNIQUE INDEX idx_employee_email ON employee(email);
CREATE UNIQUE INDEX idx_project_code ON project(project_code);
CREATE INDEX idx_project_status ON project(status);
CREATE INDEX idx_allocation_employee ON allocation(employee_id);
CREATE INDEX idx_allocation_project ON allocation(project_id);
CREATE INDEX idx_allocation_employee_project ON allocation(employee_id, project_id);
CREATE UNIQUE INDEX idx_skill_name ON skill(skill_name);
CREATE INDEX idx_employee_skill_employee ON employee_skill(employee_id);
CREATE INDEX idx_employee_skill_skill ON employee_skill(skill_id);
