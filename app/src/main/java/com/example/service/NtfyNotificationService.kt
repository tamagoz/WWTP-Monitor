package com.example.service

import android.util.Log
import com.example.model.CabinetStatus
import com.example.model.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NtfyNotificationService {

    companion object {
        private const val TAG = "NtfyService"
        const val DEFAULT_TOPIC = "chumphae_wwtp_alerts"
        const val NTFY_BASE_URL = "https://ntfy.sh/"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss น.", Locale("th", "TH"))

    /**
     * ส่งข้อความแจ้งเตือนผ่าน App NTFY เมื่อค่าผิดปกติ
     */
    suspend fun sendAlert(
        topic: String,
        title: String,
        message: String,
        priority: String = "urgent", // urgent, high, default
        tags: String = "warning,rotating_light,hospital"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val safeTopic = topic.trim().ifBlank { DEFAULT_TOPIC }
        val url = "$NTFY_BASE_URL$safeTopic"

        try {
            val requestBody = message.toRequestBody("text/plain; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Title", title)
                .header("Priority", priority)
                .header("Tags", tags)
                .build()

            val response = httpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()

            if (isSuccess) {
                Log.d(TAG, "Sent NTFY alert to topic $safeTopic successfully")
                Result.success(true)
            } else {
                Log.e(TAG, "Failed to send NTFY: HTTP ${response.code}")
                Result.failure(Exception("HTTP error ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending NTFY alert", e)
            Result.failure(e)
        }
    }

    /**
     * ส่งรายงานสรุปประจำรอบเวลา (1 - 24 ชั่วโมง)
     */
    suspend fun sendPeriodicReport(
        topic: String,
        reading: SensorReading,
        status: CabinetStatus,
        intervalHours: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val safeTopic = topic.trim().ifBlank { DEFAULT_TOPIC }
        val nowStr = timeFormat.format(Date())

        val title = "📊 [WWTP สรุปผลรอบ ${intervalHours} ชม.] รพ.ชุมแพ"
        val message = """รายงานสรุปสถานะระบบบำบัดน้ำเสีย รพ.ชุมแพ
รอบเวลา: ทุกๆ $intervalHours ชั่วโมง (ณ $nowStr)

💧 ข้อมูลทางไฮดรอลิกส์:
• รอบการสูบสะสมวันนี้: ${status.cyclesToday} ครั้ง
• ปริมาณน้ำบำบัดวันนี้: ${String.format("%,d", status.litersToday)} ลิตร (+8,400 ลิตร/รอบ)
• ปริมาณสะสมประจำเดือน: ${"%.1f".format(status.monthlyM3)} m³ (${String.format("%,d", (status.monthlyM3 * 1000).toLong())} ลิตร)
• ระดับน้ำในถังพัก: ${"%.2f".format(reading.waterLevelM)} เมตร

🧪 คุณภาพน้ำทิ้ง (Modbus Sensors):
• pH: ${"%.2f".format(reading.ph)} (เกณฑ์: 5.50 - 9.00)
• DO: ${"%.2f".format(reading.dissolvedOxygenMgL)} mg/L (เกณฑ์: ≥ 2.00 mg/L)
• ORP: ${reading.orpMv.toInt()} mV (เกณฑ์: ≥ 650 mV)
• ความขุ่น: ${"%.1f".format(reading.turbidityNtu)} NTU (เกณฑ์: ≤ 20.0 NTU)
• กำลังไฟฟ้าตู้ MDB: ${"%.1f".format(reading.powerKw)} kW

⚙️ สถานะตู้ MDB: โหมด ${status.controlMode.name} | ปั๊ม P1=${if (reading.isPumpP1Running) "RUN" else "STOP"} | Aerator P2=${if (reading.isPumpP2Running) "RUN" else "STOP"}
สถานะความปลอดภัย: ${if (status.isEmergencyStopped) "⚠️ E-STOP ACTIVE" else "ปกติ (NORMAL)"}"""

        sendAlert(
            topic = safeTopic,
            title = title,
            message = message,
            priority = "default",
            tags = "bar_chart,droplet,hospital"
        )
    }
}
