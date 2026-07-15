# AI Rules — Hướng dẫn AI khi làm việc trên dự án này

File này là **entry point** — AI phải đọc file này đầu tiên trước khi đọc bất kỳ file nào khác hoặc viết bất kỳ dòng code nào.

---

## 1. Danh sách tài liệu & thứ tự đọc bắt buộc

Khi được giao 1 task (VD: "code module Allocation", "viết Controller cho Project"), AI phải đọc theo đúng thứ tự sau, **không được bỏ qua bước nào**:

| Thứ tự | File | Đọc để làm gì |
|---|---|---|
| 1 | `AI-Rules.md` | File này — quy tắc hành xử chung |
| 2 | `phase.md` | Xác định task đang thuộc Phase nào, đã đủ điều kiện dependency để làm chưa |
| 3 | `architecture.md` | Xác định đúng package, đúng layer, đúng vị trí đặt file |
| 4 | `database-schema.md` | Xác định đúng tên bảng/cột, kiểu dữ liệu, FK, index |
| 5 | `business-rules.md` | Xác định đúng rule nghiệp vụ + exception + status code phải ném ra |
| 6 | `api-specs.md` | Xác định đúng endpoint, request/response DTO, field, error case |
| 7 | `coding-conventions.md` | Xác định đúng naming, format response, cách viết code |
| 8 | `testing-checklist.md` | Xác định các case bắt buộc phải test/pass trước khi báo hoàn thành task |

> Nếu task chỉ liên quan 1 module cụ thể (VD: chỉ sửa Employee), vẫn phải đọc đủ cả 6 file — vì `business-rules.md` và `api-specs.md` có thể có phần liên quan chéo module (VD: BR-EMP-03 phụ thuộc Allocation).

---

## 2. Nguyên tắc ưu tiên khi có mâu thuẫn giữa các file

Nếu phát hiện 2 file mô tả khác nhau về cùng 1 vấn đề, ưu tiên theo thứ tự:

```
business-rules.md  >  api-specs.md  >  architecture.md  >  coding-conventions.md  >  database-schema.md
```

Lý do: `business-rules.md` chứa quyết định nghiệp vụ gốc (source of truth), các file còn lại chỉ là cách hiện thực hoá rule đó. Nếu phát hiện mâu thuẫn, **AI phải dừng lại và hỏi lại người dùng**, không tự ý chọn 1 bên rồi code tiếp.

---

## 3. Quy tắc bắt buộc khi sinh code

1. **Không tự bịa field, endpoint, hoặc rule không có trong tài liệu.** Nếu task yêu cầu thứ chưa được định nghĩa (VD: thêm 1 field mới vào Entity), AI phải hỏi lại trước, không tự quyết định rồi code luôn.
2. **Không tự đổi tên đã chốt.** Tên class, package, field, endpoint trong tài liệu là chuẩn cuối — không "cải tiến" hay đổi theo style khác dù thấy hợp lý hơn.
3. **Luôn bám theo `phase.md` để xác định thứ tự.** Không code Allocation trước khi Employee + Project đã có Entity/Repository (xem dependency graph trong `phase.md` mục cuối).
4. **Mọi exception ném ra phải khớp bảng ở `business-rules.md` mục 4** — không tự tạo exception mới nếu đã có exception phù hợp trong bảng.
5. **Mọi response phải khớp DTO đã định nghĩa ở `api-specs.md`** — không thêm/bớt field tuỳ tiện.
6. **Không tự thêm dependency/thư viện mới** (VD: đổi từ MapStruct sang thư viện khác) nếu chưa có trong `architecture.md` mục 1 (Tech Stack). Nếu thấy cần, đề xuất trước, không tự ý thêm vào `pom.xml` rồi code theo.
7. **Giữ nguyên convention response/error format** ở `coding-conventions.md` mục 4 — không bọc `ApiResponse<T>` trừ khi tài liệu được cập nhật lại.

---

## 4. Khi tài liệu chưa đủ thông tin (gap)

Một số quyết định trong các file đã được đánh dấu là **đề xuất mặc định**, không phải quyết định cuối cùng tuyệt đối (VD: soft-delete employee, Option A cho BR-PRJ-04, không tính overlap ngày ở BR-ALC-02). Khi AI gặp các mục này:

- Nếu đã có dòng "**Đề xuất:**" hoặc "**Chọn Option ...**" trong tài liệu → coi đó là quyết định đã chốt, cứ theo đó code, không hỏi lại.
- Nếu gặp case hoàn toàn chưa được nhắc tới ở bất kỳ file nào → **dừng lại, liệt kê rõ câu hỏi, hỏi người dùng trước khi code**, không tự suy đoán rồi generate.

---

## 5. Khi code xong 1 phần — BẮT BUỘC TEST TRƯỚC KHI BÁO DONE

- Sau khi code xong 1 module/endpoint, AI **phải** chạy qua toàn bộ test case tương ứng trong `testing-checklist.md` (unit test nếu case đánh dấu **[UNIT]**, hoặc mô tả lại bước test thủ công nếu đánh dấu **[MANUAL]**) — không được báo "hoàn thành" khi chưa verify.
- Đối chiếu với bảng "Definition of Done theo từng Phase" ở `testing-checklist.md` mục 7 để biết Phase hiện tại cần pass đủ case nào.
- Ưu tiên đặc biệt: case A10 (update giảm % phải loại trừ record đang sửa) và case R4 (biên overloaded dùng `>` chứ không phải `>=`) — đây là 2 lỗi hay gặp nhất, phải test riêng, không gộp chung chung.
- Nếu có case fail → sửa code ngay, không được note "để sau" mà không báo người dùng.
- Nếu phát hiện lúc code thực tế có case mà tài liệu chưa tính tới, **báo lại cho người dùng để cập nhật tài liệu tương ứng** (thường là `business-rules.md`, `api-specs.md`, hoặc bổ sung case mới vào `testing-checklist.md`), không tự âm thầm sửa logic khác với tài liệu mà không thông báo.

---

## 6. Việc AI KHÔNG được tự ý làm

- Không tự đổi cấu trúc package đã chốt ở `architecture.md` mục 3.
- Không tự bỏ qua bước validate DTO (`@Valid`) dù thấy "không cần thiết" cho 1 case cụ thể.
- Không tự gộp Request/Response DTO thành 1 dù field giống nhau (đã chốt ở `coding-conventions.md` mục 3).
- Không tự thêm Spring Security / auth nếu không được yêu cầu (ngoài scope theo `architecture.md` mục 7).
- Không tự viết business logic trong Controller hoặc Repository (vi phạm `coding-conventions.md` mục 2).

---

## 7. Tóm tắt nhanh (checklist trước khi bắt đầu 1 task code)

```
[ ] Đã đọc đủ 6 file theo đúng thứ tự ở mục 1 chưa?
[ ] Task này đúng Phase hiện tại theo phase.md chưa, dependency đã sẵn sàng chưa?
[ ] Đã xác định đúng package/vị trí file theo architecture.md chưa?
[ ] Đã map đúng field DTO theo api-specs.md chưa?
[ ] Đã xác định đúng exception/status code cần ném theo business-rules.md chưa?
[ ] Có phần nào chưa rõ / chưa có trong tài liệu không? Nếu có → hỏi trước khi code.
[ ] Sau khi code xong: đã chạy đủ test case liên quan trong testing-checklist.md chưa?
```
