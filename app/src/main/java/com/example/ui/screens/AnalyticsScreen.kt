package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CabinetStatus
import com.example.model.PumpCycleRecord
import com.example.model.SensorReading
import com.example.ui.theme.*

@Composable
fun AnalyticsScreen(
    reading: SensorReading,
    cabinetStatus: CabinetStatus,
    pumpCycles: List<PumpCycleRecord>,
    ntfyTopic: String,
    ntfyEnabled: Boolean,
    reportIntervalHours: Int,
    onNtfyTopicChange: (String) -> Unit,
    onNtfyEnabledChange: (Boolean) -> Unit,
    onReportIntervalChange: (Int) -> Unit,
    onTestNtfy: (Context) -> Unit,
    onSendReportNow: (Context) -> Unit,
    onExportPdf: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingTopic by remember(ntfyTopic) { mutableStateOf(ntfyTopic) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScadaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NTFY Notification & Report Scheduler Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = ScadaPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ระบบแจ้งเตือนผ่าน App NTFY (Push Alert)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "เตือนทันทีเมื่อค่าผิดปกติ และส่งรายงานรอบ 1 - 24 ชม.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = ntfyEnabled,
                            onCheckedChange = onNtfyEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ScadaPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // NTFY Topic Input
                    Text("หัวข้อแจ้งเตือน (NTFY Topic):", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editingTopic,
                            onValueChange = { newStr ->
                                editingTopic = newStr
                                onNtfyTopicChange(newStr)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("เช่น chumphae_wwtp_alerts", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScadaPrimary,
                                unfocusedBorderColor = ScadaBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = { onTestNtfy(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193247)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ทดสอบเตือน", color = ScadaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Periodic Report Interval (1 to 24 hours)
                    Text("รอบเวลาส่งรายงานสรุปอัตโนมัติ (1 - 24 ชั่วโมง):", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val intervalOptions = listOf(1, 2, 4, 6, 8, 12, 24)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalOptions.forEach { hrs ->
                            val isSelected = reportIntervalHours == hrs
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onReportIntervalChange(hrs) },
                                color = if (isSelected) ScadaPrimary else Color(0xFF0C1622),
                                shape = RoundedCornerShape(6.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${hrs}h",
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSendReportNow(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A52)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = ScadaPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ส่งรายงานสรุปรอบ ${reportIntervalHours} ชม. เข้าแอป NTFY ทันที", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Action: Export PDF Report Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "รายงานวิเคราะห์ผลและควบคุมคุณภาพน้ำเสีย",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "จัดทำเอกสาร PDF ทางการ สำหรับเสนอผู้บริหาร / สสจ.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { onExportPdf(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = ScadaPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ส่งออก PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Summary Metric Overview Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "สรุปยอดสะสมทางไฮดรอลิกส์ (Hydraulic Statistics):",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsStatBox(
                        title = "รอบการสูบวันนี้",
                        value = "${cabinetStatus.cyclesToday}",
                        unit = "ครั้ง/วัน",
                        sub = "ตัดรอบ 90% -> 80%",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsStatBox(
                        title = "ปริมาณน้ำบำบัดวันนี้",
                        value = String.format("%,d", cabinetStatus.litersToday),
                        unit = "ลิตร",
                        sub = "+8,400 ลิตร/รอบ",
                        color = ScadaSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsStatBox(
                        title = "ยอดสะสมประจำเดือน",
                        value = "${"%.1f".format(cabinetStatus.monthlyM3)}",
                        unit = "m³ (คิว)",
                        sub = "เทียบเท่า ${String.format("%,d", (cabinetStatus.monthlyM3 * 1000).toLong())} ลิตร",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsStatBox(
                        title = "อัตราผ่านเกณฑ์ QA",
                        value = "99.4%",
                        unit = "Compliance",
                        sub = "เกณฑ์ pH, DO, ORP",
                        color = ScadaPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Real-time Trend Curves (Canvas)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "กราฟแนวโน้มพารามิเตอร์คุณภาพน้ำ (Trend Curves)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendDot(color = ScadaPrimary, label = "pH")
                            LegendDot(color = ScadaSuccess, label = "DO")
                            LegendDot(color = Color(0xFFFFD600), label = "ระดับ (m)")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CanvasTrendPlot(modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp))
                }
            }
        }

        // Table of Recent Cycles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ประวัติรอบการสูบน้ำเสีย (+8,400 ลิตร/รอบ)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Falling Edge 1 -> 0",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C1622), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("รอบ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("เวลาตัดรอบ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("ระดับ 90%->80%", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("ปริมาตรสะสม", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("QA", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    pumpCycles.take(8).forEach { cycle ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${cycle.cycleId}", color = ScadaPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(cycle.timeFormatted, color = TextPrimary, fontSize = 11.sp)
                            Text("6.30m -> 5.60m", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("+8,400 L", color = ScadaSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Surface(color = ScadaSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("PASS", color = ScadaSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        HorizontalDivider(color = ScadaBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnalyticsStatBox(
    title: String,
    value: String,
    unit: String,
    sub: String,
    color: Color = ScadaPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = TextSecondary, fontSize = 10.sp)
            }
            Text(sub, color = TextTertiary, fontSize = 9.5.sp)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun CanvasTrendPlot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Horizontal Grid Lines
        for (i in 1..4) {
            val y = (h / 5) * i
            drawLine(
                color = Color(0xFF1E354A),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw Simulated Trend Curves
        val phPoints = listOf(0.45f, 0.48f, 0.46f, 0.52f, 0.50f, 0.53f, 0.49f, 0.51f, 0.50f, 0.52f)
        val doPoints = listOf(0.65f, 0.62f, 0.64f, 0.60f, 0.63f, 0.66f, 0.64f, 0.62f, 0.65f, 0.64f)
        val lvlPoints = listOf(0.85f, 0.78f, 0.71f, 0.64f, 0.58f, 0.88f, 0.82f, 0.75f, 0.68f, 0.62f)

        fun drawSmoothCurve(points: List<Float>, strokeColor: Color) {
            val path = Path()
            val step = w / (points.size - 1)
            points.forEachIndexed { idx, p ->
                val x = idx * step
                val y = h - (p * (h - 20f)) - 10f
                if (idx == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }
            drawPath(path, strokeColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        drawSmoothCurve(phPoints, Color(0xFF00D2FF))
        drawSmoothCurve(doPoints, Color(0xFF00E676))
        drawSmoothCurve(lvlPoints, Color(0xFFFFD600))
    }
}
