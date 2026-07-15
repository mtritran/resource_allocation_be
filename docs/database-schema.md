# Database Schema

## 1. ERD tổng quan

```
employee (1) ────< allocation >──── (1) project
```

- Một `employee` có thể có nhiều `allocation`.
- Một `project` có thể có nhiều `allocation`.
- `allocation` là bảng trung gian (many-to-many giữa employee và project), có thêm dữ liệu riêng (allocation_percent, role_in_project, start_date, end_date).

---

## 2. Bảng `employee`

```sql
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
```

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| employee_id | BIGSERIAL | PK | Tự tăng |
| employee_code | VARCHAR(20) | UNIQUE, NOT NULL | Mã nhân viên, business key (VD: EMP001) |
| full_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Validate `@Email` ở tầng DTO |
| role | VARCHAR(50) | NOT NULL | VD: Senior Developer |
| department | VARCHAR(50) | nullable | VD: FSOFT-Q1 |
| created_at / updated_at | TIMESTAMP | NOT NULL | Audit field, dùng `@CreationTimestamp` / `@UpdateTimestamp` |

**Index:**
```sql
CREATE UNIQUE INDEX idx_employee_code ON employee(employee_code);
CREATE UNIQUE INDEX idx_employee_email ON employee(email);
```

---

## 3. Bảng `project`

```sql
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
```

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| project_id | BIGSERIAL | PK | |
| project_code | VARCHAR(20) | UNIQUE, NOT NULL | VD: NCG, GRID |
| project_name | VARCHAR(200) | NOT NULL | |
| customer | VARCHAR(100) | nullable | |
| start_date / end_date | DATE | nullable | end_date phải >= start_date nếu có |
| status | VARCHAR(20) | NOT NULL, CHECK | PLANNING / ACTIVE / COMPLETED |

**Index:**
```sql
CREATE UNIQUE INDEX idx_project_code ON project(project_code);
CREATE INDEX idx_project_status ON project(status);
```

> Note: dùng `VARCHAR + CHECK constraint` thay vì Postgres ENUM để đơn giản hoá migration sau này (thêm status mới không cần `ALTER TYPE`). Ở tầng Java map bằng `enum` + `@Enumerated(EnumType.STRING)`.

---

## 4. Bảng `allocation`

```sql
CREATE TABLE allocation (
    allocation_id       BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT      NOT NULL REFERENCES employee(employee_id),
    project_id          BIGINT      NOT NULL REFERENCES project(project_id),
    allocation_percent  INTEGER     NOT NULL,
    role_in_project      VARCHAR(100),
    start_date           DATE,
    end_date             DATE,
    created_at           TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_allocation_percent CHECK (allocation_percent > 0 AND allocation_percent <= 100),
    CONSTRAINT chk_allocation_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
```

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| allocation_id | BIGSERIAL | PK | |
| employee_id | BIGINT | FK → employee, NOT NULL | |
| project_id | BIGINT | FK → project, NOT NULL | |
| allocation_percent | INTEGER | CHECK 0 < x <= 100 | Double-check ở cả DB (CHECK) và Service layer (business rule tổng ≤ 100%) |
| role_in_project | VARCHAR(100) | nullable | VD: Backend Developer |
| start_date / end_date | DATE | nullable | Dùng để tính "overlap" khi validate tổng % — xem `business-rules.md` |

**Index (quan trọng cho performance query workload/report):**
```sql
CREATE INDEX idx_allocation_employee ON allocation(employee_id);
CREATE INDEX idx_allocation_project  ON allocation(project_id);
CREATE INDEX idx_allocation_employee_project ON allocation(employee_id, project_id);
```

> `idx_allocation_employee` bắt buộc phải có vì hầu hết query quan trọng nhất (tính tổng allocation, workload report, overloaded report) đều `GROUP BY employee_id` hoặc `WHERE employee_id = ?`.

**FK behavior (cần quyết định trước khi code):**

| FK | ON DELETE | Lý do đề xuất |
|---|---|---|
| allocation.employee_id → employee | `RESTRICT` (mặc định) | Không cho xoá employee nếu còn allocation, tránh mất lịch sử. Nếu cần "xoá" thì dùng soft-delete ở employee (xem `business-rules.md`) |
| allocation.project_id → project | `RESTRICT` | Tương tự, không cho xoá project nếu còn allocation |

---

## 5. Full script (gộp, có FK — bản chuẩn để chạy)

```sql
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
    role_in_project      VARCHAR(100),
    start_date           DATE,
    end_date             DATE,
    created_at           TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_allocation_percent CHECK (allocation_percent > 0 AND allocation_percent <= 100),
    CONSTRAINT chk_allocation_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE UNIQUE INDEX idx_employee_code ON employee(employee_code);
CREATE UNIQUE INDEX idx_employee_email ON employee(email);
CREATE UNIQUE INDEX idx_project_code ON project(project_code);
CREATE INDEX idx_project_status ON project(status);
CREATE INDEX idx_allocation_employee ON allocation(employee_id);
CREATE INDEX idx_allocation_project ON allocation(project_id);
CREATE INDEX idx_allocation_employee_project ON allocation(employee_id, project_id);
```

---

## 6. Câu hỏi mở cần chốt trước khi code (đánh dấu TODO)

- [ ] `department` của employee: để free-text VARCHAR hay tách bảng `department` riêng? → Hiện chọn free-text cho đơn giản (fresher scope).
- [ ] Có cần bảng `department` riêng để chuẩn hoá không? → Không, ngoài phạm vi.
- [ ] Soft-delete cho employee/project? → Xem quyết định ở `business-rules.md` mục "Xoá dữ liệu".
