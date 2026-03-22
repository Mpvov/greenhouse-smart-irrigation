# IrriSmart - IoT-Based Smart Irrigation System

## 1. Giới thiệu dự án
IrriSmart là một nền tảng IoT toàn diện dành cho hệ thống tưới tiêu thông minh, được thiết kế theo kiến trúc phân tán **Edge-Fog-Cloud**. Hệ thống cho phép giám sát các yếu tố môi trường (độ ẩm đất, nhiệt độ, độ ẩm không khí) theo thời gian thực và tự động hóa quá trình tưới tiêu dựa trên dữ liệu cảm biến cũng như lịch trình được thiết lập sẵn, hướng tới nền nông nghiệp chính xác và tiết kiệm tài nguyên.

## 2. Tech Stack
Hệ thống được xây dựng trên nền tảng công nghệ hiện đại, bất đồng bộ (Reactive) và khả năng mở rộng cao:
- **Backend:** Java 21, Spring Boot 3.3 (Spring WebFlux), Spring Integration MQTT, cấu trúc Stateless với JWT.
- **Frontend:** React 18, Vite, React Router 6, Recharts (vẽ biểu đồ), Zustand (quản lý State).
- **Database:** Reactive MongoDB (lưu trữ Time-series data, thông tin cấu hình), Reactive Redis (Real-time Pub/Sub, Caching).
- **IoT & Infrastructure:** Mosquitto MQTT, Node-RED (Fog Gateway), Nginx (Reverse Proxy), Docker & Docker Compose.
- **Edge Simulation:** Python 3.x.

## 3. Hướng dẫn chạy MVP qua Docker (How to run locally)
Dự án đã được đóng gói sẵn. Để spin-up toàn bộ hệ thống (MongoDB, Redis, Mosquitto, Nginx, Backend, Frontend), hãy sử dụng Docker Compose tại thư mục gốc của dự án:

```bash
docker-compose up --build -d
```

**Cách kiểm tra các service đã hoạt động thành công:**
- **Frontend (Dashboard):** Truy cập `http://localhost` (qua Nginx) hoặc `http://localhost:5173` (direct).
- **Backend API:** Truy cập `http://localhost/api` (qua Nginx) hoặc `http://localhost:8080` (direct).
- **Node-RED (Fog Gateway):** Truy cập `http://localhost:1880`.
- **Cloud MQTT Broker:** Kết nối qua `tcp://localhost:1884`.
- **Fog MQTT Broker (Local):** Kết nối qua `tcp://localhost:1883`.

