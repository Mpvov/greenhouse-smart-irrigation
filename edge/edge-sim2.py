import time
import random
import paho.mqtt.client as mqtt

# ==========================================
# 1. CẤU HÌNH KẾT NỐI (CONFIGURATION)
# ==========================================
# Nếu Mosquitto chạy trên cùng máy tính này (qua Docker), dùng 127.0.0.1
# Nếu EC2, thay bằng IP Public của EC2
BROKER_ADDRESS = "10.0.101.37" 
PORT = 1883
TOPIC_TELEMETRY_TEMP = "z_1/temp"
TOPIC_TELEMETRY_HUMI = "z_1/humidity"

#Row 1
TOPIC_TELEMETRY_SOIL = "z_1/r_2/soil"
TOPIC_TELEMETRY_PUMP_STATUS = "z_1/r_2/pump_status"
TOPIC_COMMAND_PUMP = "z_1/r_2/pump"
TOPIC_COMMAND_CONFIG = "z_1/r_2/config"
TOPIC_COMMAND_CONFIG_WILDCARD = "z_1/r_2/config/#"



CLIENT_ID = f"Mock_YoloBit_{int(time.time())}"

base_pump_status = 0

# ==========================================
# Config variables (Mode, Threshold, Schedule)
# ==========================================
config_mode = "AUTO"  # AUTO, MANUAL, SCHEDULE
config_threshold_min = 30.0
config_threshold_max = 70.0
config_schedules = []  # List of active schedules [{"start_time": "08:00", "duration_mins": 15}, ...]

# Hàm callback khi kết nối thành công tới Broker
def on_connect(client, userdata, flags, reason_code, properties=None):
    if reason_code == 0:
        print(f"✅ Đã kết nối thành công tới Broker {BROKER_ADDRESS}:{PORT}")
        client.subscribe(TOPIC_COMMAND_PUMP, qos=1)
        client.subscribe(TOPIC_COMMAND_CONFIG, qos=1)
        client.subscribe(TOPIC_COMMAND_CONFIG_WILDCARD, qos=1)
        print(f"📡 Đang lắng nghe lệnh bơm tại topic: {TOPIC_COMMAND_PUMP}")
        print(f"📡 Đang lắng nghe cấu hình lịch tại topic: {TOPIC_COMMAND_CONFIG}")
    else:
        print(f"❌ Kết nối thất bại, mã lỗi: {reason_code}")


def on_disconnect(client, userdata, flags, reason_code, properties=None):
    if reason_code != 0:
        print(f"⚠️ MQTT bị ngắt kết nối, reason_code={reason_code}. Đang tự reconnect...")


def on_message(client, userdata, msg):
    global base_pump_status, config_mode, config_threshold_min, config_threshold_max, config_schedules
    import json
    
    if msg.topic == TOPIC_COMMAND_PUMP:
        raw_cmd = msg.payload.decode(errors='ignore').strip().upper()

        # Handle deterministic commands first to avoid flip-flop state.
        if raw_cmd in ("ON", "1", "TRUE"):
            new_status = 1
        elif raw_cmd in ("OFF", "0", "FALSE"):
            new_status = 0
        else:
            # Keep backward compatibility for legacy TOGGLE payloads.
            new_status = 0 if base_pump_status == 1 else 1

        if new_status != base_pump_status:
            base_pump_status = new_status
            print(f"🔁 Nhận lệnh pump '{raw_cmd or 'TOGGLE'}', đổi trạng thái thành: {base_pump_status}")
        else:
            print(f"ℹ️ Nhận lệnh pump '{raw_cmd or 'TOGGLE'}', trạng thái giữ nguyên: {base_pump_status}")
    elif msg.topic.endswith("/config/mode"):
        try:
            payload_str = msg.payload.decode(errors='ignore')
            payload_json = json.loads(payload_str)
            config_mode = payload_json.get("currentMode", "AUTO")
            print(f"⚙️ Cập nhật Mode từ cloud: {config_mode}")
            print(f"   📊 Mode hiện tại: {config_mode}")
        except Exception as e:
            print(f"⚠️ Lỗi parse mode config: {e}")
    elif msg.topic.endswith("/config/threshHold"):
        try:
            payload_str = msg.payload.decode(errors='ignore')
            payload_json = json.loads(payload_str)
            threshold_config = payload_json.get("threshHoldConfig", {})
            config_threshold_min = float(threshold_config.get("threshHoldMin", 30.0))
            config_threshold_max = float(threshold_config.get("threshHoldMax", 70.0))
            print(f"🎯 Cập nhật Threshold từ cloud: min={config_threshold_min}, max={config_threshold_max}")
            print(f"   📊 Threshold hiện tại: {config_threshold_min} - {config_threshold_max}%")
        except Exception as e:
            print(f"⚠️ Lỗi parse threshold config: {e}")
    elif msg.topic.endswith("/config/schedule"):
        try:
            payload_str = msg.payload.decode(errors='ignore')
            payload_json = json.loads(payload_str)
            schedules_list = payload_json.get("schedules", [])
            config_schedules = schedules_list
            print(f"🗓️ Cập nhật Schedule từ cloud: {len(config_schedules)} lịch hoạt động")
            for sched in config_schedules:
                print(f"   📋 {sched.get('start_time', 'N/A')} - {sched.get('duration_mins', 'N/A')} phút (active={sched.get('is_active', False)})")
        except Exception as e:
            print(f"⚠️ Lỗi parse schedule config: {e}")
    elif msg.topic == TOPIC_COMMAND_CONFIG or msg.topic.startswith(TOPIC_COMMAND_CONFIG + "/"):
        print(f"🗓️ Nhận cấu hình từ cloud [{msg.topic}]: {msg.payload.decode(errors='ignore')}")

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
    global base_pump_status, config_mode, config_threshold_min, config_threshold_max
    print("🚀 Bắt đầu giả lập luồng dữ liệu cảm biến (Nhấn Ctrl+C để dừng)...")
    
    # Khởi tạo giá trị gốc
    base_temp = 28.0
    base_humi = 65.0
    base_soil = 45.0
    soil_val = base_soil
    while True:
        try:
            # Sinh dữ liệu ngẫu nhiên (dao động nhẹ quanh giá trị gốc) để test biểu đồ
            temp_val = round(base_temp + random.uniform(-1.5, 1.5), 1)
            humi_val = round(base_humi + random.uniform(-5.0, 5.0), 1)
            soil_val = soil_val - 1
            if base_pump_status == 1: # Nếu pump đang bật, soil sẽ tăng dần lên
                soil_val =  soil_val+5;        
            # Cố tình tạo ra một spike (đột biến) để test ngoại lệ E2/E3 lâu lâu 1 lần
            # if random.randint(1, 20) == 20: 
            #     soil_val = 100.0 # Lỗi cảm biến (E3)
            #     print("⚠️ [MÔ PHỎNG LỖI CẢM BIẾN]")

            # ==========================================
            # AUTO Mode: Automatic pump control based on threshold
            # ==========================================
            if config_mode == "AUTO":
                if soil_val < config_threshold_min and base_pump_status == 0:
                    # Soil too low -> turn pump ON
                    base_pump_status = 1
                    print(f"🔄 [AUTO] Soil ({soil_val}%) < min ({config_threshold_min}%) -> BẬT máy bơm")
                elif soil_val > config_threshold_max and base_pump_status == 1:
                    # Soil too high -> turn pump OFF
                    base_pump_status = 0
                    print(f"🔄 [AUTO] Soil ({soil_val}%) > max ({config_threshold_max}%) -> TẮT máy bơm")

            # Tạo Payload Dictionary
            payload_dict_temp = str(temp_val)
            payload_dict_humi = str(humi_val)
            payload_dict_soil = str(soil_val)
            payload_dict_pump_status = str(base_pump_status)
            
            # Publish lên Broker
            client.publish(TOPIC_TELEMETRY_TEMP, payload_dict_temp, qos=1)
            client.publish(TOPIC_TELEMETRY_HUMI, payload_dict_humi, qos=1)
            client.publish(TOPIC_TELEMETRY_SOIL, payload_dict_soil, qos=1)
            client.publish(TOPIC_TELEMETRY_PUMP_STATUS, payload_dict_pump_status, qos=1)

            print(
                f"📤 Đã gửi: payload_dict_temp = {payload_dict_temp}, payload_dict_humi = {payload_dict_humi}, "
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