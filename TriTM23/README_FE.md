# Resource Allocation System — Frontend

> Giao diện người dùng cho hệ thống quản lý phân bổ nguồn lực.  
> Xây dựng bằng **React 19 + TypeScript + Vite 8**.

---

## Yêu cầu hệ thống

- **Node.js** 18+ (khuyến nghị 20+)
- **npm** (đi kèm Node.js)
- Backend BE đã chạy tại `http://localhost:8080` (xem `resource_allocation_be/README.md`)

---

## Cấu trúc thư mục

```
src/
├── pages/
│   ├── Dashboard.tsx        # Trang tổng quan (reports)
│   ├── Employees.tsx        # Quản lý nhân viên
│   ├── Projects.tsx         # Quản lý dự án
│   ├── Allocations.tsx      # Quản lý phân bổ
│   └── AiAssistant.tsx      # AI Assistant (bonus)
├── services/
│   └── api.ts               # API client gọi BE
├── assets/
├── App.tsx
└── main.tsx
```

---

## Hướng dẫn chạy

### 1. Cài đặt dependencies

```bash
npm install
```

### 2. Cấu hình API URL (nếu cần)

Mặc định FE gọi BE tại `http://localhost:8080/api/v1`.  
Nếu BE chạy cổng khác, sửa `src/services/api.ts`:

```ts
const BASE_URL = 'http://localhost:PORT/api/v1';
```

### 3. Chạy dev server

```bash
npm run dev
```

Truy cập: [http://localhost:5173](http://localhost:5173)

### Build cho production

```bash
npm run build     # output trong dist/
npm run preview   # xem thử bản build
```

---

## Kết nối với Backend

Trước khi dùng FE, cần chạy BE:

```bash
# Từ thư mục resource_allocation_be
docker compose up -d postgres    # Khởi động DB
./mvnw spring-boot:run           # Khởi động BE (cổng 8080)
```

---

## Các trang chức năng

| Route | Trang | Mô tả |
|---|---|---|
| `/` | Dashboard | Báo cáo utilization, available, overloaded |
| `/employees` | Employees | CRUD nhân viên + xem workload |
| `/projects` | Projects | CRUD dự án |
| `/allocations` | Allocations | Phân bổ nhân sự vào dự án |
| `/ai` | AI Assistant | Gợi ý tài nguyên & phát hiện rủi ro |

---

## Công nghệ sử dụng

| Công nghệ | Mục đích |
|---|---|
| React 19 | UI framework |
| TypeScript ~6.0 | Kiểm soát kiểu dữ liệu |
| Vite 8 | Build tool & dev server |
| lucide-react | Icon library |
| Oxlint | Linter |
