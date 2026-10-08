package com.example.data

import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class WwtpRepository(private val scope: CoroutineScope) {

    private val _sensorReading = MutableStateFlow(SensorReading())
    val sensorReading: StateFlow<SensorReading> = _sensorReading.asStateFlow()

    private val _cabinetStatus = MutableStateFlow(CabinetStatus())
    val cabinetStatus: StateFlow<CabinetStatus> = _cabinetStatus.asStateFlow()

    private val _pumpCycles = MutableStateFlow<List<PumpCycleRecord>>(emptyList())
    val pumpCycles: StateFlow<List<PumpCycleRecord>> = _pumpCycles.asStateFlow()

    private val _qaEvaluation = MutableStateFlow(QAEvaluation())
    val qaEvaluation: StateFlow<QAEvaluation> = _qaEvaluation.asStateFlow()

    private val ntfyService = com.example.service.NtfyNotificationService()
    val ntfyTopic = MutableStateFlow(com.example.service.NtfyNotificationService.DEFAULT_TOPIC)
    val ntfyEnabled = MutableStateFlow(true)
    val reportIntervalHours = MutableStateFlow(4) // 1 to 24 hours

    private var lastAlertSentTime = 0L
    private var lastPeriodicReportTime = System.currentTimeMillis()

    private val timeFormat = SimpleDateFormat("HH:mm:ss น.", Locale("th", "TH"))

    init {
        // Initialize seed cycle history
        val initialCycles = mutableListOf<PumpCycleRecord>()
        val baseTime = System.currentTimeMillis()
        var accumulated = 117600L
        for (i in 14 downTo 10) {
            val t = SimpleDateFormat("HH:mm น.", Locale("th", "TH")).format(Date(baseTime - (15 - i) * 3600000L))
            initialCycles.add(
                PumpCycleRecord(
                    cycleId = i.toLong(),
                    timeFormatted = t,
                    startLevelM = 6.30f,
                    stopLevelM = 5.60f,
                    deltaHeightM = 0.70f,
                    volumeLitersAdded = 8400,
                    cumulativeDailyLiters = accumulated
                )
            )
            accumulated -= 8400L
        }
        _pumpCycles.value = initialCycles

        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)

                val currentReading = _sensorReading.value
                val currentStatus = _cabinetStatus.value

                if (currentStatus.isEmergencyStopped) {
                    // When E-Stop is active, pumps are forced OFF
                    _sensorReading.value = currentReading.copy(
                        isPumpP1Running = false,
                        isPumpP2Running = false,
                        isPumpP3Running = false,
                        isPumpP4Running = false,
                        isPumpP5Running = false,
                        powerKw = 0.5f,
                        flowRateM3h = 0.0f
                    )
                    continue
                }

                var newLevel = currentReading.waterLevelM
                var pump1State = currentReading.isPumpP1Running

                if (currentStatus.controlMode == ControlMode.AUTO) {
                    if (pump1State) {
                        // Pumping down (สูบน้ำออกลดระดับลง)
                        newLevel -= 0.015f
                        if (newLevel <= 5.60f) {
                            // Falling edge 1 -> 0: Stop Pump & Register +8,400 Liters
                            newLevel = 5.60f
                            pump1State = false

                            val newCyclesToday = currentStatus.cyclesToday + 1
                            val newLitersToday = currentStatus.litersToday + 8400L
                            val newMonthlyM3 = currentStatus.monthlyM3 + 8.4f

                            _cabinetStatus.value = currentStatus.copy(
                                cyclesToday = newCyclesToday,
                                litersToday = newLitersToday,
                                monthlyM3 = newMonthlyM3
                            )

                            val newCycleRecord = PumpCycleRecord(
                                cycleId = newCyclesToday.toLong(),
                                timeFormatted = timeFormat.format(Date()),
                                startLevelM = 6.30f,
                                stopLevelM = 5.60f,
                                deltaHeightM = 0.70f,
                                volumeLitersAdded = 8400,
                                cumulativeDailyLiters = newLitersToday
                            )
                            _pumpCycles.value = listOf(newCycleRecord) + _pumpCycles.value.take(20)
                        }
                    } else {
                        // Filling up (น้ำเสียจากโรงพยาบาลไหลเข้าบ่อ)
                        newLevel += 0.018f
                        if (newLevel >= 6.30f) {
                            // High level 90%: Start Pump
                            newLevel = 6.30f
                            pump1State = true
                        }
                    }
                }

                // Micro fluctuations on analog sensors (Modbus jitter)
                val ms = System.currentTimeMillis()
                val newPh = (7.32f + sin(ms / 20000.0) * 0.12f).toFloat()
                val newDo = (3.85f + cos(ms / 15000.0) * 0.22f).toFloat()
                val newOrp = (682f + sin(ms / 25000.0) * 14f).toFloat()
                val newTurb = (14.2f + (ms % 5) * 0.25f).toFloat()
                val newPower = if (pump1State) 7.2f + (ms % 3) * 0.15f else 5.5f

                _sensorReading.value = currentReading.copy(
                    timestampMs = ms,
                    waterLevelM = newLevel,
                    ph = newPh,
                    dissolvedOxygenMgL = newDo,
                    orpMv = newOrp,
                    turbidityNtu = newTurb,
                    powerKw = newPower,
                    flowRateM3h = if (pump1State) 42.0f else 0.0f,
                    isPumpP1Running = pump1State
                )

                // Update QA Compliance
                evaluateQa(newPh, newDo, newOrp, newTurb)
            }
        }
    }

    private fun evaluateQa(ph: Float, doVal: Float, orp: Float, turb: Float) {
        val violations = mutableListOf<String>()
        if (ph < 5.50f || ph > 9.00f) violations.add("ค่า pH นอกเกณฑ์ ($ph)")
        if (doVal < 2.00f) violations.add("ออกซิเจน DO ต่ำกว่าเกณฑ์ ($doVal mg/L)")
        if (orp < 650f) violations.add("ศักย์ฆ่าเชื้อ ORP ต่ำกว่าเกณฑ์ ($orp mV)")
        if (turb > 20.0f) violations.add("ความขุ่นสูงกว่าเกณฑ์ ($turb NTU)")

        if (violations.isEmpty()) {
            _qaEvaluation.value = QAEvaluation(
                isCompliant = true,
                statusText = "PASS ผ่านเกณฑ์มาตรฐาน",
                violations = emptyList(),
                summaryNote = "พารามิเตอร์น้ำทิ้งทั้งหมดอยู่ในเกณฑ์มาตรฐานควบคุมสาธารณสุข"
            )
        } else {
            _qaEvaluation.value = QAEvaluation(
                isCompliant = false,
                statusText = "FAIL ผิดปกติ (${violations.size} รายการ)",
                violations = violations,
                summaryNote = violations.joinToString(" | ")
            )

            // Auto-send NTFY push alert when parameter exceeds threshold (debounced by 60s)
            val now = System.currentTimeMillis()
            if (ntfyEnabled.value && (now - lastAlertSentTime > 60000L)) {
                lastAlertSentTime = now
                val alertMsg = "⚠️ ตรวจพบค่าพารามิเตอร์ออกนอกเกณฑ์มาตรฐานควบคุม รพ.ชุมแพ:\n" +
                        violations.joinToString("\n• ") +
                        "\n\nระดับน้ำ: ${"%.2f".format(_sensorReading.value.waterLevelM)}m | เวลา: ${timeFormat.format(Date())}"
                scope.launch(Dispatchers.IO) {
                    ntfyService.sendAlert(
                        topic = ntfyTopic.value,
                        title = "🚨 [WWTP ALERT] คุณภาพน้ำผิดปกติ รพ.ชุมแพ",
                        message = alertMsg,
                        priority = "urgent",
                        tags = "warning,hospital,water_drop"
                    )
                }
            }
        }
    }

    suspend fun testSendNtfy(customTopic: String? = null): Result<Boolean> {
        val topic = customTopic ?: ntfyTopic.value
        val testMsg = "✅ ทดสอบระบบส่งข้อความแจ้งเตือนผ่าน App NTFY สำเร็จ!\nระบบตู้ควบคุมบำบัดน้ำเสีย รพ.ชุมแพ (2568-16-Chumpea-1-WaterPump) เชื่อมต่อพร้อมทำงาน"
        return ntfyService.sendAlert(
            topic = topic,
            title = "🔔 [WWTP TEST] ทดสอบการเชื่อมต่อ NTFY",
            message = testMsg,
            priority = "high",
            tags = "white_check_mark,bell,hospital"
        )
    }

    suspend fun sendPeriodicReportNow(): Result<Boolean> {
        return ntfyService.sendPeriodicReport(
            topic = ntfyTopic.value,
            reading = _sensorReading.value,
            status = _cabinetStatus.value,
            intervalHours = reportIntervalHours.value
        )
    }

    fun setNtfyTopic(topic: String) {
        ntfyTopic.value = topic.trim()
    }

    fun setNtfyEnabled(enabled: Boolean) {
        ntfyEnabled.value = enabled
    }

    fun setReportIntervalHours(hours: Int) {
        reportIntervalHours.value = hours.coerceIn(1, 24)
    }

    // Interactive Controls
    fun setControlMode(mode: ControlMode) {
        _cabinetStatus.value = _cabinetStatus.value.copy(controlMode = mode)
    }

    fun triggerEmergencyStop() {
        _cabinetStatus.value = _cabinetStatus.value.copy(isEmergencyStopped = true)
    }

    fun resetEmergencyStop() {
        _cabinetStatus.value = _cabinetStatus.value.copy(isEmergencyStopped = false)
    }

    fun togglePump1Manual() {
        val current = _sensorReading.value.isPumpP1Running
        _sensorReading.value = _sensorReading.value.copy(isPumpP1Running = !current)
    }

    // Static Technical Documentation & FAQ Data
    fun getManualSections(): List<ManualSection> = listOf(
        ManualSection(
            id = "specs",
            title = "1. ข้อมูลจำเพาะของบอร์ดควบคุม (Hardware Specs)",
            category = "ฮาร์ดแวร์",
            summary = "รายละเอียด ESP32-S3, Raspberry Pi 3 B+, RS-485 และเซนเซอร์อุตสาหกรรม",
            keywords = listOf("esp32", "raspberry pi", "sram", "cpu", "สเปก", "ฮาร์ดแวร์"),
            contentMarkdown = """• **ESP32-S3 DevKitC-1:** Xtensa® 32-bit LX7 Dual-Core 240 MHz, SRAM 512KB, Flash 8MB รองรับ Wi-Fi/BLE และ Hardware UART 3 ช่อง
• **Raspberry Pi 3 Model B+:** ARM Cortex-A53 64-bit 1.4 GHz, RAM 1GB พอร์ต LAN/USB ทำหน้าที่เป็น Data Logging, MQTT Broker และ Analytics Engine
• **โมดูล RS-485:** ชิป MAX13487 แบบ Auto-Direction พร้อม TVS Diode ตัดแรงดันกระชาก
• **ชุดหัวโพรบ Modbus RTU:** pH (0-14), Optical DO (0-20 mg/L), ORP (-1000 ถึง +1000 mV), Turbidity (0-1000 NTU), Submersible Level (0-10m)"""
        ),
        ManualSection(
            id = "hydraulics",
            title = "2. การคำนวณไฮดรอลิกส์ และปริมาตร 8,400 ลิตร",
            category = "ไฮดรอลิกส์",
            summary = "มิติถังพักน้ำเสีย 3m x 4m x 7m และสูตรคำนวณปริมาตรรอบการสูบ",
            keywords = listOf("8400", "ลิตร", "ถังพัก", "ไฮดรอลิกส์", "90%", "80%", "ลูกลอย"),
            contentMarkdown = """• **มิติถังพักน้ำเสีย (Wet Well):** กว้าง 3.0 m x ยาว 4.0 m x ลึก 7.0 m ปริมาตรเต็มถัง = 84.0 m³ (84,000 ลิตร)
• **เงื่อนไขรอบการสูบน้ำ:**
  - สตาร์ทปั๊ม (Start Pump): ที่ระดับ 90% ของความลึก = 6.30 m
  - หยุดปั๊ม (Stop Pump): ที่ระดับ 80% ของความลึก = 5.60 m
  - ระยะลดระดับต่อรอบ (Δh): 0.70 m (10%)
• **สูตรคำนวณปริมาตร:**
  Volume = 3.0m x 4.0m x 0.70m = 8.40 m³ = 8,400 ลิตร ต่อ 1 รอบ
• **ระบบบันทึกแบบ Falling Edge:** เมื่อปั๊มเปลี่ยนสถานะจาก 1 -> 0 (RUN -> STOP) ระบบจะลงบันทึก +8,400 ลิตร และ +1 รอบทันที"""
        ),
        ManualSection(
            id = "pinout",
            title = "3. ตารางกำหนดตำแหน่งขา (Pinout Mapping)",
            category = "วงจรและการต่อสาย",
            summary = "ตำแหน่งขาสัญญาณ DI, DO, Analog และ Communication ของบอร์ดควบคุม",
            keywords = listOf("pin", "gpio", "ขา", "ไดอะแกรม", "optocoupler", "relay"),
            contentMarkdown = """• **Digital Inputs (Optocoupler 24VDC):**
  - GPIO 32: ลูกลอยระดับสูง FS-H (90% = 6.30m)
  - GPIO 33: ลูกลอยระดับต่ำ FS-L (80% = 5.60m)
  - GPIO 25: โอเวอร์โหลดปั๊ม 1 (OL1_TRIP)
  - GPIO 26: โอเวอร์โหลดปั๊ม 2 (OL2_TRIP Aerator)
  - GPIO 27: Phase Protection (PHASE_OK)
  - GPIO 14: Flow Pulse (10 ลิตร/พัลส์)
  - GPIO 13: E-Stop สัญญาณหยุดฉุกเฉิน
• **Digital Outputs (PCF8574 I2C):**
  - P0: DO1 คอยล์แมกเนติก KM1 (ปั๊มน้ำเสีย 1.5 kW)
  - P1: DO2 คอยล์แมกเนติก KM2 (เครื่องเติมอากาศ 5.5 kW)
  - P2: DO3 คอยล์แมกเนติก KM3 (ปั๊มตะกอนย้อนกลับ 0.75 kW)
  - P3: DO4 คอยล์แมกเนติก KM4 (ปั๊มน้ำทิ้ง 1.5 kW)
  - P4: DO5 คอยล์แมกเนติก KM5 (ปั๊มคลอรีน)
  - P5: DO6 สัญญาณ Alarm Siren & Beacon"""
        ),
        ManualSection(
            id = "safety",
            title = "4. ข้อควรระวังและมาตรการความปลอดภัยในโรงพยาบาล",
            category = "ความปลอดภัย",
            summary = "ความปลอดภัยทางไฟฟ้า LOTO, ก๊าซพิษพื้นที่อับอากาศ และสารคลอรีน",
            keywords = listOf("ความปลอดภัย", "loto", "พื้นที่อับอากาศ", "ก๊าซไข่เน่า", "คลอรีน", "ct"),
            contentMarkdown = """• **มาตรฐาน LOTO (Lock Out - Tag Out):** ปลดเบรกเกอร์ คล้องกุญแจ แขวนป้าย และวัดพิสูจน์แรงดันศูนย์ก่อนเปิดตู้ MDB ทุกครั้ง
• **อันตรายจาก CT (Current Transformer):** ห้ามปลดสายทุติยภูมิของ CT ออกขณะมีไฟเด็ดขาด เพราะจะเกิดแรงดันสูงเหนี่ยวนำระเบิดเป็นอันตรายถึงชีวิต
• **พื้นที่อับอากาศและก๊าซไข่เน่า (H2S):** ห้ามลงบ่อพักน้ำเสียโดยไม่มีใบอนุญาต (Work Permit) และเครื่องตรวจวัดก๊าซเด็ดขาด
• **การป้องกันปั๊มแห้ง (Dry-Run):** หากระดับน้ำลดต่ำกว่า FS-L เกิน 10 วินาที ปั๊มต้องตัดทันทีเพื่อรักษา Mechanical Seal"""
        ),
        ManualSection(
            id = "troubleshoot",
            title = "5. แนวทางแก้ไขปัญหาเบื้องต้นหน้างาน (Troubleshooting)",
            category = "การแก้ไขปัญหา",
            summary = "ขั้นตอนตรวจเช็คอาการเสีย ปั๊มไม่ตัด, Overload Trip, ค่าเซนเซอร์ไม่ขึ้น",
            keywords = listOf("แก้ไขปัญหา", "overload", "ปั๊มไม่ตัด", "rs485", "ซ่อม"),
            contentMarkdown = """1. **ปั๊มไม่ตัดเมื่อน้ำลด:** สับสวิตช์หน้าตู้ไปที่ OFF ทันที ตรวจสอบลูกลอย FS-L ทำความสะอาดคราบไขมัน
2. **Overload Trip ติดค้าง:** สับเบรกเกอร์ลง ตรวจสอบเศษขยะพันใบพัด และกดปุ่ม Reset สีฟ้าบน Thermal Overload Relay
3. **ค่าเซนเซอร์ Modbus หาย/เป็น 0:** ตรวจสอบสายชีลด์ RS-485 ขั้ว A-B และวัดแรงดันบัส (ต้องได้ 2.5V - 3.3V)
4. **ไฟ Phase Failure เตือน:** วัดแรงดันไฟเมน 3 เฟส 380V ว่ามีเฟสตกหรือไม่ หากเกิดจากหม้อแปลงโรงพยาบาล ให้แจ้งกองช่างทันที"""
        )
    )

    fun getTroubleshootingItems(): List<TroubleshootingItem> = listOf(
        TroubleshootingItem(
            symptom = "ปั๊มสูบน้ำเสีย P1 ไม่ยอมทำงานเมื่อน้ำแตะระดับ 90%",
            possibleCauses = listOf("สวิตช์หน้าตู้ไม่ได้อยู่ที่ AUTO", "ลูกลอย FS-H ติดค้างคราบขยะ", "สายสัญญาณ GPIO 32 หลุดหลวม"),
            correctiveActions = listOf(
                "ตรวจเช็คสวิตช์หน้าตู้ให้อยู่ตำแหน่ง AUTO",
                "ใช้มัลติมิเตอร์วัดแรงดันขั้ว FS-H ต้องได้ 24VDC เมื่อลอยขึ้น",
                "ทำความสะอาดคราบสิ่งปฏิกูลที่พันลูกลอย"
            ),
            urgencyLevel = "WARNING"
        ),
        TroubleshootingItem(
            symptom = "ปั๊มไม่ยอมตัดการทำงาน น้ำลดลงต่ำกว่า 80% แล้ว",
            possibleCauses = listOf("ลูกลอย FS-L จมติดขัด", "หน้าสัมผัสรีเลย์ DO1 อาร์คค้าง", "การตั้งค่าหน่วงเวลาผิดพลาด"),
            correctiveActions = listOf(
                "สับสวิตช์หน้าตู้ไปที่ OFF ทันทีเพื่อป้องกัน Dry-Run",
                "ทดสอบขยับลูกลอย FS-L ด้วยมือและวัดสัญญาณที่ขั้ว Opto",
                "ตรวจสอบหน้าสัมผัสของ Magnetic Contactor KM1"
            ),
            urgencyLevel = "CRITICAL"
        ),
        TroubleshootingItem(
            symptom = "สัญญาณเตือน Overload Trip ติดค้าง (ไฟสีส้มหน้าตู้)",
            possibleCauses = listOf("มอเตอร์กินกระแสเกินพิกัด", "มีเศษผ้าหรือขยะอุดตันในใบพัด", "ลูกปืนมอเตอร์ฝืดแตก"),
            correctiveActions = listOf(
                "สับเบรกเกอร์ปั๊มลง ดำเนินการ LOTO ล็อกกุญแจ",
                "เปิดฝาครอบท่อดูดเพื่อขจัดสิ่งอุดตัน",
                "วัดความต้านทานขดลวดมอเตอร์ Megger Test",
                "กดปุ่ม Blue Reset บนตัว Thermal Overload Relay"
            ),
            urgencyLevel = "CRITICAL"
        ),
        TroubleshootingItem(
            symptom = "ค่าเซนเซอร์ pH หรือ DO ไม่อัปเดต / ค้างอยู่ที่ค่าเดิม",
            possibleCauses = listOf("สายบัส RS-485 หลวมหรือหลุด", "หัวโพรบสกปรกมี Biofilm เมือกเกาะ", "ไฟเลี้ยง 24V เซนเซอร์ตก"),
            correctiveActions = listOf(
                "ตรวจสอบไฟกระพริบ RX/TX บนโมดูล MAX13487",
                "วัดแรงดันระหว่างขั้ว A และ B ต้องได้ประมาณ 2.5V - 3.3V",
                "นำหัววัดขึ้นมาล้างด้วยน้ำสะอาดและ Calibrate ค่าใหม่"
            ),
            urgencyLevel = "WARNING"
        )
    )

    fun getPinDefinitions(): List<PinDefinition> = listOf(
        PinDefinition("GPIO 32", "GPIO32", "DI1 (FS-H)", "ลูกลอยระดับสูง", "ตรวจจับน้ำ 90% (6.30m) สั่งสตาร์ทปั๊ม", "24VDC Opto Isolated"),
        PinDefinition("GPIO 33", "GPIO33", "DI2 (FS-L)", "ลูกลอยระดับต่ำ", "ตรวจจับน้ำ 80% (5.60m) สั่งหยุดปั๊ม", "24VDC Opto Isolated"),
        PinDefinition("GPIO 25", "GPIO25", "DI3 (OL1_TRIP)", "Thermal Overload 1", "สถานะทริปของปั๊มสูบน้ำเสีย P1", "24VDC Contact NC"),
        PinDefinition("GPIO 26", "GPIO26", "DI4 (OL2_TRIP)", "Thermal Overload 2", "สถานะทริปของเครื่องเติมอากาศ P2", "24VDC Contact NC"),
        PinDefinition("GPIO 27", "GPIO27", "DI5 (PHASE_OK)", "Phase Protection Relay", "ตรวจเช็คไฟฟ้า 3 เฟส 380V สมดุล", "24VDC Contact NO"),
        PinDefinition("GPIO 14", "GPIO14", "DI6 (FLOW_PULSE)", "มาตรวัดน้ำ Flow Meter", "รับสัญญาณพัลส์ (1 Pulse = 10 ลิตร)", "Dry Contact Pulse"),
        PinDefinition("GPIO 13", "GPIO13", "DI7 (E-STOP)", "ปุ่มกดหยุดฉุกเฉิน", "Safety Bus ตัดวงจรคอยล์แมกเนติก", "24VDC Safety Loop"),
        PinDefinition("GPIO 17", "GPIO17", "TXD (RS485)", "โมดูล MAX13487", "ส่งคำสั่ง Modbus RTU Polling", "3.3V TTL to RS-485"),
        PinDefinition("GPIO 18", "GPIO18", "RXD (RS485)", "โมดูล MAX13487", "รับข้อมูลเซนเซอร์คุณภาพน้ำ", "3.3V TTL to RS-485"),
        PinDefinition("GPIO 21", "GPIO21", "I2C SDA", "PCF8574 & DS3231", "ดาต้าไลน์บัส I2C ควบคุมรีเลย์", "3.3V I2C Bus"),
        PinDefinition("GPIO 22", "GPIO22", "I2C SCL", "PCF8574 & DS3231", "สัญญาณนาฬิกาบัส I2C", "3.3V I2C Bus")
    )
}
