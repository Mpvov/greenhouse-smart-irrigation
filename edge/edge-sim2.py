import time
import random
import paho.mqtt.client as mqtt

# ==========================================
# 1. CẤU HÌNH KẾT NỐI (CONFIGURATION)
# ==========================================
# Nếu Mosquitto chạy trên cùng máy tính này (qua Docker), dùng 127.0.0.1
# Nếu EC2, thay bằng IP Public của EC2
BROKER_ADDRESS = "10.0.236.176" 
PORT = 1883

#Row 2
TOPIC_TELEMETRY_SOIL = "z_1/r_2/soil"
TOPIC_TELEMETRY_PUMP_STATUS = "z_1/r_2/pump_status"
TOPIC_COMMAND_PUMP = "z_1/r_2/pump"



CLIENT_ID = f"Mock_YoloBit_{int(time.time())}"

base_pump_status = 0

# Hàm callback khi kết nối thành công tới Broker
def on_connect(client, userdata, flags, reason_code, properties=None):
    if reason_code == 0:
        print(f"✅ Đã kết nối thành công tới Broker {BROKER_ADDRESS}:{PORT}")
        client.subscribe(TOPIC_COMMAND_PUMP, qos=1)
        print(f"📡 Đang lắng nghe lệnh bơm tại topic: {TOPIC_COMMAND_PUMP}")
    else:
        print(f"❌ Kết nối thất bại, mã lỗi: {reason_code}")


def on_disconnect(client, userdata, flags, reason_code, properties=None):
    if reason_code != 0:
        print(f"⚠️ MQTT bị ngắt kết nối, reason_code={reason_code}. Đang tự reconnect...")


def on_message(client, userdata, msg):
    global base_pump_status
    if msg.topic == TOPIC_COMMAND_PUMP:
        base_pump_status = 0 if base_pump_status == 1 else 1
        print(f"🔁 Nhận lệnh pump, đổi trạng thái thành: {base_pump_status}")

# ==========================================
# 2. KHỞI TẠO MQTT CLIENT
# ==========================================
# Sử dụng API version 2 của paho-mqtt (bản mới nhất)
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=CLIENT_ID)
client.on_connect = on_connect
client.on_disconnect = on_disconnect
client.on_message = on_message

# Nếu có dùng username/password thì bỏ comment dòng dưới:
# client.username_pw_set("admin", "password123")

client.connect(BROKER_ADDRESS, PORT, 60)

# Khởi chạy một thread chạy ngầm để duy trì kết nối mạng
client.loop_start()

# ==========================================
# 3. VÒNG LẶP SINH DỮ LIỆU GIẢ (DATA GENERATOR)
# ==========================================
def publish_mock_sensor_data():
    global base_pump_status
    print("🚀 Bắt đầu giả lập luồng dữ liệu cảm biến (Nhấn Ctrl+C để dừng)...")
    
    # Khởi tạo giá trị gốc
    base_soil = 45.0
    while True:
        try:
            # Sinh dữ liệu ngẫu nhiên (dao động nhẹ quanh giá trị gốc) để test biểu đồ
            soil_val = round(base_soil + random.uniform(-2.0, 2.0), 1)
            
            # Cố tình tạo ra một spike (đột biến) để test ngoại lệ E2/E3 lâu lâu 1 lần
            if random.randint(1, 20) == 20: 
                soil_val = 100.0 # Lỗi cảm biến (E3)
                print("⚠️ [MÔ PHỎNG LỖI CẢM BIẾN]")

            # Tạo Payload Dictionary
            payload_dict_soil = str(soil_val)
            payload_dict_pump_status = str(base_pump_status)
            
            # Publish lên Broker
            client.publish(TOPIC_TELEMETRY_SOIL, payload_dict_soil, qos=1)
            client.publish(TOPIC_TELEMETRY_PUMP_STATUS, payload_dict_pump_status, qos=1)

            print(
                f"payload_dict_soil = {payload_dict_soil}, payload_dict_pump_status = {payload_dict_pump_status}"
            )
            
        except KeyboardInterrupt:
            print("\n🛑 Đã dừng giả lập.")
            client.loop_stop()
            client.disconnect()
            break
        except Exception as e:
            print(f"Lỗi: {e}")
            
        # Gửi mỗi 3 giây 1 lần
        time.sleep(5)

# Chạy vòng lặp
if __name__ == "__main__":
    publish_mock_sensor_data()