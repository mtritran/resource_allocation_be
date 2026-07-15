# Business Rules

Mỗi rule bên dưới đi kèm: điều kiện, exception tương ứng, HTTP status, message mẫu.
`GlobalExceptionHandler` bắt các exception này và convert sang `ErrorResponse` (format chung xem ở `architecture.md`).

---

## 1. Employee

### BR-EMP-01: Employee code / email không được trùng
- Điều kiện: `employee_code` và `email` là UNIQUE ở DB.
- Exception: `DuplicateResourceException`
- HTTP: `409 Conflict`
- Message: `"Employee code {code} already exists"` / `"Email {email} already exists"`

### BR-EMP-02: Không tìm thấy employee
- Exception: `ResourceNotFoundException("Employee", id)`
- HTTP: `404 Not Found`
- Message: `"Employee not found with id: {id}"`

### BR-EMP-03: Xoá employee
- Điều kiện: Không cho xoá cứng nếu employee còn allocation đang active (chưa hết `end_date` hoặc `end_date` null).
- Exception: `EmployeeInUseException`
- HTTP: `409 Conflict`
- Message: `"Cannot delete employee: still has active allocations"`
- Đề xuất: Nếu cần "xoá" employee đã nghỉ việc, dùng soft-delete (thêm cột `active BOOLEAN DEFAULT true`), không xoá cứng. *(cần chốt lại nếu scope yêu cầu xoá cứng thật)*

---

## 2. Project

### BR-PRJ-01: Project code không được trùng
- Exception: `DuplicateResourceException`
- HTTP: `409 Conflict`
- Message: `"Project code {code} already exists"`

### BR-PRJ-02: Không tìm thấy project
- Exception: `ResourceNotFoundException("Project", id)`
- HTTP: `404 Not Found`
- Message: `"Project not found with id: {id}"`

### BR-PRJ-03: Không cho allocate vào project COMPLETED
- Điều kiện: Khi tạo/update allocation, nếu `project.status == COMPLETED` → reject.
- Áp dụng cho cả **create** và **update** allocation (kể cả đổi allocation sang project khác đang COMPLETED).
- Exception: `InvalidProjectStatusException`
- HTTP: `400 Bad Request`
- Message: `"Cannot allocate to a COMPLETED project"`

### BR-PRJ-04: Chuyển status COMPLETED
- Câu hỏi mở: Khi PM đổi status project từ ACTIVE → COMPLETED, các allocation đang active của project đó xử lý sao?
  - Option A (đề xuất): Cho phép đổi status tự do, chỉ chặn việc **tạo mới allocation** vào project COMPLETED (allocation cũ giữ nguyên, coi như lịch sử).
  - Option B: Chặn đổi sang COMPLETED nếu còn allocation chưa hết `end_date`.
  - → **Chọn Option A** cho đơn giản, ghi rõ trong code comment.

---

## 3. Allocation — Rule quan trọng nhất

### BR-ALC-01: Allocation percent hợp lệ
- Điều kiện: `0 < allocation_percent <= 100`
- Exception: `InvalidAllocationPercentException` (hoặc dùng `@Min(1) @Max(100)` ở DTO → `MethodArgumentNotValidException` mặc định của Spring)
- HTTP: `400 Bad Request`
- Message: `"Allocation percent must be between 1 and 100"`

### BR-ALC-02: Tổng allocation của 1 employee không vượt 100%
- Đây là rule trung tâm của cả hệ thống, cần làm rõ 2 case:

**Case A — Create allocation mới:**
```
newTotal = SUM(allocation_percent của các allocation hiện có của employee) + allocationPercent (mới)
if newTotal > 100 → reject
```

**Case B — Update allocation đã tồn tại (allocation_id = X):**
```
newTotal = SUM(allocation_percent của employee, LOẠI TRỪ allocation_id = X) + allocationPercent (giá trị mới)
if newTotal > 100 → reject
```
⚠️ Lỗi thường gặp: quên loại trừ record đang update ra khỏi tổng, dẫn tới không thể update giảm % (VD employee đang 100%, muốn sửa từ 60% xuống 50%, nếu không loại trừ sẽ tính thành 100+50=150% → reject sai).

**Có tính theo overlap ngày không?**
- Đề xuất scope fresher: **KHÔNG** tính theo overlap `start_date`/`end_date`, coi mọi allocation của employee là cộng dồn toàn thời gian (đơn giản, đúng với ví dụ trong spec gốc).
- Nếu muốn nâng cao (bonus): chỉ cộng các allocation có khoảng `[start_date, end_date]` giao nhau với allocation đang xét. Ghi chú lại như 1 improvement, không bắt buộc.

- Exception: `AllocationExceededException`
- HTTP: `400 Bad Request`
- Message: `"Employee allocation exceeds 100%"` (đúng theo spec gốc)

### BR-ALC-03: Không allocate vào project COMPLETED
- Xem BR-PRJ-03.

### BR-ALC-04: Employee/Project phải tồn tại
- Nếu `employeeId` hoặc `projectId` trong request không tồn tại:
- Exception: `ResourceNotFoundException("Employee", employeeId)` hoặc `ResourceNotFoundException("Project", projectId)`
- HTTP: `404 Not Found`

### BR-ALC-05: Xoá allocation
- Xoá cứng (hard delete), không cần soft-delete vì allocation bản chất là dữ liệu điều phối theo thời gian, không phải master data.
- Không có ràng buộc chặn xoá — xoá allocation luôn được phép.
- Log lại hành động xoá qua `LoggingAspect` (xem `architecture.md`) để phục vụ audit thay vì soft-delete.

### BR-ALC-06: start_date / end_date của allocation
- `end_date >= start_date` nếu cả 2 đều có giá trị (đã có CHECK constraint ở DB).
- Không bắt buộc phải nằm trong khoảng `start_date`/`end_date` của project (không có rule nào yêu cầu điều này trong spec gốc) — nhưng nên validate ở tầng Service với warning/log nếu allocation nằm ngoài range project, không reject cứng.

---

## 4. Bảng tổng hợp Exception (dùng cho `GlobalExceptionHandler`)

| Exception | HTTP Status | Khi nào ném |
|---|---|---|
| `ResourceNotFoundException` | 404 | Employee/Project/Allocation id không tồn tại |
| `DuplicateResourceException` | 409 | employee_code / email / project_code trùng |
| `InvalidAllocationPercentException` | 400 | allocation_percent ngoài khoảng (1-100) — có thể thay bằng validation annotation |
| `AllocationExceededException` | 400 | Tổng allocation của employee > 100% |
| `InvalidProjectStatusException` | 400 | Allocate vào project COMPLETED |
| `EmployeeInUseException` | 409 | Xoá employee còn allocation active |
| `MethodArgumentNotValidException` (Spring built-in) | 400 | Lỗi validate DTO (@NotBlank, @Email...) |

**Format `ErrorResponse` chuẩn** (chi tiết field xem `architecture.md`):
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Employee allocation exceeds 100%",
  "path": "/allocations"
}
```

---

## 5. Reporting — điều kiện lọc (nhắc lại để nhất quán khi code Repository)

| Report | Điều kiện |
|---|---|
| Employee Utilization | `SUM(allocation_percent) GROUP BY employee_id`, không lọc |
| Available Resource | `SUM(allocation_percent) < 100`, trả kèm `available = 100 - total` |
| Overloaded Employee | `SUM(allocation_percent) > 90` |

> Cả 3 report đều nên tính trên **allocation hiện tại** — nếu sau này áp dụng overlap theo ngày (BR-ALC-02 phần nâng cao) thì 3 report này cũng phải đồng bộ theo cùng logic, tránh vênh số liệu giữa report và validation lúc tạo allocation.
