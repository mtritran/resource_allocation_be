# API Specifications

Quy ước chung áp dụng cho toàn bộ API bên dưới:
- Base path: `/api/v1`
- Content-Type: `application/json`
- Format lỗi: xem `business-rules.md` mục 4 (bảng Exception) — mọi lỗi trả về theo `ErrorResponse` chuẩn.
- Field convention: JSON dùng `camelCase` (map từ `snake_case` ở DB qua JPA).
- Response thành công: trả thẳng DTO (không bọc `ApiResponse<T>` wrapper) — xem lý do ở `coding-conventions.md` mục Response Format.

---

## 1. Employee API

### 1.1 Create Employee
```
POST /api/v1/employees
```

**Request — `EmployeeRequest`**
```json
{
  "employeeCode": "EMP001",
  "fullName": "Tuan Ho Anh",
  "email": "tuanha@company.com",
  "role": "Senior Developer",
  "department": "FSOFT-Q1"
}
```

| Field | Kiểu | Validation |
|---|---|---|
| employeeCode | String | `@NotBlank`, unique (check ở Service) |
| fullName | String | `@NotBlank` |
| email | String | `@NotBlank @Email`, unique |
| role | String | `@NotBlank` |
| department | String | optional |

**Response `201 Created` — `EmployeeResponse`**
```json
{
  "employeeId": 1,
  "employeeCode": "EMP001",
  "fullName": "Tuan Ho Anh",
  "email": "tuanha@company.com",
  "role": "Senior Developer",
  "department": "FSOFT-Q1",
  "createdAt": "2025-01-01T10:00:00"
}
```

**Error cases:** `409` employeeCode/email trùng (BR-EMP-01) · `400` validate DTO fail

---

### 1.2 Get All Employees
```
GET /api/v1/employees
```
**Query params (optional):** `?department=FSOFT-Q1&role=Senior+Developer&page=0&size=20`

**Response `200 OK`**
```json
{
  "content": [ /* EmployeeResponse[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42
}
```
> Dùng Spring `Pageable` chuẩn, không cần tự viết wrapper phân trang thủ công.

---

### 1.3 Get Employee by ID
```
GET /api/v1/employees/{id}
```
**Response `200 OK`:** `EmployeeResponse` (như mục 1.1)
**Error:** `404` nếu không tồn tại (BR-EMP-02)

---

### 1.4 Update Employee
```
PUT /api/v1/employees/{id}
```
**Request:** `EmployeeRequest` (giống Create)
**Response `200 OK`:** `EmployeeResponse`
**Error:** `404` không tồn tại · `409` nếu đổi email/code trùng với record khác

---

### 1.5 Delete Employee
```
DELETE /api/v1/employees/{id}
```
**Response:** `204 No Content`
**Error:** `404` không tồn tại · `409` còn allocation active (BR-EMP-03)

---

### 1.6 Get Employee Workload
```
GET /api/v1/employees/{id}/workload
```
**Response `200 OK` — `WorkloadResponse`**
```json
{
  "employeeId": 1,
  "employeeName": "Tuan Ho Anh",
  "totalAllocation": 80,
  "available": 20,
  "allocations": [
    { "projectCode": "NCG", "allocationPercent": 60 },
    { "projectCode": "GRID", "allocationPercent": 20 }
  ]
}
```
> `allocations` là phần mở rộng thêm so với spec gốc — hữu ích để FE hiển thị breakdown, không bắt buộc nhưng nên có.

---

## 2. Project API

### 2.1 Create Project
```
POST /api/v1/projects
```
**Request — `ProjectRequest`**
```json
{
  "projectCode": "NCG",
  "projectName": "New Core Gateway",
  "customer": "ABC Corp",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "status": "PLANNING"
}
```

| Field | Kiểu | Validation |
|---|---|---|
| projectCode | String | `@NotBlank`, unique |
| projectName | String | `@NotBlank` |
| customer | String | optional |
| startDate / endDate | LocalDate | optional, endDate >= startDate |
| status | Enum(PLANNING/ACTIVE/COMPLETED) | optional, default `PLANNING` nếu không truyền |

**Response `201 Created`:** `ProjectResponse` (tương tự request + `projectId`, `createdAt`)
**Error:** `409` projectCode trùng (BR-PRJ-01) · `400` endDate < startDate hoặc status không hợp lệ

---

### 2.2 Get All Projects
```
GET /api/v1/projects
```
**Query params (optional):** `?status=ACTIVE&page=0&size=20`

### 2.3 Get Project by ID
```
GET /api/v1/projects/{id}
```
**Error:** `404` (BR-PRJ-02)

### 2.4 Update Project
```
PUT /api/v1/projects/{id}
```
**Request:** `ProjectRequest`
**Lưu ý:** Đổi status sang `COMPLETED` được phép tự do (Option A, BR-PRJ-04) — không chặn dù còn allocation active.

### 2.5 Delete Project
```
DELETE /api/v1/projects/{id}
```
**Response:** `204 No Content`
**Error:** `404` · `409` nếu còn allocation liên quan (tương tự BR-EMP-03, áp dụng chung logic cho project)

---

## 3. Allocation API

### 3.1 Create Allocation
```
POST /api/v1/allocations
```
**Request — `AllocationRequest`**
```json
{
  "employeeId": 1,
  "projectId": 2,
  "allocationPercent": 50,
  "roleInProject": "Backend Developer",
  "startDate": "2025-01-01",
  "endDate": "2025-06-30"
}
```

| Field | Kiểu | Validation |
|---|---|---|
| employeeId | Long | `@NotNull`, phải tồn tại (BR-ALC-04) |
| projectId | Long | `@NotNull`, phải tồn tại, project không COMPLETED (BR-PRJ-03) |
| allocationPercent | Integer | `@Min(1) @Max(100)` (BR-ALC-01) + check tổng ≤ 100% (BR-ALC-02) |
| roleInProject | String | optional |
| startDate / endDate | LocalDate | optional, endDate >= startDate (BR-ALC-06) |

**Response `201 Created` — `AllocationResponse`**
```json
{
  "allocationId": 10,
  "employeeId": 1,
  "employeeName": "Tuan Ho Anh",
  "projectId": 2,
  "projectCode": "NCG",
  "allocationPercent": 50,
  "roleInProject": "Backend Developer",
  "startDate": "2025-01-01",
  "endDate": "2025-06-30"
}
```

**Error cases:**
- `404` employeeId/projectId không tồn tại
- `400` allocationPercent ngoài khoảng 1-100
- `400` tổng allocation vượt 100% → `AllocationExceededException`, message `"Employee allocation exceeds 100%"`
- `400` project đang COMPLETED

---

### 3.2 Update Allocation
```
PUT /api/v1/allocations/{id}
```
**Request:** `AllocationRequest`
**Lưu ý quan trọng:** Khi tính tổng 100% phải **loại trừ chính allocation đang update** ra khỏi tổng trước khi cộng giá trị mới (BR-ALC-02, Case B) — đây là lỗi hay gặp nhất khi code phần này.

**Response `200 OK`:** `AllocationResponse`
**Error:** giống Create + `404` nếu allocationId không tồn tại

---

### 3.3 Delete Allocation
```
DELETE /api/v1/allocations/{id}
```
**Response:** `204 No Content`
**Error:** `404` nếu không tồn tại
**Lưu ý:** Xoá cứng, không có ràng buộc chặn (BR-ALC-05).

---

### 3.4 Get Allocation by ID
```
GET /api/v1/allocations/{id}
```
**Response:** `AllocationResponse`

### 3.5 Get All Allocations
```
GET /api/v1/allocations
```
**Query params (optional):** `?employeeId=1&projectId=2&page=0&size=20`

---

## 4. Reporting API

### 4.1 Employee Utilization Report
```
GET /api/v1/reports/utilization
```
**Response `200 OK`**
```json
[
  { "employeeId": 1, "employeeName": "A", "totalAllocation": 100 },
  { "employeeId": 2, "employeeName": "B", "totalAllocation": 80 }
]
```

### 4.2 Available Resource Report
```
GET /api/v1/reports/available?minAvailable=50
```
> `minAvailable` optional, dùng cho AI Resource Recommendation (mục 5.1) — mặc định không truyền thì trả tất cả employee có `available > 0`.

**Response `200 OK`**
```json
[
  { "employeeId": 1, "employeeName": "A", "available": 20 },
  { "employeeId": 2, "employeeName": "B", "available": 50 }
]
```

### 4.3 Overloaded Employee Report
```
GET /api/v1/reports/overloaded
```
**Điều kiện:** `totalAllocation > 90` (BR mục 5, bảng Reporting)

**Response `200 OK`**
```json
[
  { "employeeId": 5, "employeeName": "Tuan", "totalAllocation": 95 },
  { "employeeId": 6, "employeeName": "Nam", "totalAllocation": 100 }
]
```

---

## 5. AI Bonus API

### 5.1 AI Resource Recommendation
```
POST /api/v1/ai/recommend
```
**Request**
```json
{ "query": "Tìm Java Developer còn tối thiểu 50% available" }
```
**Response `200 OK`**
```json
{
  "recommendedResources": [
    { "employee": "Nguyen Van A", "available": 60 }
  ]
}
```
> Luồng xử lý đề xuất: parse `query` (role + ngưỡng %) → gọi lại `AllocationService` để lấy Available Resource Report có sẵn (mục 4.2) → filter theo role → gọi AI model chỉ để format lại câu trả lời tự nhiên (không để AI tự bịa số liệu).

### 5.2 AI Risk Detection
```
POST /api/v1/ai/risk-detection
```
**Request**
```json
{ "query": "Sprint tới cần thêm 2 Java Developer" }
```
**Response `200 OK`**
```json
{
  "risks": [
    "Team đang sử dụng 92% capacity.",
    "Chỉ còn 1 resource available trên 50%."
  ]
}
```
> Tương tự 5.1 — số liệu (92% capacity, số resource available) phải lấy từ report thật (mục 4.1, 4.2) rồi mới đưa vào prompt cho AI tổng hợp câu chữ, tránh AI tự tính sai.

---

## 6. Tổng hợp mapping DTO ↔ Entity

| Module | Entity | Request DTO | Response DTO |
|---|---|---|---|
| Employee | `Employee` | `EmployeeRequest` | `EmployeeResponse` |
| Project | `Project` | `ProjectRequest` | `ProjectResponse` |
| Allocation | `Allocation` | `AllocationRequest` | `AllocationResponse` |
| Report | *(không có entity riêng, query trực tiếp)* | — | `UtilizationResponse`, `AvailableResponse`, `OverloadedResponse` |

Vị trí đặt DTO và cách mapping (MapStruct vs thủ công): xem `coding-conventions.md`.
