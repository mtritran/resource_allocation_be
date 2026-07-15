# AI Review Report — Resource Allocation System

> **Dự án:** Hệ thống quản lý phân bổ nguồn lực (Resource Allocation System)  
> **Ngày review:** 2026-07-15  
> **Công cụ:** Code analysis bằng AI (Claude)  
> **Mục đích:** Soát xét mã nguồn theo tiêu chí đánh giá của Assignment

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Java & OOP](#2-java--oop)
3. [Database & SQL](#3-database--sql)
4. [Spring Boot & REST API](#4-spring-boot--rest-api)
5. [Business Logic](#5-business-logic)
6. [Bonus Features](#6-bonus-features)
7. [Phát hiện chi tiết](#7-phát-hiện-chi-tiết)
8. [Khuyến nghị cải thiện](#8-khuyến-nghị-cải-thiện)
9. [Kết luận](#9-kết-luận)

---

## 1. Tổng quan

| Hạng mục | Đánh giá |
|---|---|
| Tổng số files | ~50 files (Java, SQL, YAML, Docker, docs) |
| Entity | 3: Employee, Project, Allocation |
| Controllers | 5: Employee, Project, Allocation, Report, AI |
| Services | 5: Employee, Project, Allocation, Report, AI |
| Repositories | 3: Employee, Project, Allocation |
| Exception | 6 custom + GlobalExceptionHandler |
| Test files | 10 (Service + Controller tests) |

**Điểm tổng thể: 9/10** ✅

---

## 2. Java & OOP

### 2.1 OOP — 10/10 ✅

| Tiêu chí | Nhận xét |
|---|---|
| **Encapsulation** | Tất cả field đều `private` với Lombok `@Getter/@Setter`. Dùng `@FieldDefaults(level = AccessLevel.PRIVATE)` |
| **Inheritance** | Custom exceptions kế thừa `RuntimeException`, dùng constructor `super(message)` |
| **Polymorphism** | `GlobalExceptionHandler` dùng `@ExceptionHandler(RuntimeException.class)` cho nhóm exception cùng kiểu |
| **Abstraction** | JpaRepository cung cấp sẵn interface CRUD, Service ẩn chi tiết implement |

### 2.2 SOLID — 9/10 ✅

| Nguyên lý | Đạt? | Giải thích |
|---|---|---|
| **S**ingle Responsibility | ✅ | Mỗi class làm 1 việc: Controller nhận request, Service xử lý business, Repository query DB |
| **O**pen/Closed | ✅ | Có thể thêm exception mới không cần sửa handler cũ (thêm `@ExceptionHandler` riêng) |
| **L**iskov Substitution | ✅ | `RuntimeException` ← các custom exception, đều xài được qua handler chung |
| **I**nterface Segregation | ✅ | Repository chỉ extend `JpaRepository` với method cần |
| **D**ependency Inversion | ✅ | Inject dependency qua constructor (`@RequiredArgsConstructor`), không hard-code |

### 2.3 Exception Handling — 9/10 ✅

```
✅ 6 custom exceptions: ResourceNotFoundException, DuplicateResourceException,
   AllocationExceededException, InvalidProjectStatusException,
   InvalidAllocationPercentException, EmployeeInUseException
✅ GlobalExceptionHandler với @RestControllerAdvice
✅ ErrorResponse chuẩn: timestamp, status, error, message, path
✅ Xử lý MethodArgumentNotValidException cho validation DTO
```

**Phát hiện:**  
- ⚠️ `InvalidFormatException` bắt bằng string `className.contains(...)` (fragile, nên bắt bằng `@ExceptionHandler` riêng)  
- ⚠️ `IllegalArgumentException` dùng chung cho cả date validation — không có exception riêng

### 2.4 Layer Design — 10/10 ✅

```
Controller (@RestController)
    ↓ DTO
Service (@Service)           ← business logic
    ↓ Entity
Repository (JpaRepository)   ← data access
```

- Controller **không chứa** business logic ✅
- Service nhận/trả **DTO**, không lộ Entity ✅
- Repository chỉ chứa query, không logic nghiệp vụ ✅
- Mapper (MapStruct) chuyển đổi Entity ↔ DTO ✅

---

## 3. Database & SQL

### 3.1 PK/FK — 10/10 ✅

```sql
-- PK
employee_id     BIGSERIAL PRIMARY KEY
project_id      BIGSERIAL PRIMARY KEY
allocation_id   BIGSERIAL PRIMARY KEY

-- FK
employee_id  BIGINT NOT NULL REFERENCES employee(employee_id)
project_id   BIGINT NOT NULL REFERENCES project(project_id)
```

### 3.2 JOIN — 10/10 ✅

```sql
SELECT e.employee_id, e.full_name, SUM(a.allocation_percent)
FROM employee e
LEFT JOIN allocation a ON a.employee_id = e.employee_id
GROUP BY e.employee_id, e.full_name
```

- Dùng `LEFT JOIN` để bao gồm cả nhân viên chưa có allocation ✅
- Không dùng `INNER JOIN` (sẽ mất nhân viên không có allocation → sai số liệu report) ✅

### 3.3 GROUP BY — 10/10 ✅

```sql
GROUP BY e.employee_id, e.full_name
```

- Nhóm đúng và đủ cột (tất cả cột SELECT không phải aggregate) ✅

### 3.4 Aggregate Functions — 10/10 ✅

```sql
SUM(allocation_percent)       -- tổng allocation
COALESCE(SUM(...), 0)          -- xử lý NULL
COUNT(...)                     -- nếu cần
```

- `COALESCE` xử lý đúng case employee chưa có allocation (SUM = NULL → 0) ✅

---

## 4. Spring Boot & REST API

### 4.1 REST API — 10/10 ✅

| Method | Endpoint | Status | Đúng REST? |
|---|---|---|---|
| POST | `/api/v1/employees` | 201 | ✅ |
| GET | `/api/v1/employees` | 200 | ✅ |
| GET | `/api/v1/employees/{id}` | 200 | ✅ |
| PUT | `/api/v1/employees/{id}` | 200 | ✅ |
| DELETE | `/api/v1/employees/{id}` | 204 | ✅ |

- Base path `/api/v1` ✅
- Content-Type `application/json` ✅
- Không bọc response trong `ApiResponse<T>` wrapper (trả thẳng DTO) ✅

### 4.2 Validation — 10/10 ✅

- `@Valid` trên `@RequestBody` ✅
- `@NotBlank`, `@Email`, `@Min`, `@Max`, `@NotNull` trên Request DTO ✅
- Business validation (tổng ≤ 100%, project COMPLETED) trong Service layer ✅
- Message lỗi tùy chỉnh: `"Employee code is required"` ✅

### 4.3 Service Layer — 10/10 ✅

- `@Transactional` trên method ghi dữ liệu ✅
- `@Transactional(readOnly = true)` trên method đọc ✅
- Xử lý đúng business rule trước khi save ✅

### 4.4 Repository Layer — 10/10 ✅

- Spring Data JPA method naming convention ✅
- `@Query` với JPQL và native SQL ✅
- Filter động dùng `IS NULL OR ...` pattern ✅

---

## 5. Business Logic

### 5.1 Allocation Validation — 10/10 ✅

| Rule | Implement | Test |
|---|---|---|
| BR-ALC-01: percent 1-100 | `@Min(1) @Max(100)` + check trong Service | ✅ A1-A5 |
| BR-ALC-02: tổng ≤ 100% (Create) | `sumAllocationByEmployeeExcluding(id, -1L)` | ✅ A6-A9 |
| BR-ALC-02: tổng ≤ 100% (Update) | `sumAllocationByEmployeeExcluding(id, excludeId)` | ✅ A10-A13 |
| BR-ALC-03: project COMPLETED | Check `project.status == COMPLETED` | ✅ A14-A17 |
| BR-ALC-04: employee/project tồn tại | `findById()` → `ResourceNotFoundException` | ✅ A18-A19 |
| BR-ALC-05: delete | `hard delete`, log qua AOP | ✅ A20-A21 |
| BR-ALC-06: dates | `endDate >= startDate` | ✅ A22-A23 |

**Lưu ý đặc biệt — Case Update giảm % (A10):**  
Code đã loại trừ đúng `allocationId` đang update khỏi tổng:
```java
int currentSum = allocationRepository.sumAllocationByEmployeeExcluding(
    request.getEmployeeId(), excludeId);  // excludeId = id cũ
```
Đây là lỗi thường gặp — dự án đã xử lý đúng ✅

### 5.2 Workload Calculation — 10/10 ✅

```
GET /employees/{id}/workload → totalAllocation, available, breakdown
```

- Tính `totalAllocation` = SUM(allocationPercent) ✅
- Tính `available` = 100 - totalAllocation ✅
- Breakdown theo project (projectCode + allocationPercent) ✅

### 5.3 Project Status Validation — 9/10 ✅

- BR-PRJ-01: code trùng → `409` ✅
- BR-PRJ-02: không tìm thấy → `404` ✅
- BR-PRJ-03: không allocate vào COMPLETED → `400` ✅
- BR-PRJ-04: chuyển COMPLETED tự do (Option A) ✅
- ⚠️ BR-PRJ-04: xoá project còn allocation → **TODO còn trong code**

---

## 6. Bonus Features

### 6.1 Unit Test — 9/10 ✅

| File | Loại | Coverage |
|---|---|---|
| `AllocationServiceTest` | Mockito (Unit) | 8 tests, cover create/update/exception |
| `AllocationControllerTest` | MockMvc (Integration) | 6 tests, cover create/update/delete/409 |
| `EmployeeServiceTest` | Mockito (Unit) | 6 tests, cover CRUD + exception |
| `EmployeeControllerTest` | MockMvc (Integration) | 7 tests, cover CRUD + workload |
| `ProjectServiceTest` | Mockito (Unit) | 5 tests, cover CRUD + exception |
| `ProjectControllerTest` | MockMvc (Integration) | 5 tests, cover create/update/delete |
| `ReportControllerTest` | MockMvc (Integration) | 5 tests, cover 3 report types |
| `AiRecommendationServiceTest` | Mockito (Unit) | 4 tests, cover recommend + risk + fallback |

**Tổng: ~46 test cases**  
- Service layer dùng Mockito → test nhanh, không cần DB ✅  
- Controller layer dùng `@SpringBootTest` + `@Transactional` → rollback sau mỗi test ✅  
- ⚠️ `@SpringBootTest` cần H2 hoặc PostgreSQL chạy — có thể ảnh hưởng CI speed

### 6.2 Swagger — 10/10 ✅

```java
@Operation(summary = "Create Employee", description = "...")
@ApiResponses({ @ApiResponse(responseCode = "201", description = "...") })
@Tag(name = "Employee API", description = "...")
```

- `springdoc-openapi` v3 (thay vì Springfox cũ) ✅
- Mọi Controller method đều có `@Operation` ✅
- Cấu hình `OpenApiConfig` với title/description ✅
- Swagger UI tại `/swagger-ui.html` ✅

### 6.3 Docker — 10/10 ✅

**Dockerfile:**
```
- Multi-stage build (Maven build → JRE runtime)
- FROM eclipse-temurin:17-jre
- COPY jar từ build stage
```

**docker-compose.yml:**
```
- postgres:16-alpine + healthcheck
- pgadmin4 + depends_on healthcheck
- app service (build từ Dockerfile)
- Environment variables từ .env
- Volumes cho data persistence
```

- ⚠️ `.env` cần thêm `APP_PORT` (docker-compose dùng `${APP_PORT}` nhưng .env không có)

### 6.4 AI Integration — 9/10 ✅

| Feature | Mô tả |
|---|---|
| **AI Recommend** (5.1) | Parse query → lấy available report thật → build prompt → gọi Gemini → JSON response |
| **AI Risk Detection** (5.2) | Lấy utilization + overloaded report thật → build prompt → gọi Gemini → risk list |
| **Fallback** | Khi Gemini lỗi hoặc parse fail → trả data từ database, không để AI bịa số |

- ✅ Kiến trúc **"AI chỉ format câu chữ, số liệu từ database"** — tránh AI hallucination
- ✅ GeminiClient dùng Spring 6 `RestClient` (thay vì RestTemplate cũ)
- ✅ Fallback mechanism khi API key không config hoặc Gemini trả lỗi
- ⚠️ API key config qua env `GEMINI_API_KEY` — cần nhớ set trước khi test

---

## 7. Phát hiện chi tiết (Issues Found)

### 🔴 Nghiêm trọng (cần sửa)

| # | File | Vấn đề | Mức độ |
|---|---|---|---|
| 1 | `ProjectService.java:78` | TODO còn trong code: chưa check allocation trước khi delete project | **Cao** |
| 2 | `pom.xml:8` | Spring Boot `4.1.0` không tồn tại (hiện tại là 3.x). Các artifact `webmvc`, `aspectj` sai tên | **Cao** |

### 🟡 Trung bình (nên sửa)

| # | File | Vấn đề |
|---|---|---|
| 3 | `GlobalExceptionHandler.java:50` | Bắt `InvalidFormatException` bằng class name string |
| 4 | `.env` | Thiếu biến `APP_PORT` mặc dù docker-compose dùng |

### 🟢 Nhẹ (gợi ý)

| # | File | Vấn đề |
|---|---|---|
| 5 | `Allocation.java:39` | `role_in_project` length = 50 trong Entity nhưng DB = 100 |
| 6 | `AllocationResponse.java` | Thiếu field `createdAt` |

---

## 8. Khuyến nghị cải thiện

### Có thể nâng cao (out of scope hiện tại, để sau):

1. **Tính overlap ngày** (BR-ALC-02 nâng cao): Chỉ cộng allocation có khoảng ngày giao nhau
2. **Soft-delete Employee**: Thêm cột `active BOOLEAN DEFAULT true` thay vì xoá cứng
3. **Pagination cho Reports**: Thêm `Pageable` cho report endpoint
4. **Cache cho Report**: Dùng Spring Cache nếu data ít thay đổi
5. **API Versioning**: Hiện tại chỉ có `/api/v1`, sau này có thể mở rộng
6. **Swagger mẫu request**: Thêm `@Schema(example = "...")` cho DTO để Swagger UI đẹp hơn

---

## 9. Kết luận

| Tiêu chí | Điểm | Ghi chú |
|---|---|---|
| **Java (OOP + SOLID + Exception + Layer)** | 9.5/10 | SOLID tốt, chỉ 1-2 điểm nhỏ về exception handling |
| **Database (PK/FK + JOIN + GROUP BY + Aggregate)** | 10/10 | SQL chuẩn, có index, FK đúng |
| **Spring Boot (REST + Validation + Service + Repository)** | 9.5/10 | REST chuẩn, validation đủ, layer tách biệt |
| **Business Logic (Allocation + Workload + Status)** | 9/10 | Core rules đúng, thiếu check delete project |
| **Bonus (Unit Test + Swagger + Docker + AI)** | 9/10 | Test coverage tốt, Docker đầy đủ, AI có fallback |
| **Tổng** | **9.4/10** | **Ứng viên đạt yêu cầu Assignment** ✅ |

**Điểm mạnh:**
- Kiến trúc layer rõ ràng, đúng chuẩn Spring Boot
- Business rules implement đầy đủ và chính xác
- Test coverage tốt, đặc biệt là các case biên (edge cases)
- AI integration đúng pattern (số liệu thật → prompt → AI format)

**Điểm cần cải thiện:**
- Sửa `ProjectService.deleteProject()` (TODO còn sót)
- Sửa `pom.xml` (Spring Boot version + artifact names)
- Bắt exception bằng class cụ thể thay vì string

---

*Report generated by AI Code Review — Claude (Anthropic)*
