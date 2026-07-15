# Testing Checklist

File này chứa test case cụ thể (input → expected output) cho từng rule/endpoint, dùng để AI (hoặc người) tự verify sau khi code xong 1 module — trước khi báo "done" ở bất kỳ Phase nào trong `phase.md`.

Quy ước ký hiệu: **[UNIT]** = nên viết unit test (Service layer, mock Repository) — **[MANUAL]** = test thủ công qua Postman/integration test, cần DB thật.

---

## 1. Employee

| # | Case | Input | Expected | Loại |
|---|---|---|---|---|
| E1 | Create hợp lệ | employeeCode=EMP001, email hợp lệ, đủ field required | `201`, trả đúng field đã gửi + `employeeId` sinh tự động | [MANUAL] |
| E2 | Create thiếu field required | `fullName` = "" | `400`, message chỉ rõ field lỗi | [UNIT] |
| E3 | Create email sai format | email="abc" | `400` (`@Email` fail) | [UNIT] |
| E4 | Create trùng employeeCode | employeeCode đã tồn tại | `409`, message `"Employee code {code} already exists"` (BR-EMP-01) | [UNIT] |
| E5 | Create trùng email | email đã tồn tại (code khác) | `409`, message `"Email {email} already exists"` | [UNIT] |
| E6 | Get by id không tồn tại | id=9999 | `404`, message `"Employee not found with id: 9999"` (BR-EMP-02) | [UNIT] |
| E7 | Update thành công | đổi `department` | `200`, field mới được cập nhật | [MANUAL] |
| E8 | Update email trùng với employee khác | email của record khác | `409` | [UNIT] |
| E9 | Delete employee không có allocation | employee mới tạo, chưa allocate | `204` | [MANUAL] |
| E10 | Delete employee còn allocation active | employee đang có allocation | `409`, message `"Cannot delete employee: still has active allocations"` (BR-EMP-03) — **phải test sau khi có module Allocation (Phase 4)** | [UNIT] |

---

## 2. Project

| # | Case | Input | Expected | Loại |
|---|---|---|---|---|
| P1 | Create hợp lệ, không truyền status | projectCode=NCG, không có `status` | `201`, `status` mặc định = `PLANNING` | [MANUAL] |
| P2 | Create trùng projectCode | projectCode đã tồn tại | `409` (BR-PRJ-01) | [UNIT] |
| P3 | Create endDate < startDate | startDate=2025-06-01, endDate=2025-01-01 | `400` | [UNIT] |
| P4 | Create status không hợp lệ | status="DONE" (không nằm trong enum) | `400` | [UNIT] |
| P5 | Get by id không tồn tại | id=9999 | `404` (BR-PRJ-02) | [UNIT] |
| P6 | Update status ACTIVE → COMPLETED dù còn allocation active | project đang có allocation active | `200`, cho phép đổi (Option A, BR-PRJ-04) — **không** reject | [UNIT] |
| P7 | Delete project không có allocation | project mới tạo | `204` | [MANUAL] |

---

## 3. Allocation — nhóm quan trọng nhất, test kỹ

### 3.1 Validate percent (BR-ALC-01)

| # | Case | Input | Expected |
|---|---|---|---|
| A1 | percent = 0 | allocationPercent=0 | `400` |
| A2 | percent = 1 | allocationPercent=1 | `201` (biên dưới hợp lệ) |
| A3 | percent = 100 | allocationPercent=100 | `201` (biên trên hợp lệ) |
| A4 | percent = 101 | allocationPercent=101 | `400` |
| A5 | percent âm | allocationPercent=-10 | `400` |

### 3.2 Tổng allocation ≤ 100% — Create (BR-ALC-02 Case A)

| # | Case | Setup | Input | Expected |
|---|---|---|---|---|
| A6 | Tổng đúng 100% | Employee chưa có allocation nào | Tạo allocation 100% | `201` |
| A7 | Tổng đúng 100% (nhiều allocation) | Employee đã có 1 allocation 60% | Tạo thêm allocation 40% | `201`, tổng = 100% |
| A8 | Tổng vượt 100% | Employee đã có allocation 60% | Tạo thêm allocation 50% | `400`, message `"Employee allocation exceeds 100%"` (BR-ALC-02), tổng lẽ ra = 110% |
| A9 | Employee đã 100%, tạo thêm bất kỳ % nào | Employee đã có allocation 100% | Tạo thêm allocation 1% | `400` |

### 3.3 Tổng allocation ≤ 100% — Update (BR-ALC-02 Case B) — **case hay bị code sai nhất**

| # | Case | Setup | Input | Expected |
|---|---|---|---|---|
| A10 | Update giảm % | Employee đang 100% (allocation X=60%, Y=40%) | Update X từ 60% → 50% | `200`, **PHẢI pass** vì tổng mới = 50+40=90%. Nếu code quên loại trừ record X khi tính tổng cũ, sẽ tính sai thành 60+50+40=150% → reject sai → **đây là bug cần test bắt được** |
| A11 | Update giữ nguyên % | allocation X=60%, không đổi percent, chỉ đổi `roleInProject` | Update X, percent vẫn 60% | `200`, pass |
| A12 | Update tăng % vượt ngưỡng | Employee đang: X=60%, Y=40% (đã 100%) | Update X từ 60% → 70% | `400`, vì tổng mới (loại X ra) = 40 + 70 = 110% > 100% |
| A13 | Update tăng % vẫn trong ngưỡng | Employee đang: X=50%, Y=30% (tổng 80%) | Update X từ 50% → 60% | `200`, tổng mới = 30+60=90%, pass |

### 3.4 Project status (BR-ALC-03 / BR-PRJ-03)

| # | Case | Setup | Input | Expected |
|---|---|---|---|---|
| A14 | Allocate vào project COMPLETED | Project status=COMPLETED | Tạo allocation vào project đó | `400`, message `"Cannot allocate to a COMPLETED project"` |
| A15 | Allocate vào project PLANNING | Project status=PLANNING | Tạo allocation | `201`, cho phép |
| A16 | Allocate vào project ACTIVE | Project status=ACTIVE | Tạo allocation | `201`, cho phép |
| A17 | Update allocation sang project khác đang COMPLETED | Đổi `projectId` của allocation hiện có sang project COMPLETED | Update | `400` (áp dụng cả create lẫn update theo BR-PRJ-03) |

### 3.5 Employee/Project tồn tại (BR-ALC-04)

| # | Case | Input | Expected |
|---|---|---|---|
| A18 | employeeId không tồn tại | employeeId=9999 | `404` |
| A19 | projectId không tồn tại | projectId=9999 | `404` |

### 3.6 Delete (BR-ALC-05)

| # | Case | Input | Expected |
|---|---|---|---|
| A20 | Delete allocation bất kỳ | allocationId hợp lệ | `204`, xoá tự do không điều kiện |
| A21 | Delete allocationId không tồn tại | allocationId=9999 | `404` |

### 3.7 Dates (BR-ALC-06)

| # | Case | Input | Expected |
|---|---|---|---|
| A22 | endDate < startDate | startDate=2025-06-01, endDate=2025-01-01 | `400` |
| A23 | Không truyền startDate/endDate | cả 2 = null | `201`, cho phép (optional) |

---

## 4. Employee Workload

| # | Case | Setup | Expected |
|---|---|---|---|
| W1 | Employee có nhiều allocation | X=60% (project NCG), Y=20% (project GRID) | `totalAllocation=80`, `available=20`, `allocations` có 2 dòng đúng breakdown |
| W2 | Employee chưa có allocation nào | — | `totalAllocation=0`, `available=100`, `allocations=[]` |

---

## 5. Reports

| # | Report | Setup | Expected |
|---|---|---|---|
| R1 | Utilization | 3 employee: A=100%, B=80%, C=40% | Trả đủ 3 dòng đúng số |
| R2 | Available | A=100%, B=80% | Trả B (available=20%), **không** trả A (available=0, không < 100) |
| R3 | Available với `minAvailable=50` | A available=20%, B available=60% | Chỉ trả B |
| R4 | Overloaded (biên = 90) | Employee X=90%, Y=91% | X **không** xuất hiện (90 không > 90), Y xuất hiện (91 > 90) — test đúng biên `>` chứ không phải `>=` (BR mục 5) |
| R5 | Overloaded = 100% | Employee=100% | Xuất hiện trong overloaded |

---

## 6. AI Bonus (sanity check, không cần chính xác tuyệt đối vì phụ thuộc AI model)

| # | Case | Expected |
|---|---|---|
| AI1 | `/ai/recommend` với query "Tìm Java Developer còn tối thiểu 50% available" | Response chứa `recommendedResources`, số liệu `available` trong response phải khớp với data thật lấy từ `/reports/available?minAvailable=50` tại thời điểm gọi — **không được lệch số** |
| AI2 | `/ai/risk-detection` với query mẫu trong spec gốc | Response chứa `risks`, số % nhắc tới trong câu trả lời phải khớp với `/reports/utilization` và `/reports/overloaded` thật, không tự bịa số |

---

## 7. Definition of Done theo từng Phase (đối chiếu với `phase.md`)

| Phase | Bắt buộc pass hết case |
|---|---|
| Phase 2 (Employee) | E1 → E9 (E10 hoãn tới Phase 4) |
| Phase 3 (Project) | P1 → P7 |
| Phase 4 (Allocation) | A1 → A23 **+ quay lại chạy E10** |
| Phase 5 (Workload) | W1, W2 |
| Phase 6 (Report) | R1 → R5 |
| Phase 7 (AI Bonus) | AI1, AI2 |

> AI không được báo "Phase X hoàn thành" nếu chưa chạy qua đủ các case tương ứng trong bảng này. Nếu case nào fail, phải sửa code trước, không được bỏ qua hoặc note "để sau" mà không báo lại người dùng.
