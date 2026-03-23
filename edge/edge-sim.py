import time
import json
import random
import paho.mqtt.client as mqtt

# ==========================================
# 1. CẤU HÌNH KẾT NỐI (CONFIGURATION)
# ==========================================
# Nếu Mosquitto chạy trên cùng máy tính này (qua Docker), dùng 127.0.0.1
# Nếu EC2, thay bằng IP Public của EC2
BROKER_ADDRESS = "10.118.205.72" 
PORT = 1883
TOPIC_TELEMETRY_TEMP = "z_1/temp"
TOPIC_TELEMETRY_HUMI = "z_1/humidity"
TOPIC_TELEMETRY_SOIL = "z_1/r_1/soil"

# Hàm callback khi kết nối thành công tới Broker
def on_connect(client, userdata, flags, reason_code, properties=None):
    if reason_code == 0:
        print(f"✅ Đã kết nối thành công tới Broker {BROKER_ADDRESS}:{PORT}")
    else:
        print(f"❌ Kết nối thất bại, mã lỗi: {reason_code}")

# ==========================================
# 2. KHỞI TẠO MQTT CLIENT
# ==========================================
# Sử dụng API version 2 của paho-mqtt (bản mới nhất)
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="Mock_YoloBit_01")
client.on_connect = on_connect

# Nếu có dùng username/password thì bỏ comment dòng dưới:
# client.username_pw_set("admin", "password123")

client.connect(BROKER_ADDRESS, PORT, 60)

# Khởi chạy một thread chạy ngầm để duy trì kết nối mạng
client.loop_start()

# ==========================================
# 3. VÒNG LẶP SINH DỮ LIỆU GIẢ (DATA GENERATOR)
# ==========================================
def publish_mock_sensor_data():
    print("🚀 Bắt đầu giả lập luồng dữ liệu cảm biến (Nhấn Ctrl+C để dừng)...")
    
    # Khởi tạo giá trị gốc
    base_temp = 28.0
    base_humi = 65.0
    base_soil = 45.0

    while True:
        try:
            # Sinh dữ liệu ngẫu nhiên (dao động nhẹ quanh giá trị gốc) để test biểu đồ
            temp_val = round(base_temp + random.uniform(-1.5, 1.5), 1)
            humi_val = round(base_humi + random.uniform(-5.0, 5.0), 1)
            soil_val = round(base_soil + random.uniform(-2.0, 2.0), 1)
            
            # Cố tình tạo ra một spike (đột biến) để test ngoại lệ E2/E3 lâu lâu 1 lần
            if random.randint(1, 20) == 20: 
                soil_val = 100.0 # Lỗi cảm biến (E3)
                print("⚠️ [MÔ PHỎNG LỖI CẢM BIẾN]")

            # Tạo Payload Dictionary
            payload_dict_temp = str(temp_val)
            payload_dict_humi = str(humi_val)
            payload_dict_soil = str(soil_val)
            
            # Publish lên Broker
            client.publish(TOPIC_TELEMETRY_TEMP, payload_dict_temp, qos=1)
            client.publish(TOPIC_TELEMETRY_HUMI, payload_dict_humi, qos=1)
            client.publish(TOPIC_TELEMETRY_SOIL, payload_dict_soil, qos=1)

            print(f"📤 Đã gửi: payload_dict_temp = {payload_dict_temp}, payload_dict_humi = {payload_dict_humi}, payload_dict_soil = {payload_dict_soil}    ")
            
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