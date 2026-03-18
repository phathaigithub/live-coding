# Live Coding Platform - Backend Documentation

Hệ thống cung cấp nền tảng thực thi mã nguồn trực tuyến (Live Code Execution) hỗ trợ đa ngôn ngữ (Python, JavaScript) với kiến trúc hướng sự kiện (Event-driven) và xử lý bất đồng bộ.

## 1. Architecture Overview

### End-to-end Request Flow
Hệ thống xử lý yêu cầu của người dùng qua các giai đoạn:

1.  **Code Session Creation**: Người dùng khởi tạo phiên làm việc qua `POST /code-sessions`. Hệ thống tạo bản ghi trong PostgreSQL để quản lý ngữ cảnh (ngôn ngữ, mã nguồn).
2.  **Autosave Behavior**: Trong khi soạn thảo, client gửi `PATCH /code-sessions/{id}` định kỳ. Hệ thống cập nhật mã nguồn vào Database để đảm bảo dữ liệu không bị mất nếu có sự cố.
3.  **Execution Request**: Khi nhấn "Run", client gọi `POST /code-sessions/{id}/run`. Hệ thống tạo một bản ghi `Execution` với trạng thái `QUEUED` và đẩy metadata vào **Redis List Queue**. Phản hồi được trả về ngay lập tức cho client.
4.  **Background Execution**: `ExecutionWorker` liên tục "pop" các task từ Redis. Worker thực thi mã trong các **Docker Containers** bị cô lập để đảm bảo an toàn.
5.  **Result Polling**: Client sử dụng `execution_id` để gọi `GET /executions/{id}` định kỳ (polling) nhằm kiểm tra trạng thái và nhận kết quả cuối cùng (stdout/stderr).

##  Architecture Decisions
*   **Worker Queue Pattern**: Tách biệt API nhận yêu cầu và quá trình thực thi code nặng. Điều này giúp hệ thống đạt trạng thái **Non-blocking**, xử lý hàng nghìn request đồng thời mà không nghẽn.
*   **Docker-based Sandboxing**: Mỗi đoạn code người dùng được chạy trong một Container riêng biệt. Đây là quyết định quan trọng để:
    *   **Isolation**: Cô lập hoàn toàn mã độc khỏi server chính.
    *   **Resource Control**: Giới hạn chính xác CPU (0.5 Core) và RAM (128MB) cho mỗi task.
    *   **Security**: Chặn hoàn toàn kết nối Internet của container (`networkMode: none`).
*   **Redis as Message Broker**: Sử dụng Redis List nhờ tốc độ truy xuất nhanh và cơ chế Pop task an toàn cho mô hình nhiều Worker chạy song song.

### Execution Lifecycle and State Management
Mỗi yêu cầu thực thi đều trải qua vòng đời nghiêm ngặt:
*   **QUEUED**: Task đã nằm trong hàng đợi.
*   **RUNNING**: Worker đã nhận task và bắt đầu khởi tạo container.
*   **COMPLETED**: Mã thực thi thành công và trả về kết quả.
*   **FAILED**: Lỗi cú pháp, lỗi thực thi hoặc lỗi hệ thống.
*   **TIMEOUT**: Mã chạy quá thời gian quy định (mặc định 10s).

## 2. Reliability & Data Model

### Execution States
Vòng đời trạng thái: `QUEUED → RUNNING → COMPLETED / FAILED / TIMEOUT`. 
Trạng thái được cập nhật trực tiếp vào PostgreSQL giúp đảm bảo tính nhất quán dữ liệu ngay cả khi Worker bị restart.

### Idempotency Handling
*   **Prevent duplicate runs**: Mỗi lần nhấn "Run" tạo ra một `execution_id` duy nhất (UUID), tách biệt hoàn toàn các lần thực thi khác nhau.
*   **Safe Reprocessing**: Metadata của task (code, language) được đóng gói kèm theo task trong queue, đảm bảo Worker luôn xử lý đúng phiên bản code tại thời điểm yêu cầu.

### Failure Handling
*   **Retries**: Hiện tại hệ thống ưu tiên trả về lỗi nhanh (Fail-fast). Cơ chế retry có thể được cấu hình tại Redis consumer nếu cần.
*   **Error States**: Mọi ngoại lệ (Docker error, Runtime error) đều được bắt và lưu vào trường `stderr` của bản ghi `Execution`.
*   **Resource Cleanup**: Container luôn được xóa cưỡng chế trong khối `finally` để tránh rò rỉ tài nguyên.

## 3. Scalability Considerations

*   **Handling Concurrent Sessions**: Việc lưu code trong PostgreSQL và task trong Redis giúp hệ thống chịu tải được hàng ngàn session cùng lúc mà không tốn bộ nhớ RAM của ứng dụng chính.
*   **Horizontal Scaling**: Có thể chạy nhiều instance của `ExecutionWorker` trên nhiều server khác nhau. Mỗi worker sẽ cạnh tranh để lấy task từ Redis (mô hình Competing Consumers).
*   **Queue Backlog**: Nếu số lượng yêu cầu quá lớn, hàng đợi Redis sẽ tích lũy task. Worker sẽ xử lý dần dần theo tiến độ mà không làm sập hệ thống.
*   **Bottlenecks**: Nút thắt cổ chai nằm ở Docker Engine (số lượng container tạo mới mỗi giây). Giải pháp là pooling container hoặc sử dụng cụm Docker Swarm/K8s.

## 4. Trade-offs

*   **Technology Choices**: Chọn Docker để có tốc độ khởi động nhanh (ms) và mật độ container cao trên một host.
*   **Optimization**: Hệ thống được tối ưu cho **Simplicity** và **Reliability** trong việc cô lập mã nguồn.
*   **Production Readiness Gaps**:
    *   Cần pooling Docker images để tránh độ trễ pull image.
    *   Cần Rate-limiting để ngăn một user spam quá nhiều request "Run".
    *   Cần hệ thống giám sát (Monitoring) độ dài hàng đợi.

---
## 5. Hướng dẫn khởi chạy nhanh với Docker

### Yêu cầu hệ thống (Prerequisites)
*   **Docker Desktop**: Đã cài đặt và đang chạy.

### Các bước khởi chạy

#### Bước 1: Clone dự án
```bash
git clone https://github.com/phathaigithub/live-coding.git
```

#### Bước 2: Tải các Docker Images cần thiết 
Dự án cần các image này để tạo môi trường chạy code cô lập cho Python và Node.js:
```bash
docker pull python:3.10-slim
docker pull node:18-slim
```

#### Bước 3: Khởi chạy toàn bộ hệ thống
Sử dụng Docker Compose để khởi chạy Backend, PostgreSQL và Redis:

**Trên Linux/macOS:**
```bash
docker-compose up --build -d
```

**Trên Windows (PowerShell):**
Nếu bạn gặp lỗi về volume mount `/var/run/docker.sock`, hãy chỉnh sửa file `docker-compose.yml` phần volume của service `backend` thành `//./pipe/docker_engine://./pipe/docker_engine` trước khi chạy:
```powershell
docker-compose up --build -d
```

#### Bước 4: Truy cập ứng dụng
Ứng dụng Backend sẽ sẵn sàng tại: `http://localhost:8080`

##  API Documentation

| Method | Endpoint | Description | Request Body Example | Response Example (200 OK) |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/code-sessions` | Tạo phiên làm việc mới | N/A | `{"session_id": "uuid", "status": "ACTIVE"}` |
| **PATCH** | `/code-sessions/{id}` | Autosave mã nguồn | `{"language": "python", "source_code": "print(1)"}` | `{"session_id": "uuid", "status": "ACTIVE"}` |
| **POST** | `/code-sessions/{id}/run`| Thực thi code (Async) | N/A | `{"execution_id": "uuid", "status": "QUEUED"}` |
| **GET** | `/executions/{id}` | Lấy kết quả thực thi | N/A | `{"status": "COMPLETED", "stdout": "1\n", ...}` |

