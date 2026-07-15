# Coding Conventions

Áp dụng thống nhất cho toàn bộ 3 module (employee, project, allocation) để AI generate code không bị lệch phong cách giữa các lần chat.

---

## 1. Package & Class Naming

Theo kiến trúc đã chốt (package-by-feature cho core, package-by-layer cho shared):

```
com.company.resourceallocation
├── core
│   ├── employee
│   │   ├── Employee.java
│   │   ├── EmployeeRepository.java
│   │   ├── EmployeeService.java
│   │   ├── EmployeeController.java
│   │   ├── EmployeeMapper.java
│   │   └── dto
│   │       ├── EmployeeRequest.java
│   │       └── EmployeeResponse.java
│   ├── project
│   │   └── ... (cùng cấu trúc)
│   └── allocation
│       └── ... (cùng cấu trúc)
├── report
│   ├── ReportService.java
│   ├── ReportController.java
│   └── dto
│       ├── UtilizationResponse.java
│       ├── AvailableResponse.java
│       └── OverloadedResponse.java
├── ai
│   ├── AiRecommendationService.java
│   ├── AiController.java
│   └── dto
├── exception
├── aspect
└── config
```

> DTO đặt trong package con `dto` của từng module (không gộp chung 1 package `dto` ở cấp cha) — giữ nguyên tắc "package-by-feature", tránh 1 package `dto` phình to chứa DTO của mọi module không liên quan nhau.

**Naming rule:**

| Loại | Convention | Ví dụ |
|---|---|---|
| Entity | Danh từ số ít, PascalCase | `Employee`, `Allocation` |
| Repository | `{Entity}Repository` | `EmployeeRepository` |
| Service interface | `{Entity}Service` | `AllocationService` |
| Service impl | `{Entity}ServiceImpl` (chỉ khi có interface riêng) | `AllocationServiceImpl` |
| Controller | `{Entity}Controller` | `ProjectController` |
| Request DTO | `{Entity}Request` | `EmployeeRequest` |
| Response DTO | `{Entity}Response` | `EmployeeResponse` |
| Mapper | `{Entity}Mapper` | `AllocationMapper` |
| Exception | `{Lý do}Exception`, hậu tố bắt buộc `Exception` | `AllocationExceededException` |

---

## 2. Layer Responsibility (tách rõ để AI không đặt sai logic vào sai tầng)

- **Controller:** chỉ nhận request, gọi Service, trả response. Không chứa business logic, không query trực tiếp Repository.
- **Service:** chứa toàn bộ business rule (xem `business-rules.md`). Service nhận/trả **DTO**, không để lộ Entity ra ngoài tầng Controller.
- **Repository:** chỉ chứa query (Spring Data JPA method hoặc `@Query`), không chứa business logic.
- **Mapper:** convert Entity ↔ DTO. Không đặt business logic trong Mapper.

---

## 3. DTO & Mapping Convention

- Dùng **MapStruct** cho mapping Entity ↔ DTO (giảm boilerplate, tránh lỗi map tay sai field). Nếu team chưa quen MapStruct, fallback: viết method mapping tay trong class `{Entity}Mapper` (POJO thường, không phải MapStruct interface) — miễn nhất quán 1 cách xuyên suốt dự án, không trộn lẫn 2 kiểu.
- **Không** dùng chung 1 DTO cho cả Request và Response (kể cả khi field giống hệt nhau) — tách riêng để dễ mở rộng sau này (VD Response cần thêm `createdAt`, Request thì không).
- Field trong DTO dùng **camelCase**, kể cả khi cột DB là `snake_case` (JPA tự mapping qua `@Column(name = "...")` hoặc naming strategy mặc định).
- Validation annotation (`@NotBlank`, `@Email`, `@Min`, `@Max`) đặt ở **Request DTO**, không đặt ở Entity.

---

## 4. Response Format

- **Không** bọc response thành công trong wrapper `ApiResponse<T>` chung (VD `{ "success": true, "data": {...} }`) — trả thẳng DTO. Lý do: giữ đơn giản đúng scope fresher, tránh phải sửa lại toàn bộ Controller nếu đổi ý sau.
- Response lỗi thì luôn theo `ErrorResponse` chuẩn (xem `business-rules.md` mục 4):
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/api/v1/allocations"
}
```
- List trả về nhiều item (không phân trang) → trả thẳng JSON array `[...]`.
- List có phân trang → trả `Page<T>` mặc định của Spring Data (dùng `Pageable` trong Controller param), không tự viết wrapper phân trang riêng.

---

## 5. HTTP Status Code Convention

| Action | Status thành công |
|---|---|
| Create (POST) | `201 Created` |
| Read (GET) | `200 OK` |
| Update (PUT) | `200 OK` |
| Delete (DELETE) | `204 No Content` |

Status lỗi: theo bảng Exception ở `business-rules.md` mục 4.

---

## 6. Exception Handling

- Toàn bộ custom exception kế thừa `RuntimeException`, đặt trong package `exception` cấp cha (đã chốt ở `architecture.md`).
- `GlobalExceptionHandler` dùng `@RestControllerAdvice`, mỗi exception có 1 `@ExceptionHandler` riêng, map sang đúng HTTP status theo bảng ở `business-rules.md`.
- Không dùng `try-catch` bắt exception trong Controller/Service để tự trả response lỗi — luôn ném exception ra để `GlobalExceptionHandler` xử lý tập trung.

---

## 7. Logging (AOP)

- `LoggingAspect` áp dụng `@Around` cho toàn bộ method `create/update/delete` ở tầng Service (pointcut theo package `core..*Service`), không log ở tầng Controller hay Repository.
- Log format thống nhất: `[ACTION] {EntityName} - id={id} - by={caller nếu có auth}` — VD: `[CREATE] Allocation - id=10`.
- Không log toàn bộ Request/Response body (tránh lộ dữ liệu nhạy cảm như email) — chỉ log id và action.

---

## 8. Validation

- Dùng Bean Validation annotation chuẩn ở Request DTO: `@NotBlank`, `@Email`, `@Min`, `@Max`, `@NotNull`.
- Business rule không thể validate bằng annotation (VD: tổng allocation ≤ 100%, project không COMPLETED) → validate trong Service, ném custom exception tương ứng.
- Controller thêm `@Valid` trước `@RequestBody` để trigger validation annotation tự động.

---

## 9. Testing (bonus)

- Unit test tập trung vào **Service layer** (nơi chứa business logic) — ưu tiên test các case trong `business-rules.md`, đặc biệt BR-ALC-02 (Case A và Case B).
- Naming test method: `should_{expectedResult}_when_{condition}` — VD: `should_throwException_when_totalAllocationExceeds100()`.
- Dùng Mockito để mock Repository trong Service test, không cần Testcontainers/DB thật (ngoài scope fresher).

---

## 10. Swagger (bonus)

- Dùng `springdoc-openapi`, cấu hình ở `config/OpenApiConfig.java`.
- Mỗi Controller method thêm `@Operation(summary = "...")` ngắn gọn mô tả đúng theo `api-specs.md`.
