package com.example.model

/**
 * โครงสร้างข้อมูลการตรวจวัดพารามิเตอร์คุณภาพน้ำแบบเรียลไทม์
 */
data class SensorReading(
    val timestampMs: Long = System.currentTimeMillis(),
    val waterLevelM: Float = 6.15f,     // ระดับน้ำในบ่อ 0.00 - 7.00 m
    val ph: Float = 7.35f,              // ความเป็นกรด-ด่าง 5.50 - 9.00
    val dissolvedOxygenMgL: Float = 3.82f, // ออกซิเจนละลาย ≥ 2.00 mg/L
    val orpMv: Float = 685f,            // ศักย์ฆ่าเชื้อคลอรีน ≥ 650 mV
    val turbidityNtu: Float = 14.5f,    // ความขุ่น ≤ 20.0 NTU
    val temperatureC: Float = 28.5f,    // อุณหภูมิน้ำเสีย °C
    val powerKw: Float = 7.2f,          // กำลังไฟฟ้าตู้ MDB
    val flowRateM3h: Float = 42.0f,     // อัตราการสูบ m³/ชม.
    val isPumpP1Running: Boolean = true,// ปั๊มน้ำเสียเข้าบ่อ (KM1)
    val isPumpP2Running: Boolean = true,// เครื่องเติมอากาศ (KM2, 5.5kW)
    val isPumpP3Running: Boolean = false,// ปั๊มตะกอนย้อนกลับ (KM3)
    val isPumpP4Running: Boolean = true,// ปั๊มน้ำทิ้ง (KM4)
    val isPumpP5Running: Boolean = true // ปั๊มจ่ายสารคลอรีน (KM5)
)

/**
 * บันทึกประวัติรอบการสูบน้ำ (+8,400 ลิตร ต่อ 1 รอบ)
 */
data class PumpCycleRecord(
    val cycleId: Long,
    val timeFormatted: String,
    val startLevelM: Float = 6.30f,    // เริ่มสูบที่ 90%
    val stopLevelM: Float = 5.60f,     // หยุดสูบที่ 80%
    val deltaHeightM: Float = 0.70f,   // ความสูงลดลง 0.70 m
    val volumeLitersAdded: Int = 8400, // 8,400 ลิตร (8.40 m³)
    val cumulativeDailyLiters: Long,
    val qaComplianceStatus: String = "PASS"
)

/**
 * การประเมินคุณภาพน้ำตามเกณฑ์สาธารณสุข
 */
data class QAEvaluation(
    val isCompliant: Boolean = true,
    val statusText: String = "PASS ผ่านเกณฑ์มาตรฐาน",
    val violations: List<String> = emptyList(),
    val summaryNote: String = "พารามิเตอร์น้ำทิ้งทั้งหมดอยู่ในเกณฑ์มาตรฐานควบคุมสาธารณสุข"
)

enum class ControlMode {
    AUTO,
    MANUAL,
    REMOTE
}

data class CabinetStatus(
    val controlMode: ControlMode = ControlMode.AUTO,
    val isEmergencyStopped: Boolean = false,
    val isPhaseOk: Boolean = true,
    val isOverload1Trip: Boolean = false,
    val isOverload2Trip: Boolean = false,
    val isDryRunAlert: Boolean = false,
    val cyclesToday: Int = 14,
    val litersToday: Long = 117600L,
    val monthlyM3: Float = 2856.0f
)

/**
 * ข้อความการสนทนากับ Gemini AI วิศวกรผู้เชี่ยวชาญ
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val thinkingSteps: String? = null
)

enum class MessageSender {
    USER,
    GEMINI_ENGINEER
}

/**
 * หัวข้อในคู่มือตู้ควบคุม (OP Manual Rev2)
 */
data class ManualSection(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val contentMarkdown: String,
    val keywords: List<String>
)

/**
 * ตารางตรวจเช็คและแก้ไขปัญหาหน้างาน
 */
data class TroubleshootingItem(
    val symptom: String,
    val possibleCauses: List<String>,
    val correctiveActions: List<String>,
    val urgencyLevel: String // CRITICAL, WARNING, INFO
)

/**
 * ข้อมูลตำแหน่งขา Pin Mapping
 */
data class PinDefinition(
    val pinName: String,
    val gpio: String,
    val signalName: String,
    val connectedDevice: String,
    val description: String,
    val electricalSpec: String
)
