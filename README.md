# Resource Allocation System (Backend)

> Hệ thống quản lý phân bổ nhân sự cho công ty outsourcing.  
> Xây dựng bằng **Spring Boot 3.x** (Java 17), **PostgreSQL**, **Flyway**, **Docker Compose**.

---

## Yêu cầu hệ thống (Prerequisites)

* **Java 17+** (JDK 17 trở lên)
* **Docker Desktop** (để chạy PostgreSQL & pgAdmin)
* **Git** (quản lý phiên bản)
* **Node.js 18+** (nếu chạy cả FE — xem `resource_allocation_fe/README.md`)

---

## Hướng dẫn Thiết lập & Chạy dự án

### Cách 1: Chạy toàn bộ stack bằng Docker Compose
```bash
docker compose up --build -d
```
Sau đó:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- pgAdmin: http://localhost:5050

### Cách 2: Chạy backend local (không dùng container app)
```bash
docker compose up -d postgres pgadmin
./mvnw.cmd spring-boot:run
```

### Bước 1: Chuẩn bị file biến môi trường (`.env`)
1. Từ thư mục gốc của dự án, sao chép file cấu hình mẫu `.env.example` thành `.env`:
   ```bash
   cp .env.example .env
   ```
2. Mở file `.env` vừa tạo và chỉnh sửa cấu hình (cổng kết nối, tài khoản cơ sở dữ liệu) nếu muốn chạy cấu hình tùy chỉnh dưới máy của bạn.

---

### Bước 2: Khởi động Database & pgAdmin (Docker)
1. Để chạy PostgreSQL và công cụ quản lý pgAdmin, mở terminal tại thư mục gốc chứa file `docker-compose.yml` và chạy lệnh:
   ```bash
   docker compose up -d
   ```
   *Lưu ý: Lệnh này sẽ khởi động cơ sở dữ liệu ngầm ở cổng mặc định `5432` và công cụ pgAdmin ở cổng `5050`.*

2. Truy cập vào giao diện quản trị pgAdmin và đăng ký kết nối cơ sở dữ liệu:
   * **URL truy cập:** [http://localhost:5050](http://localhost:5050)
   * **Thông tin đăng nhập pgAdmin:**
     * **Email:** `admin@company.com` (hoặc email cấu hình trong `.env`)
     * **Password:** `admin123` (hoặc password cấu hình trong `.env`)
   * **Cách tạo kết nối tới Database (Register Server):**
     * Click chuột phải vào **Servers** -> **Register** -> **Server...**
     * Tại tab **General**: Điền tên tùy chọn vào ô **Name** (Ví dụ: `Resource Allocation DB`).
     * Tại tab **Connection**: Điền các thông tin kết nối sau:
       * **Host name/address:** `postgres` (Tên service/container chạy trong cùng mạng Docker)
       * **Port:** `5432`
       * **Maintenance database:** `resource_allocation` (hoặc DB Name cấu hình trong `.env`)
       * **Username:** `dev` (hoặc User cấu hình trong `.env`)
       * **Password:** `dev123` (hoặc Password cấu hình trong `.env`)
     * Click **Save** để kết nối và xem cấu trúc bảng.

---

### Bước 3: Khởi chạy ứng dụng Spring Boot

#### Trường hợp 1: Chạy trực tiếp (Sử dụng giá trị mặc định)
Nếu bạn giữ nguyên cấu hình mặc định trong file `.env` (Postgres User: `dev`, Pass: `dev123`, DB: `resource_allocation`), bạn chỉ cần chạy trực tiếp:

* **Trên Windows (cmd/PowerShell):**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
* **Trên Linux / macOS:**
  ```bash
  ./mvnw spring-boot:run
  ```

#### Trường hợp 2: Chạy khi có cấu hình tùy chỉnh (Khác với mặc định)
Nếu bạn thay đổi các giá trị cấu hình trong file `.env`, bạn cần nạp các biến môi trường này vào terminal trước khi chạy dự án:

* **Trên Windows PowerShell:**
  ```powershell
  # 1. Nạp biến môi trường từ file .env
  Get-Content .env | ForEach-Object { $name, $value = $_.Split('=', 2); if ($name -and $value) { [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process") } }

  # 2. Khởi chạy ứng dụng
  .\mvnw.cmd spring-boot:run
  ```
* **Trên Windows Command Prompt (CMD):**
  ```cmd
  set POSTGRES_PORT=5432
  set POSTGRES_DB=resource_allocation
  set POSTGRES_USER=tên_user_tùy_chỉnh
  set POSTGRES_PASSWORD=mật_khẩu_tùy_chỉnh
  .\mvnw.cmd spring-boot:run
  ```
* **Trên Linux / macOS:**
  ```bash
  export $(cat .env | xargs) && ./mvnw spring-boot:run
  ```

---

## Kiểm tra Trạng thái Ứng dụng (Verification)

Sau khi ứng dụng khởi động thành công, bạn có thể kiểm tra qua các URL sau:

| Dịch vụ | URL truy cập | Thông tin bổ sung |
| :--- | :--- | :--- |
| **Ứng dụng API** | `http://localhost:8080` | |
| **Kiểm tra trạng thái (Health Check)** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Trả về `{"status":"UP"}` nếu chạy tốt |
| **Tài liệu API (Swagger UI)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Dùng để thử nghiệm các endpoint API |
| **Trình quản lý DB (pgAdmin)** | [http://localhost:5050](http://localhost:5050) | Đăng nhập mặc định: `admin@company.com` / `admin123` |

---

## Cấu trúc thư mục chính (Key Project Structure)

### Backend
```
com.company.resourceallocation
├── core                      # Module nghiệp vụ chính
│   ├── employee              #   Employee CRUD
│   ├── project               #   Project CRUD
│   └── allocation            #   Allocation (phân bổ) — business logic lõi
├── report                    # Báo cáo (utilization, available, overloaded)
├── ai                        # AI Bonus (Gemini integration)
├── exception                 # GlobalExceptionHandler + custom exceptions
├── aspect                    # AOP Logging
└── config                    # OpenAPI / Swagger config
```

### Frontend (riêng)
```
resource_allocation_fe/
├── src/pages/
│   ├── Dashboard.tsx
│   ├── Employees.tsx
│   ├── Projects.tsx
│   ├── Allocations.tsx
│   └── AiAssistant.tsx
├── src/services/api.ts       # API client
└── Backend: http://localhost:8080/api/v1
```

---

## Seed Data

Hệ thống tự động tạo dữ liệu mẫu qua **Flyway migration** (`V2__seed_data.sql`):

| Employee | Role | Allocation |
|---|---|---|
| EMP001 - Nguyen Van A | Java Developer | 100% (PRJ001=60%, PRJ002=40%) |
| EMP002 - Tran Thi B | Java Developer | 80% (PRJ001=80%) |
| EMP003 - Le Van C | React Developer | 40% (PRJ001=40%) |
| EMP004 - Pham Van D | DevOps Engineer | 0% |

| Project | Status |
|---|---|
| PRJ001 - E-Commerce Platform | ACTIVE |
| PRJ002 - Internal Dashboard | PLANNING |
| PRJ003 - Legacy Upgrade | COMPLETED |

---

## Cấu hình AI (Gemini) — cho AI Bonus

Nếu muốn dùng AI Bonus (recommend / risk-detection), cần set API key:

```bash
# Trên Windows PowerShell
$env:GEMINI_API_KEY="your-gemini-api-key"

# Trên Linux/macOS
export GEMINI_API_KEY="your-gemini-api-key"
```

Hoặc thêm vào file `.env`:
```
GEMINI_API_KEY=your-gemini-api-key
```

> **Không có key?** AI endpoint vẫn chạy — fallback về dữ liệu thật từ database, không bị lỗi.  
> Lấy Gemini API key miễn phí tại: https://aistudio.google.com/apikey

---

## Deliverables

| # | File | Vị trí |
|---|---|---|
| 1 | Source Code Git Repository | `https://github.com/mtritran/resource_allocation_be.git` |
| 2 | SQL Script (Flyway migration) | `src/main/resources/db/migration/V1__init_schema.sql` |
| 3 | README.md (file này) | `README.md` |
| 4 | Postman Collection | `deliverables/Resource_Allocation_API.postman_collection.json` |
| 5 | API Screenshots | `deliverables/screenshots/` *(cần chụp thêm)* |
| 6 | AI Review Report | `deliverables/AI_Review_Report.md` |
