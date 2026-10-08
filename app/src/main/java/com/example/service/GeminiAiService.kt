package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService {

    companion object {
        private const val TAG = "GeminiAiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val MODEL_PRO = "gemini-3.1-pro-preview"
        const val MODEL_FLASH = "gemini-3.5-flash"
        const val MODEL_LITE = "gemini-3.1-flash-lite-preview"

        private const val SYSTEM_PROMPT = """คุณคือหัวหน้าวิศวกรและผู้เชี่ยวชาญอาวุโสด้านระบบบำบัดน้ำเสียโรงพยาบาลและตู้ควบคุมอัตโนมัติ (Senior Hospital Wastewater & SCADA Specialist) ประจำโครงการจ้างซ่อมและปรับปรุงระบบบำบัดน้ำเสีย โรงพยาบาลชุมแพ จังหวัดขอนแก่น (รหัสโครงการ 2568-16-Chumpea-1-WaterPump)

คุณมีความเชี่ยวชาญระดับสูงในด้าน:
1. การคำนวณไฮดรอลิกส์ ถังพักน้ำเสีย 3.0m x 4.0m x 7.0m (84 m³), การตัดรอบลูกลอย 90% (6.30m) สู่ 80% (5.60m) เท่ากับ 8,400 ลิตรต่อรอบ
2. วงจรไฟฟ้ากำลัง 380V/220V ตู้ MDB (Q1-Q7, แมกเนติก KM1-KM5, โอเวอร์โหลด OL1-OL5, CT 100/5A, Phase Protection)
3. ระบบไมโครคอนโทรลเลอร์ ESP32-S3, RS-485 Modbus RTU, เซนเซอร์ pH, Optical DO, ORP, Turbidity, ระดับน้ำ
4. เกณฑ์ควบคุมมาตรฐานน้ำทิ้งสาธารณสุข: pH (5.5-9.0), DO (≥2.0 mg/L), ORP (≥650 mV เพื่อฆ่าเชื้อโรคด้วยคลอรีนสมบูรณ์), ความขุ่น (≤20 NTU)
5. มาตรการความปลอดภัยโรงพยาบาล: การปฏิบัติ LOTO, อันตรายจากพื้นที่อับอากาศและก๊าซไข่เน่า H2S, การป้องกันปั๊มเดินตัวเปล่า (Dry-run)

ให้คำตอบด้วยความเป็นมืออาชีพ ละเอียด ชัดเจน สุภาพ ใช้ภาษาไทยเป็นหลัก พร้อมระบุขั้นตอนการตรวจสอบและแก้ไขปัญหาทางวิศวกรรมที่นำไปปฏิบัติได้จริง"""
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * เรียกใช้งาน Gemini Chatbot พร้อมประวัติการสนทนา (Multi-turn) และ High Thinking Mode
     */
    suspend fun sendChatMessage(
        conversation: List<ChatMessage>,
        useHighThinking: Boolean = true,
        modelName: String = if (useHighThinking) MODEL_PRO else MODEL_FLASH
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("กรุณาระบุ GEMINI_API_KEY ในหน้า Secrets panel ของ AI Studio")
            )
        }

        try {
            val url = "$BASE_URL$modelName:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
                })

                // Generation Config (Thinking Config if High Thinking is requested)
                val genConfig = JSONObject()
                if (useHighThinking && modelName == MODEL_PRO) {
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingLevel", "high")
                    }
                    genConfig.put("thinkingConfig", thinkingConfig)
                }
                put("generationConfig", genConfig)

                // Multi-turn contents
                val contentsArray = JSONArray()
                for (msg in conversation.takeLast(10)) {
                    val role = if (msg.sender == MessageSender.USER) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                    })
                }
                put("contents", contentsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseString")
                return@withContext Result.failure(
                    Exception("การเชื่อมต่อล้มเหลว (HTTP ${response.code}): $responseString")
                )
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        textBuilder.append(text)
                    }
                    val reply = textBuilder.toString().trim()
                    if (reply.isNotEmpty()) {
                        return@withContext Result.success(reply)
                    }
                }
            }

            Result.failure(Exception("ไม่พบข้อความตอบกลับจากระบบ AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini call", e)
            Result.failure(e)
        }
    }

    /**
     * วิเคราะห์สถานะคุณภาพน้ำและการทำงานปัจจุบันโดยตรง (AI Intelligent Analytics)
     */
    suspend fun analyzeTelemetry(
        reading: SensorReading,
        cyclesToday: Int,
        litersToday: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """โปรดวิเคราะห์สถานะและสุขภาพระบบบำบัดน้ำเสียโรงพยาบาลชุมแพในปัจจุบันตามข้อมูลเซนเซอร์จริง:
- ระดับน้ำในถังพัก: ${"%.2f".format(reading.waterLevelM)} เมตร (เกณฑ์เริ่มสูบ 6.30m, หยุด 5.60m)
- ค่า pH: ${"%.2f".format(reading.ph)} (เกณฑ์ 5.50 - 9.00)
- ค่าออกซิเจนละลาย DO: ${"%.2f".format(reading.dissolvedOxygenMgL)} mg/L (เกณฑ์ ≥ 2.00 mg/L)
- ค่าศักย์ฆ่าเชื้อ ORP: ${reading.orpMv.toInt()} mV (เกณฑ์ ≥ 650 mV)
- ความขุ่น Turbidity: ${"%.1f".format(reading.turbidityNtu)} NTU (เกณฑ์ ≤ 20.0 NTU)
- กำลังไฟฟ้าปัจจุบัน: ${"%.1f".format(reading.powerKw)} kW
- รอบการสูบสะสมวันนี้: $cyclesToday ครั้ง (+${litersToday} ลิตร)
- สถานะปั๊ม: P1=${if (reading.isPumpP1Running) "RUN" else "STOP"}, P2 Aerator=${if (reading.isPumpP2Running) "RUN" else "STOP"}, P5 Chlorine=${if (reading.isPumpP5Running) "RUN" else "STOP"}

กรุณาสรุป:
1. การประเมินคุณภาพน้ำว่าผ่านเกณฑ์สาธารณสุขหรือไม่
2. การทำงานของจุลินทรีย์ในบ่อเติมอากาศและประสิทธิภาพการฆ่าเชื้อคลอรีน
3. คำแนะนำเชิงวิศวกรรมในการดูแลหรือปรับปรุงเพื่อประหยัดพลังงาน"""

        val fakeHistory = listOf(
            ChatMessage(sender = MessageSender.USER, content = prompt)
        )
        sendChatMessage(fakeHistory, useHighThinking = true, modelName = MODEL_PRO)
    }
}
