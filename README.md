# Resource Allocation System (Backend)

Dự án Backend quản lý phân bổ nguồn lực (Resource Allocation System) xây dựng bằng **Spring Boot 4.x** (Java 17) và **PostgreSQL**. Dự án sử dụng **Flyway** để quản lý di cư cơ sở dữ liệu và **Docker Compose** để thiết lập môi trường chạy thử nghiệm.

---

## Yêu cầu hệ thống (Prerequisites)
Trước khi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:
* **Java 17+** (JDK 17 trở lên)
* **Docker Desktop** (để khởi chạy Database)
* **Git** (để quản lý phiên bản)

---

## Hướng dẫn Thiết lập & Chạy dự án

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
```
com.company.resourceallocation
├── core                      # Chứa các module nghiệp vụ chính (Employee, Project, Allocation)
│   ├── employee
│   ├── project
│   └── allocation
├── report                    # Module xuất báo cáo
├── ai                        # Tích hợp AI (gợi ý phân bổ / cảnh báo rủi ro)
├── exception                 # Xử lý ngoại lệ tập trung (GlobalExceptionHandler)
├── aspect                    # Ghi logs tự động (AOP Logging)
└── config                    # Cấu hình OpenAPI (Swagger)
```
