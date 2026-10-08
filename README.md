# 🏥 ระบบตรวจวัด ควบคุม และวิเคราะห์คุณภาพน้ำเสียโรงพยาบาล (Hospital WWTP SCADA System)
### โครงการ: จ้างซ่อมและปรับปรุงระบบบำบัดน้ำเสีย — ตู้ควบคุม (Control Cabinet) พร้อมระบบตรวจสอบออนไลน์
**รหัสโครงการ:** 2568-16-Chumpea-1-WaterPump  
**สถานที่ดำเนินงาน:** โรงพยาบาลชุมแพ จังหวัดขอนแก่น  

---

## 📑 สารบัญ (Table of Contents)
1. [ภาพรวมของโครงการ (Project Overview)](#1-ภาพรวมของโครงการ)
2. [ข้อมูลจำเพาะของบอร์ดและอุปกรณ์ (Hardware Specifications)](#2-ข้อมูลจำเพาะของบอร์ดและอุปกรณ์)
3. [ตารางกำหนดตำแหน่งขา (Pinout Mapping)](#3-ตารางกำหนดตำแหน่งขา)
4. [หลักการทำงานและการคำนวณทางไฮดรอลิกส์ (Principle of Operation & Hydraulics)](#4-หลักการทำงานและการคำนวณทางไฮดรอลิกส์)
5. [ผังวงจรไฟฟ้าและการต่อสาย (Schematic & Wiring Diagrams)](#5-ผังวงจรไฟฟ้าและการต่อสาย)
6. [ขั้นตอนการต่อวงจรและ Commissioning (Wiring & Commissioning Steps)](#6-ขั้นตอนการต่อวงจรและ-commissioning)
7. [ข้อควรระวังและมาตรการความปลอดภัย (Safety Precautions)](#7-ข้อควรระวังและมาตรการความปลอดภัย)
8. [คู่มือซอฟต์แวร์และโค้ดฉบับสมบูรณ์ (Complete Software Source Code)](#8-คู่มือซอฟต์แวร์และโค้ดฉบับสมบูรณ์)
   - [8.1 ESP32-S3 Firmware (Modbus RTU & MQTT Stream)](#81-esp32-s3-firmware)
   - [8.2 Raspberry Pi 3 B+ Node.js Analytics & Cloud Sync](#82-raspberry-pi-3-b-nodejs-analytics--cloud-sync)
   - [8.3 Asynchronous Responsive Web Application](#83-asynchronous-responsive-web-application)
   - [8.4 Android Native Application (Jetpack Compose & Gemini AI)](#84-android-native-application)

---

## 1. ภาพรวมของโครงการ
ระบบบำบัดน้ำเสียของโรงพยาบาลชุมแพ จังหวัดขอนแก่น เป็นระบบบำบัดน้ำเสียแบบชีวภาพ (Activated Sludge & Aeration) ร่วมกับระบบฆ่าเชื้อด้วยคลอรีน (Chlorination) ที่ได้รับการยกระดับด้วยตู้ควบคุมอัจฉริยะ (IoT MDB Control Cabinet) มีเป้าหมายเพื่อ:
* ควบคุมการทำงานของปั๊มสูบน้ำเสีย (Influent Pumps) และเครื่องเติมอากาศ (Aerator 5.5 kW) อัตโนมัติตามระดับน้ำและตารางเวลา
* ตรวจวัดพารามิเตอร์คุณภาพน้ำแบบเรียลไทม์ (pH, Dissolved Oxygen, ORP, Turbidity, ระดับน้ำ) ด้วยเซนเซอร์อุตสาหกรรม Modbus RS-485
* บันทึกปริมาณน้ำเสียสะสม (+8,400 ลิตร ต่อ 1 รอบการสูบ) รวบรวมยอดสะสมรายวันและรายเดือน
* แจ้งเตือนข้อผิดพลาด (Alarm & Overload Trip) ไปยังแอปพลิเคชันบนสมาร์ทโฟนแบบเรียลไทม์
* วิเคราะห์ข้อมูลเชิงลึกและวินิจฉัยปัญหาด้วย Google Gemini AI (Thinking Mode: High)

---

## 2. ข้อมูลจำเพาะของบอร์ดและอุปกรณ์

### 2.1 บอร์ดประมวลผลหลักตู้ควบคุม (Controller Boards)
* **ESP32-S3 DevKitC-1 / ESP32-WROOM-32E:**
  * CPU: Xtensa® 32-bit LX7/LX6 Dual-core ความเร็วสูงสุด 240 MHz
  * SRAM: 512 KB, Flash: 8 MB / 4 MB (รองรับ OTA Update)
  * การเชื่อมต่อ: Wi-Fi 2.4 GHz (802.11 b/g/n), Bluetooth 5 LE
  * Logic Level: 3.3V DC (ห้ามป้อน 5V/24V เข้า GPIO โดยตรง)
* **Raspberry Pi 3 Model B+ (Host Server & Edge Analytics Gateway):**
  * SoC: Broadcom BCM2837B0, Cortex-A53 64-bit @ 1.4 GHz, RAM: 1 GB
  * พอร์ต: Gigabit Ethernet, USB 2.0 x 4, HDMI, 2.4/5GHz Wi-Fi
  * ระบบปฏิบัติการ: Raspberry Pi OS Lite (Debian Linux) 64-bit
  * ซอฟต์แวร์สแต็ก: Node.js, Mosquitto MQTT Broker, Nginx / Web Server

### 2.2 โมดูลและอุปกรณ์เชื่อมต่อสัญญาณ (Signal & Interface Modules)
* **โมดูลแปลงสัญญาณ TTL to RS-485:** ชิป MAX13487 หรือ MAX485 มี TVS Diode ป้องกันแรงดันกระชาก
* **Optocoupler Isolation Module (24VDC to 3.3V Logic):** ป้องกันสัญญาณรบกวนทางไฟฟ้าจากคอยล์แมกเนติก
* **I/O Expander PCF8574:** สื่อสารผ่าน I2C ขับคอยล์รีเลย์ 8 ช่อง
* **DC-DC Step-Down Converter:** 24VDC to 5VDC 3A และ 5VDC to 3.3VDC

### 2.3 ชุดหัวโพรบเซนเซอร์อุตสาหกรรม (Modbus RTU RS-485)
* **pH / Temperature Sensor:** ย่านวัด 0.00 – 14.00 pH, Junction PTFE ทนสารเคมี, Resolution 0.01 pH
* **Dissolved Oxygen (DO) Sensor (Optical Luminescence):** ย่านวัด 0.00 – 20.00 mg/L, ทนทานต่อก๊าซไข่เน่า $H_2S$
* **ORP (Oxidation Reduction Potential) Sensor:** ย่านวัด -1000 ถึง +1000 mV, หัววัด Platinum Pin ตรวจวัดคลอรีน
* **Turbidity Sensor:** ย่านวัด 0 – 1000 NTU, มาตรฐาน ISO 7027 มีระบบใบปัดคราบตะกอนอัตโนมัติ (Auto-wiper)
* **Submersible Hydrostatic Level Transmitter:** ย่านวัด 0.00 – 10.00 เมตร, หัวเซนเซอร์สแตนเลส 316L

---

## 3. ตารางกำหนดตำแหน่งขา (Pinout Mapping)

### 3.1 Digital Inputs (Optocoupler 24VDC Logic)
| GPIO | สัญญาณ | อุปกรณ์ต้นทาง | หน้าที่การทำงาน |
| :---: | :---: | :---: | :--- |
| **GPIO 32** | DI1 (FS-H) | สวิตช์ลูกลอยระดับสูง | ตรวจจับน้ำแตะระดับ 90% (6.30 m) สั่งสตาร์ทปั๊มสูบ |
| **GPIO 33** | DI2 (FS-L) | สวิตช์ลูกลอยระดับต่ำ | ตรวจจับน้ำลดลงแตะระดับ 80% (5.60 m) สั่งหยุดปั๊มสูบ |
| **GPIO 25** | DI3 (OL1_TRIP) | Thermal Overload OL1 | สถานะทริปของปั๊มสูบน้ำเสีย P1 |
| **GPIO 26** | DI4 (OL2_TRIP) | Thermal Overload OL2 | สถานะทริปของเครื่องเติมอากาศ P2 |
| **GPIO 27** | DI5 (PHASE_OK) | Phase Protection Relay | ตรวจสอบแรงดันไฟฟ้า 3 เฟส 380V สมดุล |
| **GPIO 14** | DI6 (FLOW_PULSE) | Flow Meter Pulse Out | สัญญาณพัลส์มาตรวัดน้ำ (1 Pulse = 10 ลิตร) |
| **GPIO 13** | DI7 (E-STOP) | ปุ่มกดหยุดฉุกเฉิน E-Stop | สัญญาณ Safety (Active Low / ปกติ 24V) |
| **GPIO 15** | DI8 (AUTO_SEL) | Selector Switch หน้าตู้ | ตรวจสอบว่าตู้เปิดโหมด AUTO หรือไม่ |

### 3.2 Digital Outputs (Relay Driver ผ่าน PCF8574 I2C: 0x20)
| ขา PCF8574 | สัญญาณ | ขับคอยล์แมกเนติก | พิกัดโหลด |
| :---: | :---: | :---: | :--- |
| **P0** | DO1 (RUN_P1) | แมกเนติก KM1 | ปั๊มน้ำเสียเข้าบ่อปรับสภาพ (1.5 kW, 42 m³/hr) |
| **P1** | DO2 (RUN_P2) | แมกเนติก KM2 | เครื่องเติมอากาศ Aerator (5.5 kW) |
| **P2** | DO3 (RUN_P3) | แมกเนติก KM3 | ปั๊มสูบตะกอนย้อนกลับ Sludge Return (0.75 kW) |
| **P3** | DO4 (RUN_P4) | แมกเนติก KM4 | ปั๊มสูบน้ำทิ้ง Discharge Pump (1.5 kW) |
| **P4** | DO5 (RUN_P5) | แมกเนติก KM5 | ปั๊มจ่ายสารละลายคลอรีน Chlorine Dosing Pump |
| **P5** | DO6 (ALARM) | สัญญาณไซเรน & Beacon | ไฟเตือนหน้าตู้และเสียงสัญญาณแจ้งเตือน |
| **P6** | DO7 (SPARE) | ช่องสำรอง | ติดตั้งเพิ่มเติมในอนาคต |
| **P7** | DO8 (REMOTE_RST) | รีเซ็ตระบบ | สั่งรีเซ็ตสถานะ Alarm ผ่านมือถือ |

### 3.3 Communication & Serial Bus
| ขา ESP32 | หน้าที่ | การเชื่อมต่อไปยัง | หมายเหตุ |
| :---: | :---: | :---: | :--- |
| **GPIO 17** | UART TX | ขา DI ของบอร์ด MAX13487 RS-485 | ส่งคำสั่ง Modbus RTU Polling |
| **GPIO 18** | UART RX | ขา RO ของบอร์ด MAX13487 RS-485 | รับข้อมูลจากชุดเซนเซอร์คุณภาพน้ำ |
| **GPIO 4** | DIR (DE/RE) | ขา DE/RE ของโมดูล RS-485 | ควบคุมทิศทาง (หากใช้ MAX13487 ไม่ต้องต่อ) |
| **GPIO 21** | I2C SDA | ขา SDA ของ PCF8574 และ RTC DS3231 | Bus ส่งข้อมูล |
| **GPIO 22** | I2C SCL | ขา SCL ของ PCF8574 และ RTC DS3231 | Bus สัญญาณนาฬิกา |

---

## 4. หลักการทำงานและการคำนวณทางไฮดรอลิกส์

### 4.1 มิติถังพักน้ำเสีย (Wet Well Dimensions)
* **ขนาดถัง:** กว้าง $3.0\text{ m} \times \text{ยาว } 4.0\text{ m} \times \text{ลึก } 7.0\text{ m}$
* **ปริมาตรสุทธิทั้งหมด:** $3.0 \times 4.0 \times 7.0 = \mathbf{84.0\text{ m}^3}$ ($84,000$ ลิตร)

### 4.2 การคำนวณปริมาตรรอบการสูบ (Pump Cycle Calculation)
* ระดับน้ำเริ่มทำงาน (Start Level): $90\%$ ของความลึกถัง = $7.0 \times 0.90 = \mathbf{6.30\text{ m}}$
* ระดับน้ำหยุดทำงาน (Stop Level): $80\%$ ของความลึกถัง = $7.0 \times 0.80 = \mathbf{5.60\text{ m}}$
* ระยะความสูงระดับน้ำที่ลดลงต่อรอบ ($\Delta h$): $6.30 - 5.60 = \mathbf{0.70\text{ m}}$ ($10\%$)
* **ปริมาตรน้ำที่สูบออกต่อ 1 รอบ:**
  $$\text{Volume} = 3.0\text{ m} \times 4.0\text{ m} \times 0.70\text{ m} = \mathbf{8.40\text{ m}^3} = \mathbf{8,400\text{ ลิตร}}$$
* **ระบบตรวจจับการตัดรอบ (Falling Edge Detection):**
  เมื่อสัญญาณปั๊มเปลี่ยนสถานะจาก $1 \rightarrow 0$ (RUN $\rightarrow$ STOP) ระบบจะบวกปริมาตรสะสม $+8,400$ ลิตรทันที และเพิ่มจำนวนรอบการสูบ $+1$ ครั้ง

### 4.3 เกณฑ์มาตรฐานควบคุมคุณภาพน้ำทิ้ง (QA Compliance Matrix)
| พารามิเตอร์ | เกณฑ์มาตรฐานควบคุม | การแปลผลเมื่อออกนอกเกณฑ์ |
| :---: | :---: | :--- |
| **pH** | $5.50 - 9.00$ | เป็นกรดหรือด่างเกินไป อาจส่งผลต่อสิ่งแวดล้อมและท่อระบายน้ำสาธารณะ |
| **DO (ออกซิเจนละลาย)** | $\ge 2.00\text{ mg/L}$ | ประสิทธิภาพการเติมอากาศต่ำ จุลินทรีย์ขาดออกซิเจน เกิดกลิ่นเหม็น |
| **ORP (ศักย์ฆ่าเชื้อ)** | $\ge 650\text{ mV}$ | ปริมาณคลอรีนอิสระไม่เพียงพอ เชื้อโรคและแบคทีเรียไม่ถูกทำลาย |
| **Turbidity (ความขุ่น)** | $\le 20.0\text{ NTU}$ | มีตะกอนแขวนลอยหลุดรอดจากถังตกตะกอน |

---

## 5. ผังวงจรไฟฟ้าและการต่อสาย

### 5.1 ผังวงจรกำลังไฟฟ้า 3 เฟส (Power Circuit)
```text
  380/220 VAC 50Hz (L1, L2, L3, N, PE)
         │
         ├───► Q1 (Main MCCB 100A 3P)
         │       ├──► SPD1 (Surge Protection Device Type 2)
         │       ├──► PM1 (Phase Protection Relay)
         │       ├──► EM1 (Digital Energy Meter ผ่าน Current Transformer CT 100/5A)
         │       │
         │       ├──► Q2 (MCB 16A 3P) ─► KM1 ─► OL1 (Overload) ─► P1 (ปั๊มสูบน้ำเสีย 1.5 kW)
         │       ├──► Q3 (MCB 20A 3P) ─► KM2 ─► OL2 (Overload) ─► P2 (เครื่องเติมอากาศ 5.5 kW)
         │       ├──► Q4 (MCB 10A 3P) ─► KM3 ─► OL3 (Overload) ─► P3 (ปั๊มตะกอนย้อนกลับ 0.75 kW)
         │       ├──► Q5 (MCB 16A 3P) ─► KM4 ─► OL4 (Overload) ─► P4 (ปั๊มน้ำทิ้ง 1.5 kW)
         │       └──► Q6 (MCB 6A 1P)  ─► KM5 ─► OL5 (Overload) ─► P5 (ปั๊มจ่ายสารคลอรีน)
         │
         └───► Q7 (MCB 10A 1P + RCBO) ─► Switching Power Supply 24VDC 5A
                                                  │
                                                  ├──► DC-DC Buck (24V -> 5V 3A) ─► ESP32-S3 & Raspberry Pi
                                                  └──► สาย Safety Bus & Optocoupler Inputs
```

### 5.2 ผังวงจรควบคุมและสื่อสาร (Control & Communication Circuit)
```text
  +----------------------------------------------------------------------------------+
  |                           ESP32-S3 Microcontroller                               |
  |                                                                                  |
  |   [3V3]   [GND]   [GPIO17/TX]   [GPIO18/RX]   [GPIO21/SDA]  [GPIO22/SCL]  [GPIO13] |
  +-----│-------│----------│-------------│--------------│-------------│----------│---+
        │       │          │             │              │             │          │
        │       │    +─────┴─────────────┴─────+  +─────┴─────────────┴─────+    │
        │       │    |  MAX13487 RS-485 Modbus |  | PCF8574 I2C Relay Board |    │
        │       │    |   (Auto-Direction Ctrl) |  |   (Outputs DO1 - DO8)   |    │
        │       │    +─────────────┬───────────+  +─────────────┬───────────+    │
        │       │                  │                            │                │
        │       │         [A+]     │     [B-]                   │ (Coil 24V)     │
        │       │           │      │       │                    ▼                │
        │       │           └──────┼───────┘             [KM1-KM5 Contactor]     │
        │       │                  │                                             │
        │       │       (Shielded Twisted Pair)                                  │
        │       │                  │                                             │
        │       │   +──────────────┼──────────────+                              │
        │       │   |              |              |                              │
        │       │ [pH Sen]      [DO Sen]      [ORP Sen]                          │
        │       │ (ID:1)        (ID:2)        (ID:3)                             │
        │       │                                                                │
        ▼       ▼                                                                │
   Common Ground Star                                   Emergency Stop Circuit ──┘
```

---

## 6. ขั้นตอนการต่อวงจรและ Commissioning

1. **การปฏิบัติตามมาตรฐาน LOTO (Lock Out - Tag Out):**
   * ปลดเบรกเกอร์ต้นทาง ติดป้ายเตือนและคล้องกุญแจความปลอดภัย LOTO
   * ใช้เครื่องวัดโวลต์ดิจิทัลพิสูจน์ทราบว่าไม่มีแรงดันไฟฟ้าหลงเหลือ (Zero Energy Verification)
2. **การต่อระบบกราวด์ (Grounding):**
   * ตอกหลักดินทองแดงลึก $\ge 2.4$ เมตร วัดค่าความต้านทานดินต้องได้ $\le 5\ \Omega$
   * ต่อสายดินจากโครงตู้ ประตูตู้ และขั้ว PE ของทุกอุปกรณ์เข้าสู่ Ground Busbar หลัก
3. **การเดินสายสัญญาณ RS-485:**
   * ใช้สายคู่บิดเกลียวแบบมีชิลด์ Belden 9841 เดินขนานแบบ Daisy-Chain ห้ามต่อแบบแยกกิ่ง (Star)
   * ต่อตัวต้านทาน Termination Resistor $120\ \Omega\ \frac{1}{4}\text{W}$ ที่หัวและท้ายสายบัส
   * ต่อชีลด์สายกราวด์เพียงฝั่งเดียว (One-point Shield Grounding) เพื่อป้องกัน Ground Loop
4. **ขั้นตอนการทดสอบเริ่มเดินระบบ (Commissioning Checklist):**
   * สับเบรกเกอร์ Q7 เปิดไฟระบบควบคุม 24VDC ตรวจสอบไฟสถานะ LED บนบอร์ด ESP32 และ Raspberry Pi
   * ตรวจสอบแรงดันไฟฟ้า 3 เฟสผ่าน Phase Protection Relay (ค่าต้องสมดุล $380\text{V} \pm 10\%$)
   * ทดสอบสวิตช์ลูกลอยระดับน้ำ: ยกลูกลอย FS-H จำลองระดับน้ำ 90% ตรวจสอบว่าปั๊ม KM1 สั่งงาน
   * กดปุ่ม E-Stop ตรวจสอบว่าคอยล์แมกเนติกทุกตัวถูกตัดวงจรทันที

---

## 7. ข้อควรระวังและมาตรการความปลอดภัย

1. **ความปลอดภัยทางไฟฟ้าและหม้อแปลงกระแส (CT Hazard):**
   * **ห้ามปลดหรือเปิดวงจรสาย CT (Current Transformer) ขณะที่ระบบมีโหลดเด็ดขาด** เพราะจะเหนี่ยวนำให้เกิดแรงดันไฟฟ้าแรงสูงระดับหลายพันโวลต์จนเกิดประกายไฟระเบิดและเป็นอันตรายถึงชีวิต
2. **การป้องกันมอเตอร์เสียหายจากการเดินตัวเปล่า (Dry-Run Protection):**
   * หากลูกลอย FS-L สัญญาณหลุดหรือไม่แตะน้ำ ระบบจะหน่วงเวลาไม่เกิน 10 วินาทีและสั่งตัดปั๊มทันทีเพื่อป้องกันซีลกันรั่ว (Mechanical Seal) แตกเสียหายจากความร้อน
3. **ความปลอดภัยในพื้นที่อับอากาศและสารเคมี (Confined Space & Toxic Gases):**
   * บ่อพักน้ำเสียมีก๊าซไข่เน่า ($H_2S$) และมีเทน ($CH_4$) ห้ามบุคคลลงบ่อโดยไม่มีใบอนุญาต (Work Permit) และต้องตรวจวัดก๊าซก่อนลงปฏิบัติงานเสมอ
   * ผู้ดูแลต้องสวมอุปกรณ์ PPE (แว่นตานิรภัย ถุงมือยาง หน้ากากกรองไอระเหย) ขณะผสมและเติมสารละลายคลอรีน

---

## 8. คู่มือซอฟต์แวร์และโค้ดฉบับสมบูรณ์

### 8.1 ESP32-S3 Firmware
ดูไฟล์ซอร์สโค้ดฉบับเต็มในโปรเจกต์:
* สตรีมข้อมูลระดับมิลลิวินาที (ms) ผ่าน RAM Buffer
* แปลงโปรโตคอล Modbus RTU เซนเซอร์ส่งขึ้น Mosquitto MQTT Broker

### 8.2 Raspberry Pi 3 B+ Node.js Analytics
* ตรวจจับรอบปั๊ม Falling Edge ($1 \rightarrow 0$) เพิ่มปริมาตร $+8,400$ ลิตร
* ส่งออกไฟล์ CSV รายวันและรายเดือน (UTF-8 BOM สำหรับ Microsoft Excel)
* บริจด์ข้อมูลขึ้น HiveMQ Cloud เพื่อให้มือถือตรวจสอบได้ทุกที่

### 8.3 Asynchronous Responsive Web Application
* หน้าเว็บ Responsive HTML5/CSS3/JavaScript แบบ Real-time WebSocket/MQTT
* แสดงผลเกจวัดระดับน้ำ, คุณภาพน้ำ, และปุ่มส่งออกรายงาน PDF

### 8.4 Android Native Application
* พัฒนาด้วย Jetpack Compose และ Material Design 3
* บูรณาการ Google Gemini AI (Thinking Mode High) ช่วยวิเคราะห์และตอบคำถาม
* ส่งออกรายงาน PDF มาตรฐานโรงพยาบาลได้ทันทีในตัวแอป
