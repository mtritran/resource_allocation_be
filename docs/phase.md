# Implementation Phases

Lộ trình chia theo phase, mỗi phase build xong nên chạy được và test thủ công qua Postman trước khi sang phase tiếp theo. Thứ tự ưu tiên: nền tảng → CRUD đơn giản → business logic phức tạp → report → bonus.

---

## Phase 0 — Setup (chuẩn bị)

### 0.1 Docker Compose — PostgreSQL + pgAdmin

- [x] Tạo `docker-compose.yml` tại thư mục gốc của project:
  ```yaml
  version: '3.8'

  services:
    postgres:
      image: postgres:16-alpine
      container_name: resource_allocation_db
      restart: unless-stopped
      ports:
        - "5432:5432"
      environment:
        POSTGRES_DB: resource_allocation
        POSTGRES_USER: dev
        POSTGRES_PASSWORD: dev123
      volumes:
        - postgres_data:/var/lib/postgresql/data
      healthcheck:
        test: ["CMD-SHELL", "pg_isready -U dev -d resource_allocation"]
        interval: 10s
        timeout: 5s
        retries: 5

    pgadmin:
      image: dpage/pgadmin4:latest
      container_name: resource_allocation_pgadmin
      restart: unless-stopped
      ports:
        - "5050:80"
      environment:
        PGADMIN_DEFAULT_EMAIL: admin@company.com
        PGADMIN_DEFAULT_PASSWORD: admin123
      depends_on:
        postgres:
          condition: service_healthy
      volumes:
        - pgadmin_data:/var/lib/pgadmin

  volumes:
    postgres_data:
    pgadmin_data:
  ```
- [x] Chạy thử: `docker compose up -d` → kiểm tra `docker ps` thấy 2 container đang chạy.
- [x] Vào `http://localhost:5050`, login `admin@company.com / admin123`, register server:
  - Host: `postgres`
  - Port: `5432`
  - Username: `dev`
  - Password: `dev123`

### 0.2 Khởi tạo Spring Boot project

- [x] Tạo Spring Boot project (Spring Initializr) với các dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Validation (`spring-boot-starter-validation`)
  - Lombok
  - AOP (`spring-boot-starter-aop`)
  - **Flyway** (`flyway-core` + `flyway-database-postgresql`)
  - Springdoc OpenAPI (swagger)
  - Spring Boot Actuator
- [x] Cấu hình `application.yml`:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/resource_allocation
      username: dev
      password: dev123
      driver-class-name: org.postgresql.Driver

    jpa:
      hibernate:
        ddl-auto: validate    # Flyway quản lý schema, JPA chỉ validate
      show-sql: true
      properties:
        hibernate:
          format_sql: true
          dialect: org.hibernate.dialect.PostgreSQLDialect

    flyway:
      enabled: true
      locations: classpath:db/migration
      baseline-on-migrate: true

  server:
    port: 8080

  springdoc:
    api-docs:
      path: /api-docs
    swagger-ui:
      path: /swagger-ui.html

  logging:
    level:
      com.company.resourceallocation: DEBUG
  ```

### 0.3 Flyway migration — thay thế chạy SQL script thủ công

- [x] Tạo migration file đầu tiên tại `src/main/resources/db/migration/V1__init_schema.sql`:
  ```sql
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
      created_at          TIMESTAMP   NOT NULL DEFAULT now(),
      updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
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

### 0.4 Package structure + verify

- [x] Tạo package structure (tạo trước các package rỗng):
  ```
  com.company.resourceallocation
  ├── core
  │   ├── employee
  │   │   └── dto
  │   ├── project
  │   │   └── dto
  │   └── allocation
  │       └── dto
  ├── exception
  ├── aspect
  ├── report
  │   └── dto
  ├── ai
  │   └── dto
  └── config
  ```
- [x] Chạy `docker compose up -d` (nếu chưa chạy ở bước 0.1).
- [x] Start Spring Boot app → kiểm tra log thấy Flyway chạy migration `V1__init_schema.sql` thành công.
- [x] Verify: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}` hoặc vào Swagger `http://localhost:8080/swagger-ui.html`.

**Deliverable:** Project chạy được, PostgreSQL + pgAdmin qua Docker Compose, schema tự động tạo qua Flyway, package structure sẵn sàng.

---

## Phase 1 — Foundation dùng chung

Làm trước vì mọi module sau đều phụ thuộc vào phần này.

- [x] `exception/ErrorResponse.java`
- [x] `exception/ResourceNotFoundException.java`, `DuplicateResourceException.java` (2 exception dùng chung nhiều module nhất)
- [x] `exception/GlobalExceptionHandler.java` — xử lý 2 exception trên + `MethodArgumentNotValidException`
- [x] `aspect/LoggingAspect.java` — pointcut trỏ vào `core..*Service.create*/update*/delete*` (viết sẵn, dù chưa có Service nào để log — sẽ hoạt động ngay khi module đầu tiên xong)
- [x] `config/OpenApiConfig.java` (bonus, có thể để cuối)

**Deliverable:** Chạy 1 request lỗi giả (VD gọi endpoint chưa tồn tại) vẫn thấy format `ErrorResponse` chuẩn khi tích hợp module đầu tiên.

---

## Phase 2 — Module Employee (CRUD đơn giản nhất, làm mẫu cho 2 module sau)

- [x] `Employee.java` (Entity)
- [x] `EmployeeRepository.java`
- [x] `dto/EmployeeRequest.java`, `dto/EmployeeResponse.java`
- [x] `EmployeeMapper.java`
- [x] `EmployeeService.java` — CRUD + BR-EMP-01 (check trùng code/email), BR-EMP-02 (404)
- [x] `EmployeeController.java` — 4 endpoint theo `api-specs.md` mục 1.1-1.5 (chưa cần `/workload` vội, để Phase 4)
- [x] Test thủ công qua Postman: Create, Get all, Get by id, Update, Delete + case lỗi (trùng code, id không tồn tại)

**Deliverable:** CRUD Employee hoàn chỉnh, dùng làm pattern mẫu để copy sang Project/Allocation.

> Lưu ý: BR-EMP-03 (chặn xoá khi còn allocation active) phụ thuộc vào module Allocation → để lại implement ở Phase 5, tạm thời Phase 2 xoá tự do.

---

## Phase 3 — Module Project

- [x] `Project.java` (Entity) — bao gồm enum `ProjectStatus { PLANNING, ACTIVE, COMPLETED }`
- [x] `ProjectRepository.java`
- [x] `dto/ProjectRequest.java`, `dto/ProjectResponse.java`
- [x] `ProjectMapper.java`
- [x] `ProjectService.java` — CRUD + BR-PRJ-01 (check trùng code), BR-PRJ-02 (404), validate `endDate >= startDate`
- [x] `ProjectController.java` — theo `api-specs.md` mục 2.1-2.5
- [ ] Test thủ công qua Postman

**Deliverable:** CRUD Project hoàn chỉnh, có validate status enum.

---

## Phase 4 — Module Allocation (phần lõi, quan trọng nhất)

Đây là phase trọng tâm của cả hệ thống — chứa toàn bộ business rule phức tạp.

- [x] `Allocation.java` (Entity) — có FK tới `Employee`, `Project`
- [x] `AllocationRepository.java` — thêm custom query:
  ```java
  @Query("SELECT COALESCE(SUM(a.allocationPercent), 0) FROM Allocation a WHERE a.employee.id = :employeeId AND a.id <> :excludeId")
  Integer sumAllocationByEmployeeExcluding(Long employeeId, Long excludeId);
  ```
  (dùng chung cho cả create — truyền `excludeId = -1` hoặc overload method — và update, xem BR-ALC-02 Case A/B ở `business-rules.md`)
- [x] `dto/AllocationRequest.java`, `dto/AllocationResponse.java`
- [x] `AllocationMapper.java`
- [x] `exception/AllocationExceededException.java`, `InvalidProjectStatusException.java`, `InvalidAllocationPercentException.java`
- [x] Bổ sung handler cho 3 exception trên vào `GlobalExceptionHandler`
- [x] `AllocationService.java` — implement đầy đủ theo flow ở `architecture.md` mục 6:
  - BR-ALC-01: check percent 1-100
  - BR-ALC-02: check tổng ≤ 100% (Case A create, Case B update — **test kỹ case update**)
  - BR-ALC-03/BR-PRJ-03: check project không COMPLETED
  - BR-ALC-04: check employee/project tồn tại
  - BR-ALC-05: delete tự do
  - BR-ALC-06: check startDate/endDate
- [x] `AllocationController.java` — theo `api-specs.md` mục 3.1-3.5
- [x] Quay lại `EmployeeService`: bổ sung BR-EMP-03 (chặn xoá employee còn allocation active) — giờ đã có `AllocationRepository` để check
- [x] Test thủ công kỹ các case:
  - [x] Tạo allocation tổng đúng 100% → pass
  - [x] Tạo allocation tổng vượt 100% → reject đúng message
  - [x] Update allocation giảm % (case dễ sai nếu quên loại trừ record đang sửa) → phải pass
  - [x] Tạo allocation vào project COMPLETED → reject
  - [x] Xoá employee còn allocation active → reject

**Deliverable:** Module Allocation hoàn chỉnh, pass toàn bộ test case ở `business-rules.md`.

---

## Phase 5 — Employee Workload Endpoint

- [x] Bổ sung `GET /employees/{id}/workload` vào `EmployeeController` (theo `api-specs.md` mục 1.6) — cần `AllocationRepository` nên để sau Phase 4.
- [x] `WorkloadResponse` DTO.

**Deliverable:** Endpoint workload trả đúng `totalAllocation`, `available`, breakdown theo project.

---

## Phase 6 — Reporting Module

- [x] `report/ReportService.java` — 3 method: utilization, available, overloaded (query qua `AllocationRepository`, `GROUP BY employee_id`)
- [x] `report/dto/UtilizationResponse.java`, `AvailableResponse.java`, `OverloadedResponse.java`
- [x] `report/ReportController.java` — theo `api-specs.md` mục 4.1-4.3
- [x] Test thủ công: seed vài data, verify số liệu report đúng với công thức ở `business-rules.md` mục 5

**Deliverable:** 3 report endpoint hoạt động đúng.

---

## Phase 7 — AI Bonus Features

- [x] `ai/AiRecommendationService.java` — gọi `ReportService.getAvailable()` lấy số liệu thật → build prompt → gọi AI model format câu trả lời (xem lưu ý ở `api-specs.md` mục 5.1, không để AI tự bịa số)
- [x] `ai/AiController.java` — `/ai/recommend`, `/ai/risk-detection`
- [x] Test thủ công với vài query mẫu trong spec gốc

**Deliverable:** 2 endpoint AI hoạt động, số liệu khớp với report thật.

---

## Phase 8 — Polish & Bonus kỹ thuật

- [x] Swagger UI hoàn chỉnh (`@Operation` cho từng endpoint theo `coding-conventions.md` mục 10)
- [x] Unit test cho `AllocationService` (ưu tiên cao nhất — theo `coding-conventions.md` mục 9), sau đó `EmployeeService`, `ProjectService`
- [x] Dockerfile cho app (multi-stage build) + cập nhật `docker-compose.yml` thêm service app
- [x] README.md: hướng dẫn chạy project (yêu cầu Docker), link Postman collection
- [x] Export Postman Collection theo đúng `api-specs.md`
- [x] Chụp API screenshot cho Deliverables

**Deliverable:** Đủ toàn bộ danh sách ở mục 9 "Deliverables" trong spec gốc.

---

## Ghi chú thứ tự phụ thuộc (dependency giữa các phase)

```
Phase 0 (setup)
   │
Phase 1 (exception + AOP dùng chung)
   │
Phase 2 (Employee) ──┐
Phase 3 (Project)  ──┼── không phụ thuộc nhau, có thể làm song song
                      │
                      ▼
              Phase 4 (Allocation) ← phụ thuộc cả Employee + Project
                      │
              ┌───────┴────────┐
              ▼                ▼
       Phase 5 (Workload)  Phase 6 (Report)
                      │
                      ▼
              Phase 7 (AI Bonus) ← phụ thuộc Phase 6
                      │
                      ▼
              Phase 8 (Polish)
```

> Nếu làm một mình (fresher solo), nên đi tuần tự đúng thứ tự Phase 0 → 8. Nếu có thời gian gấp, có thể bỏ qua Phase 7-8 và vẫn đảm bảo core (Phase 0-6) chạy đúng — đây là phần chiếm điểm chính theo mục 10 "Tiêu Chí Đánh Giá" ở spec gốc.
