# Architecture

## 1. Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot |
| Data Access | Spring Data JPA |
| Database | PostgreSQL |
| Build tool | Maven |
| Mapping | MapStruct (xem `coding-conventions.md` mục 3) |
| API Doc | springdoc-openapi (Swagger) — bonus |
| Validation | Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Min`, `@Max`) |
| AOP | Spring AOP (`spring-boot-starter-aop`) |
| Test | JUnit 5 + Mockito — bonus |

---

## 2. Layered Architecture

```
Controller  →  Service  →  Repository  →  Database
    ↑              ↓
   DTO          Entity
```

- **Controller:** nhận HTTP request, validate qua `@Valid`, gọi Service, trả DTO. Không chứa business logic.
- **Service:** chứa toàn bộ business rule (đối chiếu `business-rules.md`). Nhận/trả DTO, tự chuyển đổi Entity ↔ DTO qua Mapper.
- **Repository:** interface `JpaRepository<Entity, Long>`, chỉ chứa query, không chứa logic nghiệp vụ.
- **Mapper:** convert Entity ↔ DTO, đặt cùng package với module (`core/{module}/{Module}Mapper.java`).

Chi tiết trách nhiệm từng layer và quy tắc đặt tên: xem `coding-conventions.md` mục 1-2.

---

## 3. Package Structure

```
com.company.resourceallocation
│
├── core                                  # Module nghiệp vụ chính (package-by-feature)
│   ├── employee
│   │   ├── Employee.java                 # Entity
│   │   ├── EmployeeRepository.java
│   │   ├── EmployeeService.java
│   │   ├── EmployeeController.java
│   │   ├── EmployeeMapper.java
│   │   └── dto
│   │       ├── EmployeeRequest.java
│   │       └── EmployeeResponse.java
│   │
│   ├── project
│   │   └── ... (cùng cấu trúc như employee)
│   │
│   └── allocation
│       ├── Allocation.java
│       ├── AllocationRepository.java
│       ├── AllocationService.java        # Chứa BR-ALC-01 → BR-ALC-06
│       ├── AllocationController.java
│       ├── AllocationMapper.java
│       └── dto
│           ├── AllocationRequest.java
│           └── AllocationResponse.java
│
├── report                                # Không phải CRUD module, chỉ đọc dữ liệu tổng hợp
│   ├── ReportService.java                # Gọi AllocationRepository, không có Entity riêng
│   ├── ReportController.java
│   └── dto
│       ├── UtilizationResponse.java
│       ├── AvailableResponse.java
│       └── OverloadedResponse.java
│
├── ai                                    # AI Bonus Features
│   ├── AiRecommendationService.java      # Gọi lại ReportService lấy số liệu thật trước khi gọi AI
│   ├── AiController.java
│   └── dto
│       ├── AiRecommendRequest.java
│       └── AiRiskRequest.java
│
├── exception                             # Dùng chung toàn hệ thống — cấp cha
│   ├── ErrorResponse.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── AllocationExceededException.java
│   ├── InvalidProjectStatusException.java
│   ├── InvalidAllocationPercentException.java
│   ├── EmployeeInUseException.java
│   └── GlobalExceptionHandler.java
│
├── aspect                                # AOP Logging — cấp cha
│   └── LoggingAspect.java
│
└── config
    └── OpenApiConfig.java
```

> Package `report` và `ai` không nằm trong `core` vì chúng không phải CRUD module có Entity riêng — chúng chỉ đọc/tổng hợp dữ liệu từ các module khác. Đặt ngang hàng với `core` giúp rõ ràng đây là 2 nhóm chức năng khác bản chất (query-only / AI-integration).

---

## 4. Exception Handling Flow

```
Service ném Exception
        │
        ▼
GlobalExceptionHandler (@RestControllerAdvice)
        │
        ▼
Convert sang ErrorResponse + đúng HTTP status
        │
        ▼
Trả về Client
```

**`ErrorResponse.java`**
```java
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;      // VD: "Bad Request", "Not Found"
    private String message;
    private String path;
}
```

**`GlobalExceptionHandler.java` — cấu trúc mẫu**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(AllocationExceededException.class)
    public ResponseEntity<ErrorResponse> handleAllocationExceeded(AllocationExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ... 1 handler riêng cho mỗi exception trong bảng ở business-rules.md mục 4

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
```

Danh sách đầy đủ exception ↔ status ↔ khi nào ném: xem `business-rules.md` mục 4.

---

## 5. AOP Logging Flow

```
Client request
      │
      ▼
Controller → Service.create()/update()/delete()
                    │
        ┌───────────┴───────────┐
        │  LoggingAspect (@Around)│
        │  - log trước khi chạy   │
        │  - log sau khi chạy     │
        │  - log nếu exception    │
        └───────────┬───────────┘
                    ▼
              Method thực thi
```

**`LoggingAspect.java` — cấu trúc mẫu**
```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.company.resourceallocation.core..*Service.create*(..)) || " +
            "execution(* com.company.resourceallocation.core..*Service.update*(..)) || " +
            "execution(* com.company.resourceallocation.core..*Service.delete*(..))")
    public Object logAction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            log.info("[{}] executed successfully", methodName);
            return result;
        } catch (Exception ex) {
            log.error("[{}] failed: {}", methodName, ex.getMessage());
            throw ex;
        }
    }
}
```

> Pointcut chỉ nhắm vào method `create*/update*/delete*` ở tầng Service trong `core` package, không log GET (đọc dữ liệu không cần audit theo yêu cầu spec gốc — mục 7 "Logging": chỉ liệt kê Create/Update/Remove Allocation).
> Format log chi tiết: xem `coding-conventions.md` mục 7.

---

## 6. Request Flow — ví dụ minh hoạ (Create Allocation)

```
POST /api/v1/allocations
      │
      ▼
AllocationController.create(@Valid AllocationRequest)
      │  (Bean Validation: allocationPercent 1-100, employeeId/projectId NotNull)
      ▼
AllocationService.create(AllocationRequest)
      │
      ├─ 1. Tìm Employee theo employeeId → nếu không có: ResourceNotFoundException
      ├─ 2. Tìm Project theo projectId → nếu không có: ResourceNotFoundException
      ├─ 3. Check Project.status != COMPLETED → nếu COMPLETED: InvalidProjectStatusException
      ├─ 4. Tính tổng allocation hiện tại của Employee (AllocationRepository)
      ├─ 5. Check tổng + allocationPercent mới <= 100 → nếu vượt: AllocationExceededException
      ├─ 6. Map DTO → Entity, save qua AllocationRepository
      └─ 7. Map Entity → AllocationResponse, return
      │
      ▼ (LoggingAspect log lại action CREATE)
Controller trả 201 Created + AllocationResponse
```

---

## 7. Configuration Notes

- `OpenApiConfig.java`: cấu hình title/description cho Swagger UI, expose tại `/swagger-ui.html`.
- `application.yml`: tách 2 profile `dev` (PostgreSQL local) và `test` (H2 in-memory, nếu viết unit test có dùng DB thật — không bắt buộc vì Service test dùng Mockito).
- Không cần Spring Security trong scope fresher — bỏ qua auth/authorization.

---

## 8. Liên kết tài liệu khác

| File | Nội dung |
|---|---|
| `database-schema.md` | Bảng, FK, index |
| `business-rules.md` | Rule nghiệp vụ + exception tương ứng |
| `api-specs.md` | Chi tiết từng endpoint + DTO |
| `coding-conventions.md` | Naming, layer responsibility, format response |
| `phase.md` | Lộ trình implement |
