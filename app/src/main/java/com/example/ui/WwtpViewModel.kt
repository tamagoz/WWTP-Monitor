package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WwtpRepository
import com.example.model.*
import com.example.service.GeminiAiService
import com.example.service.PdfReportGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class WwtpViewModel : ViewModel() {

    private val repository = WwtpRepository(viewModelScope)
    private val geminiService = GeminiAiService()

    val sensorReading = repository.sensorReading
    val cabinetStatus = repository.cabinetStatus
    val pumpCycles = repository.pumpCycles
    val qaEvaluation = repository.qaEvaluation

    // Chat with Gemini AI Engineer
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.GEMINI_ENGINEER,
                content = "สวัสดีครับ ผมคือผู้เชี่ยวชาญวิศวกรรมระบบบำบัดน้ำเสียและตู้ควบคุมอัตโนมัติ รพ.ชุมแพ คุณสามารถสอบถามข้อมูลเทคนิค วิเคราะห์พารามิเตอร์น้ำ หรือปรึกษาแนวทางแก้ไขปัญหาตู้ควบคุมได้ตลอดเวลาครับ"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    val manualSections = _searchKeyword.map { query ->
        val all = repository.getManualSections()
        if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter { section ->
                section.title.lowercase().contains(q) ||
                section.summary.lowercase().contains(q) ||
                section.contentMarkdown.lowercase().contains(q) ||
                section.keywords.any { it.lowercase().contains(q) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, repository.getManualSections())

    val troubleshootingItems = repository.getTroubleshootingItems()
    val pinDefinitions = repository.getPinDefinitions()

    // NTFY Notification & Periodic Report (1 - 24 hours)
    val ntfyTopic = repository.ntfyTopic
    val ntfyEnabled = repository.ntfyEnabled
    val reportIntervalHours = repository.reportIntervalHours

    fun setNtfyTopic(topic: String) {
        repository.setNtfyTopic(topic)
    }

    fun setNtfyEnabled(enabled: Boolean) {
        repository.setNtfyEnabled(enabled)
    }

    fun setReportIntervalHours(hours: Int) {
        repository.setReportIntervalHours(hours)
    }

    fun testSendNtfy(context: Context) {
        viewModelScope.launch {
            val result = repository.testSendNtfy()
            result.onSuccess {
                Toast.makeText(context, "ส่งแจ้งเตือน NTFY สำเร็จ! ตรวจสอบที่แอป NTFY (หัวข้อ: ${ntfyTopic.value})", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, "ส่งแจ้งเตือน NTFY ไม่สำเร็จ: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sendPeriodicReportNow(context: Context) {
        viewModelScope.launch {
            val result = repository.sendPeriodicReportNow()
            result.onSuccess {
                Toast.makeText(context, "ส่งรายงานรอบ ${reportIntervalHours.value} ชม. เข้าแอป NTFY เรียบร้อย!", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, "ส่งรายงานไม่สำเร็จ: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setSearchKeyword(query: String) {
        _searchKeyword.value = query
    }

    fun setControlMode(mode: ControlMode) {
        repository.setControlMode(mode)
    }

    fun triggerEmergencyStop() {
        repository.triggerEmergencyStop()
    }

    fun resetEmergencyStop() {
        repository.resetEmergencyStop()
    }

    fun togglePump1() {
        repository.togglePump1Manual()
    }

    /**
     * ส่งข้อความถาม Gemini AI วิศวกร (พร้อม Thinking Mode: High)
     */
    fun sendChatMessage(userText: String, useHighThinking: Boolean = true) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, content = userText)
        val updated = _chatMessages.value + userMsg
        _chatMessages.value = updated

        viewModelScope.launch {
            _isAiThinking.value = true
            val result = geminiService.sendChatMessage(
                conversation = updated,
                useHighThinking = useHighThinking
            )
            _isAiThinking.value = false

            result.onSuccess { responseText ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.GEMINI_ENGINEER,
                    content = responseText
                )
            }.onFailure { err ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.GEMINI_ENGINEER,
                    content = "⚠️ ไม่สามารถติดต่อระบบ AI ได้: ${err.message}"
                )
            }
        }
    }

    /**
     * ขอให้ Gemini AI วิเคราะห์พารามิเตอร์ปัจจุบันทันที
     */
    fun requestAiDiagnostics() {
        val currentReading = sensorReading.value
        val currentStatus = cabinetStatus.value

        val prompt = "กรุณาวิเคราะห์ค่าพารามิเตอร์คุณภาพน้ำและสถานะเครื่องจักรในขณะนี้ พร้อมให้คำแนะนำเชิงวิศวกรรม"
        val userMsg = ChatMessage(sender = MessageSender.USER, content = prompt)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            val result = geminiService.analyzeTelemetry(
                reading = currentReading,
                cyclesToday = currentStatus.cyclesToday,
                litersToday = currentStatus.litersToday
            )
            _isAiThinking.value = false

            result.onSuccess { responseText ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.GEMINI_ENGINEER,
                    content = responseText
                )
            }.onFailure { err ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.GEMINI_ENGINEER,
                    content = "⚠️ การวิเคราะห์ล้มเหลว: ${err.message}"
                )
            }
        }
    }

    /**
     * ส่งออกรายงาน PDF
     */
    fun exportPdfReport(context: Context) {
        val generator = PdfReportGenerator(context)
        val file: File? = generator.generateComplianceReport(
            reading = sensorReading.value,
            status = cabinetStatus.value,
            recentCycles = pumpCycles.value
        )
        if (file != null) {
            Toast.makeText(context, "สร้างรายงาน PDF สำเร็จ กำลังเปิดแชร์...", Toast.LENGTH_SHORT).show()
            generator.sharePdfFile(file)
        } else {
            Toast.makeText(context, "ไม่สามารถสร้างไฟล์ PDF ได้", Toast.LENGTH_LONG).show()
        }
    }
}
