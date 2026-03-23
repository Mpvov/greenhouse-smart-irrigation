# Sổ tay Hướng dẫn cho Developer & Tester (How-to Guide)

Tài liệu này cung cấp các hướng dẫn thiết yếu để thành viên mới nhanh chóng làm quen với quy trình phát triển và kiểm thử hệ thống.

## 1. Hướng dẫn giả lập Edge Device & Node-RED (Simulation Guide)
Khi chạy hệ thống thông qua Docker Compose, các service (Mosquitto, Mongo, Backend...) nằm trong một **Docker Network ảo** riêng biệt. 
Do đó, nếu bạn chạy mạch phần cứng thật (như Yolo:bit) kết nối Wifi, hoặc chạy Python Edge Simulator *bên ngoài* container (trực tiếp trên máy Host), bạn **KHÔNG THỂ** sử dụng `localhost` hay `127.0.0.1` để kết nối vào MQTT Broker.

**Hướng dẫn cấu hình kết nối:**
1. Mở Terminal / Command Prompt trên máy tính (máy Host) của bạn.
2. Gõ lệnh `ipconfig` (đối với Windows) hoặc `ifconfig` (đối với Mac/Linux).
3. Tìm địa chỉ **IP LAN** của máy tính trong mạng nội bộ (Ví dụ: `192.168.1.5` hoặc `10.0.0.12`).
4. Cấu hình IP LAN này vào thiết bị:
   - Trong file `edge/edge-sim.py`, đổi biến `BROKER_ADDRESS` thành địa chỉ IP LAN vừa lấy được.
   - Cấu hình Node-RED bằng cách truy cập vào localhost:1880 sau đó import flows.json và nhấn Deploy
   - Chạy file giả lập: 

   ```bash
    # Cài môi trường ảo và thư viện cho python
    python -m venv ./venv
    .\venv\Scripts\activate
    pip install -r requirements.txt
    # Chạy giả lập
    python edge/edge-sim.py
    ```
    - Đăng nhập vào localhost với mail: admin@bk.hcm password: 123456

## 2. Quy trình thêm tính năng mới (Implementation Flow)
Dự án hoàn toàn sử dụng kiến trúc Non-blocking với **Spring WebFlux (Project Reactor)**. Tuyệt đối **KHÔNG SỬ DỤNG** các hàm đồng bộ hoặc `.block()` trong mã nguồn.

Tuân thủ luồng phát triển sau (Từ trong ra ngoài):
1. **Model & DTO:** Bắt đầu bằng việc định nghĩa Entity ánh xạ với MongoDB (Sử dụng `@Document`) và tạo các lớp DTO để nhận request / trả response.
2. **Repository:** Khởi tạo interface kế thừa `ReactiveMongoRepository<Entity, ID>`. Các hàm query custom phải trả về `Mono<>` hoặc `Flux<>`.
3. **Service Layer:** Tạo Interface và class Implementation (VD: `ZoneServiceImpl`). Xử lý nghiệp vụ bằng các toán tử Reactive (như `map()`, `flatMap()`, `filter()`, `zip()`).
   - Dùng `Mono<T>` nếu kết quả trả về là 0 hoặc 1 object.
   - Dùng `Flux<T>` nếu kết quả trả về là 1 danh sách nhiều object (List/Stream).
4. **Controller Layer:** Tạo REST Controller, inject Service, nhận Payload, gọi service method và mapping kết quả vào class `BaseResponse<T>`.

## 3. Hướng dẫn viết Test (TDD Approach)
Chúng ta duy trì chất lượng mã nguồn thông qua 2 cấp độ kiểm thử. 

**a) Unit Test:**
- Dùng để test logic nghiệp vụ thuần túy của Service layer.
- Sử dụng **Mockito** (`@Mock`, `@InjectMocks`) để mock các lớp Repository, MQTT Gateway. Không gọi Database thật.
- Khởi chạy nhanh, giúp verify các luồng điều kiện (if/else), tính toán.

**b) Integration Test:**
- Test end-to-end từ Controller -> Service -> DB.
- Dự án sử dụng thư viện **Testcontainers**. Khi chạy test, hệ thống tự động spin-up một Docker container chứa MongoDB và Redis thật, cấp port ngẫu nhiên và tiêm vào properties của Spring. Điều này loại bỏ hoàn toàn tình trạng "code chạy được trên máy tôi nhưng lỗi trên server".
- Sử dụng **`WebTestClient`** thay cho `MockMvc` truyền thống để bắn request HTTP vào các endpoint WebFlux và verify HTTP Status, Response Body.

**c) Xử lý Luồng Reactive trong Test:**
Vì `Mono`/`Flux` là luồng bất đồng bộ, bạn không thể assert bằng AssertJ/JUnit thông thường. Thay vào đó, **BẮT BUỘC** phải dùng thư viện `StepVerifier`.

*Ví dụ chuẩn mực:*
```java
// Unit test cho một hàm trả về Mono<Zone>
Mono<Zone> zoneMono = zoneService.findById("zone-id-1");

StepVerifier.create(zoneMono)
    .assertNext(zone -> {
        assertEquals("Tomato Zone", zone.getName());
        assertEquals("active", zone.getStatus());
    })
    .verifyComplete(); // Đảm bảo luồng đã kết thúc thành công
```
