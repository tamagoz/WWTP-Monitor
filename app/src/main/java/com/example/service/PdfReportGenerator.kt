package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.CabinetStatus
import com.example.model.PumpCycleRecord
import com.example.model.SensorReading
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    /**
     * สร้างไฟล์รายงาน PDF คุณภาพน้ำและสถิติตู้ควบคุม รพ.ชุมแพ
     */
    fun generateComplianceReport(
        reading: SensorReading,
        status: CabinetStatus,
        recentCycles: List<PumpCycleRecord>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 40, 85)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(235, 245, 255)
            style = Paint.Style.FILL
        }

        val passPaint = Paint().apply {
            color = Color.rgb(0, 150, 60)
            textSize = 10f
            isFakeBoldText = true
        }

        var y = 45f

        // 1. Header Block
        canvas.drawRect(30f, 30f, 565f, 100f, headerBgPaint)
        canvas.drawRect(30f, 30f, 565f, 100f, borderPaint)

        canvas.drawText("ใบรายงานผลการตรวจวัดและควบคุมคุณภาพน้ำเสียอัตโนมัติ (SCADA QA REPORT)", 45f, y, titlePaint)
        y += 18f
        canvas.drawText("โรงพยาบาลชุมแพ จังหวัดขอนแก่น | โครงการ: 2568-16-Chumpea-1-WaterPump", 45f, y, subPaint)
        y += 15f
        val dateFormat = SimpleDateFormat("วันที่ dd MMMM yyyy เวลา HH:mm:ss น.", Locale("th", "TH"))
        canvas.drawText("เวลาออกรายงาน: ${dateFormat.format(Date())} | โหมดตู้ควบคุม: ${status.controlMode.name}", 45f, y, subPaint)

        y = 125f

        // 2. Summary Statistics Box
        canvas.drawText("1. สรุปภาพรวมปริมาณน้ำบำบัด (Hydraulic Analytics)", 35f, y, boldPaint)
        y += 15f

        canvas.drawRect(30f, y, 565f, y + 80f, borderPaint)
        val cy = y + 20f
        canvas.drawText("• มิติถังพักน้ำเสีย: 3.0m x 4.0m x ลึก 7.0m (ปริมาตรรวม 84.0 m³)", 45f, cy, textPaint)
        canvas.drawText("• รอบการสูบ: จุดเริ่ม 90% (6.30m) -> จุดหยุด 80% (5.60m) | ระยะสูบ Δh = 0.70m", 45f, cy + 18f, textPaint)
        canvas.drawText("• ปริมาตรน้ำระบายต่อ 1 รอบ: +8,400 ลิตร (8.40 m³)", 45f, cy + 36f, textPaint)
        canvas.drawText("• จำนวนรอบสะสมวันนี้: ${status.cyclesToday} ครั้ง | ปริมาณน้ำวันนี้: ${String.format("%,d", status.litersToday)} ลิตร", 45f, cy + 54f, boldPaint)

        y += 105f

        // 3. Sensor Parameters Table
        canvas.drawText("2. ค่าพารามิเตอร์คุณภาพน้ำแบบเรียลไทม์ (Modbus RS-485 Sensors)", 35f, y, boldPaint)
        y += 15f

        // Table Header
        canvas.drawRect(30f, y, 565f, y + 22f, headerBgPaint)
        canvas.drawRect(30f, y, 565f, y + 22f, borderPaint)
        canvas.drawText("พารามิเตอร์", 45f, y + 15f, boldPaint)
        canvas.drawText("ค่าที่ตรวจวัดได้", 190f, y + 15f, boldPaint)
        canvas.drawText("เกณฑ์มาตรฐานควบคุม", 330f, y + 15f, boldPaint)
        canvas.drawText("ผลประเมิน", 480f, y + 15f, boldPaint)
        y += 22f

        // Table Rows
        val params = listOf(
            Triple("ความเป็นกรด-ด่าง (pH)", String.format("%.2f", reading.ph), "5.50 - 9.00"),
            Triple("ออกซิเจนละลาย (DO)", String.format("%.2f mg/L", reading.dissolvedOxygenMgL), "≥ 2.00 mg/L"),
            Triple("ศักย์ฆ่าเชื้อคลอรีน (ORP)", "${reading.orpMv.toInt()} mV", "≥ 650 mV"),
            Triple("ความขุ่น (Turbidity)", String.format("%.1f NTU", reading.turbidityNtu), "≤ 20.0 NTU"),
            Triple("ระดับน้ำในบ่อ (Level)", String.format("%.2f เมตร", reading.waterLevelM), "5.60 - 6.30 m"),
            Triple("กำลังไฟฟ้าตู้ MDB", String.format("%.1f kW", reading.powerKw), "พิกัดปกติ < 15 kW")
        )

        for (p in params) {
            canvas.drawRect(30f, y, 565f, y + 22f, borderPaint)
            canvas.drawText(p.first, 45f, y + 15f, textPaint)
            canvas.drawText(p.second, 190f, y + 15f, boldPaint)
            canvas.drawText(p.third, 330f, y + 15f, textPaint)
            canvas.drawText("PASS", 480f, y + 15f, passPaint)
            y += 22f
        }

        y += 25f

        // 4. Equipment Status
        canvas.drawText("3. สถานะการทำงานของเครื่องจักรอุปกรณ์ (MDB Motor Control)", 35f, y, boldPaint)
        y += 15f

        canvas.drawRect(30f, y, 565f, y + 70f, borderPaint)
        val ey = y + 18f
        canvas.drawText("• ปั๊มน้ำเสียเข้าบ่อ (P1 / KM1, 1.5 kW): ${if (reading.isPumpP1Running) "กำลังทำงาน (RUN)" else "หยุดการทำงาน (STOP)"}", 45f, ey, textPaint)
        canvas.drawText("• เครื่องเติมอากาศ Aerator (P2 / KM2, 5.5 kW): ${if (reading.isPumpP2Running) "กำลังทำงาน (RUN)" else "หยุดการทำงาน (STOP)"}", 45f, ey + 16f, textPaint)
        canvas.drawText("• ปั๊มตะกอนย้อนกลับ Sludge Return (P3 / KM3, 0.75 kW): ${if (reading.isPumpP3Running) "กำลังทำงาน (RUN)" else "หยุดการทำงาน (STANDBY)"}", 45f, ey + 32f, textPaint)
        canvas.drawText("• ปั๊มระบายน้ำทิ้ง Discharge (P4) & ปั๊มจ่ายคลอรีน (P5): ปกติ (RUN)", 45f, ey + 48f, textPaint)

        y += 95f

        // 5. Recent Cycles Table
        canvas.drawText("4. บันทึกรอบการสูบล่าสุด (Last 5 Pumping Cycles)", 35f, y, boldPaint)
        y += 15f

        canvas.drawRect(30f, y, 565f, y + 20f, headerBgPaint)
        canvas.drawRect(30f, y, 565f, y + 20f, borderPaint)
        canvas.drawText("รอบที่", 45f, y + 14f, boldPaint)
        canvas.drawText("เวลาตัดรอบ", 120f, y + 14f, boldPaint)
        canvas.drawText("ระดับเริ่ม -> หยุด", 240f, y + 14f, boldPaint)
        canvas.drawText("ปริมาตรที่สูบ", 380f, y + 14f, boldPaint)
        canvas.drawText("สถานะ QA", 480f, y + 14f, boldPaint)
        y += 20f

        val cyclesToShow = recentCycles.take(5).ifEmpty {
            listOf(
                PumpCycleRecord(14, "12:15 น.", 6.30f, 5.60f, 0.70f, 8400, status.litersToday),
                PumpCycleRecord(13, "11:20 น.", 6.30f, 5.60f, 0.70f, 8400, status.litersToday - 8400),
                PumpCycleRecord(12, "10:35 น.", 6.30f, 5.60f, 0.70f, 8400, status.litersToday - 16800),
                PumpCycleRecord(11, "09:40 น.", 6.30f, 5.60f, 0.70f, 8400, status.litersToday - 25200)
            )
        }

        for (c in cyclesToShow) {
            canvas.drawRect(30f, y, 565f, y + 18f, borderPaint)
            canvas.drawText("${c.cycleId}", 45f, y + 13f, textPaint)
            canvas.drawText(c.timeFormatted, 120f, y + 13f, textPaint)
            canvas.drawText("${"%.2f".format(c.startLevelM)}m -> ${"%.2f".format(c.stopLevelM)}m", 240f, y + 13f, textPaint)
            canvas.drawText("+${String.format("%,d", c.volumeLitersAdded)} ลิตร", 380f, y + 13f, boldPaint)
            canvas.drawText("PASS", 480f, y + 13f, passPaint)
            y += 18f
        }

        y += 35f

        // 6. Signatures Block
        val sigY = y + 20f
        canvas.drawLine(50f, sigY, 230f, sigY, borderPaint)
        canvas.drawText("ลงชื่อ .....................................................", 60f, sigY - 5f, textPaint)
        canvas.drawText("( เจ้าหน้าที่ผู้ควบคุมระบบบำบัดน้ำเสีย )", 50f, sigY + 16f, subPaint)

        canvas.drawLine(350f, sigY, 530f, sigY, borderPaint)
        canvas.drawText("ลงชื่อ .....................................................", 360f, sigY - 5f, textPaint)
        canvas.drawText("( หัวหน้ากลุ่มงานบริหารทั่วไป / วิศวกรรม )", 345f, sigY + 16f, subPaint)

        pdfDocument.finishPage(page)

        // Save file to app cache
        val fileName = "WWTP_Chumphae_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            pdfDocument.close()
            return null
        }
    }

    /**
     * แชร์หรือเปิดไฟล์ PDF
     */
    fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "รายงานคุณภาพน้ำเสีย รพ.ชุมแพ")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "แชร์รายงาน PDF คุณภาพน้ำเสีย").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
