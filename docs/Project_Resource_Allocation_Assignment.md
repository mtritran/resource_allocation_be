# Project Resource Allocation Management System

## 1. Bối cảnh

Công ty outsourcing thường triển khai nhiều dự án song song. Một nhân viên có thể tham gia nhiều dự án với tỷ lệ phân bổ thời gian (allocation) khác nhau.

Ví dụ:

- NCG: 50%
- GRID: 30%
- Internal AI: 20%

Tổng allocation = 100% → Hợp lệ.

Nếu tổng allocation vượt quá 100%, hệ thống phải từ chối.

---

# 2. Mục tiêu hệ thống

Cho phép PM hoặc Resource Manager:

- Quản lý nhân viên
- Quản lý dự án
- Phân bổ nhân sự vào dự án
- Theo dõi workload
- Tìm resource còn available
- Báo cáo utilization

---

# 3. Functional Requirements

## 3.1 Employee Management

### Thông tin nhân viên

- Employee Code
- Full Name
- Email
- Role
- Department

### Ví dụ

```text
EMP001
Tuan Ho Anh
tuanha@company.com
Senior Developer
FSOFT-Q1
```

### API

```http
POST /employees
GET /employees
GET /employees/{id}
```

---

## 3.2 Project Management

### Thông tin dự án

- Project Code
- Project Name
- Customer
- Start Date
- End Date
- Status

### Status

```text
PLANNING
ACTIVE
COMPLETED
```

### API

```http
POST /projects
GET /projects
GET /projects/{id}
```

---

## 3.3 Resource Allocation

### Thông tin Allocation

- Employee
- Project
- Allocation Percent
- Start Date
- End Date
- Role In Project

Ví dụ:

```text
Employee : EMP001
Project  : NCG
Allocation : 60%
Role : Backend Developer
```

### Business Rule 1

Allocation phải nằm trong khoảng:

```text
0 < allocation <= 100
```

### Business Rule 2

Tổng allocation của một nhân viên không được vượt quá 100%.

Ví dụ:

```text
Project A : 60%
Project B : 40%
```

Kết quả:

```text
100% -> Hợp lệ
```

Ví dụ không hợp lệ:

```text
Project A : 60%
Project B : 50%
```

Kết quả:

```text
110% -> Reject
```

Response:

```json
{
  "message": "Employee allocation exceeds 100%"
}
```

### Business Rule 3

Không cho phép allocate vào dự án có trạng thái:

```text
COMPLETED
```

---

# 4. Reporting Functions

## 4.1 Employee Utilization Report

Hiển thị tổng allocation của từng nhân viên.

Ví dụ:

| Employee | Allocation |
|----------|------------|
| A | 100% |
| B | 80% |
| C | 40% |

SQL tham khảo:

```sql
SELECT employee_id,
       SUM(allocation_percent)
FROM allocations
GROUP BY employee_id;
```

---

## 4.2 Available Resource Report

Tìm nhân viên còn thời gian khả dụng.

Điều kiện:

```text
Allocation < 100%
```

Kết quả:

| Employee | Available |
|----------|-----------|
| A | 20% |
| B | 50% |

---

## 4.3 Overloaded Employee Report

Tìm nhân viên có workload cao.

Điều kiện:

```text
Allocation > 90%
```

Ví dụ:

```text
Tuan : 95%
Nam  : 100%
```

---

# 5. Database Design

## employee

```sql
CREATE TABLE employee (
    employee_id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE,
    full_name VARCHAR(100),
    email VARCHAR(100),
    role VARCHAR(50),
    department VARCHAR(50)
);
```

## project

```sql
CREATE TABLE project (
    project_id BIGSERIAL PRIMARY KEY,
    project_code VARCHAR(20) UNIQUE,
    project_name VARCHAR(200),
    customer VARCHAR(100),
    status VARCHAR(20)
);
```

## allocation

```sql
CREATE TABLE allocation (
    allocation_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT,
    project_id BIGINT,
    allocation_percent INTEGER,
    role_in_project VARCHAR(100),
    start_date DATE,
    end_date DATE
);
```

---

# 6. API Requirements

## Create Allocation

```http
POST /allocations
```

Request:

```json
{
  "employeeId": 1,
  "projectId": 2,
  "allocationPercent": 50,
  "roleInProject": "Backend Developer"
}
```

## Update Allocation

```http
PUT /allocations/{id}
```

## Get Employee Workload

```http
GET /employees/{id}/workload
```

Response:

```json
{
  "employeeId": 1,
  "employeeName": "Tuan Ho Anh",
  "totalAllocation": 80,
  "available": 20
}
```

---

# 7. Technical Requirements

## Backend

- Java 17+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven

## Validation

- @NotBlank
- @Email
- @Min
- @Max

## Exception Handling

- EmployeeNotFoundException
- ProjectNotFoundException
- AllocationExceededException
- Global Exception Handler

## Logging

- Create Allocation
- Update Allocation
- Remove Allocation

---

# 8. AI Bonus Features

## AI Resource Recommendation

Người dùng nhập:

```text
Tìm Java Developer còn tối thiểu 50% available.
```

AI đề xuất:

```json
{
  "recommendedResources": [
    {
      "employee": "Nguyen Van A",
      "available": 60
    }
  ]
}
```

## AI Risk Detection

Input:

```text
Sprint tới cần thêm 2 Java Developer.
```

Output:

```text
Risk:
- Team đang sử dụng 92% capacity.
- Chỉ còn 1 resource available trên 50%.
```

---

# 9. Deliverables

1. Source Code Git Repository
2. SQL Script Create Table
3. README.md
4. Postman Collection
5. API Screenshot
6. AI Review Report

---

# 10. Tiêu Chí Đánh Giá

## Java

- OOP
- SOLID
- Exception Handling
- Layer Design

## Database

- PK/FK
- JOIN
- GROUP BY
- Aggregate Functions

## Spring Boot

- REST API
- Validation
- Service Layer
- Repository Layer

## Business Logic

- Allocation Validation
- Workload Calculation
- Project Status Validation

## Bonus

- Unit Test
- Swagger
- Docker
- AI Integration

---

# Mục Tiêu

Hoàn thành hệ thống quản lý phân bổ nhân sự giúp Fresher thực hành quy trình phát triển một ứng dụng enterprise thực tế trong môi trường outsourcing, bao gồm Java Backend, Database Design, REST API, Business Validation và AI-assisted Development.
