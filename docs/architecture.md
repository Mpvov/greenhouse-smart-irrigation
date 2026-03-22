# Thiết kế Kiến trúc Hệ thống (System Architecture)

## 1. Kiến trúc tổng quan Cloud-Edge
IrriSmart được thiết kế theo mô hình **Edge-Fog-Cloud** nhằm tối ưu hóa băng thông truyền tải, giảm độ trễ điều khiển và tăng tính dự phòng khi mất kết nối Internet:
- **Edge Layer:** Lớp thiết bị vật lý nằm tại nông trại bao gồm Cảm biến (Sensor) và Cơ cấu chấp hành (Actuator - bơm, van). Trong môi trường phát triển, lớp này được mô phỏng bởi Python script.
- **Fog Layer (Local):** Hệ thống máy chủ nhỏ đặt tại nhà kính. Bao gồm một Mosquitto MQTT Broker nội bộ (chỉ xử lý tín hiệu trong phạm vi mạng LAN nhà kính) và một Edge Gateway sử dụng **Node-RED**. Gateway thực hiện phân tích, lọc dữ liệu biên và đảm bảo thiết bị vẫn hoạt động tự động khi rớt mạng Cloud.
- **Cloud Layer:** Cụm máy chủ trung tâm. Bao gồm Cloud MQTT Broker, hệ thống Backend Spring WebFlux xử lý nghiệp vụ, MongoDB lưu trữ dữ liệu bền vững, Redis hỗ trợ real-time và Frontend Dashboard cho người quản trị.

## 2. Luồng dữ liệu (Dataflow)
Hệ thống có hai luồng dữ liệu chính hoạt động hoàn toàn bất đồng bộ.

```mermaid
graph TD
    subgraph Edge Layer
        S[Sensors] -->|MQTT| FB[Fog Broker]
        A[Actuators] <--|MQTT| FB
    end
    subgraph Fog Layer
        FB <-->|MQTT| NR[Node-RED Gateway]
    end
    subgraph Cloud Layer
        NR <-->|MQTT Over TLS| CB[Cloud Broker]
        CB -->|Upstream Telemetry| BE[Spring WebFlux Backend]
        BE -->|Downstream Command| CB
    end
    subgraph Web Clients
        BE <-->|REST / WebSockets| UI[React Dashboard]
    end
```

- **Upstream (Dữ liệu Telemetry):** Cảm biến đọc dữ liệu -> Publish lên Fog Broker -> Node-RED Gateway nhận, mapping định dạng -> Publish lên Cloud Broker -> `MqttInboundFlow` của Spring Backend nhận bản tin -> Parse JSON và đẩy vào hệ thống xử lý (TelemetryService).
- **Downstream (Dữ liệu Command/Config):** Người dùng bấm nút trên React UI -> Gọi REST API xuống Spring Backend -> `RowService/DeviceService` xử lý nghiệp vụ -> Đẩy lệnh xuống Cloud MQTT Broker -> Node-RED Gateway nhận lệnh, định tuyến lại xuống Fog Broker -> Kích hoạt Actuator tại Edge.

## 3. Kiến trúc Hot Path và Cold Path
Để đáp ứng cùng lúc 2 yêu cầu: "Giao diện phải mượt mà tức thì" và "Dữ liệu phải lưu trữ đầy đủ để phân tích", hệ thống tách luồng xử lý tại Backend thành 2 đường:
- **Hot Path (Luồng Real-time):** Khi `TelemetryService` nhận dữ liệu mới, dữ liệu ngay lập tức được đẩy vào hệ thống **Redis Pub/Sub**. Lớp `TelemetryWebSocketHandler` đang duy trì các kết nối WebSockets với React client sẽ subscribe kênh Redis này, lập tức broadcast bản tin tới Frontend mà không cần chạm vào ổ cứng. Độ trễ luồng này rất thấp.
- **Cold Path (Luồng Lưu trữ/Analytics):** Đồng thời, dữ liệu cũng được chuyển đổi và lưu trữ không đồng bộ (non-blocking) vào **MongoDB** dưới dạng Time-series data (collection `DataRecord`). Luồng này phục vụ cho API xem lịch sử (HistoryController) và vẽ biểu đồ. Sự chậm trễ của Database I/O sẽ không làm ảnh hưởng đến tốc độ của Hot Path nhờ cơ chế Reactive.

## 4. Phân tích MQTT Topic Tree và vai trò IoT Gateway
Topic Tree được phân chia rõ rệt để bảo mật và phân luồng mạng:
- **Internal Topics (Fog Layer):** Dùng định dạng ngắn gọn (VD: `local/sensor/temp`) để tối ưu payload cho vi điều khiển (MCU) yếu. Các bản tin này không bao giờ rời khỏi mạng LAN nội bộ.
- **Shared Topics (Cloud Layer):** Dùng định dạng phân cấp chuẩn RESTful (VD: `tenant/greenhouse1/zoneA/row1/telemetry`) để Backend dễ dàng parse, định danh nguồn gốc và cấp quyền truy cập.
- **Vai trò của Node-RED (IoT Gateway):** Node-RED đóng vai trò là "Thông dịch viên". Nó subscribe các *Internal Topics*, đính kèm thêm Metadata (như mã ID Nhà kính, ID Zone), định dạng lại thành chuỗi JSON chuẩn mực và publish lên các *Shared Topics* trên Cloud Broker (Briding/Routing).
