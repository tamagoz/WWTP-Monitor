package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CabinetStatus
import com.example.model.ControlMode
import com.example.model.QAEvaluation
import com.example.model.SensorReading
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun DashboardScreen(
    sensorReading: SensorReading,
    cabinetStatus: CabinetStatus,
    qaEvaluation: QAEvaluation,
    onModeChange: (ControlMode) -> Unit,
    onEmergencyStop: () -> Unit,
    onResetEmergencyStop: () -> Unit,
    onTogglePump1: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScadaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Emergency Stop Alert Banner if Active
        if (cabinetStatus.isEmergencyStopped) {
            item {
                EmergencyStopActiveBanner(onReset = onResetEmergencyStop)
            }
        }

        // 2. QA Compliance Status Banner
        item {
            QaStatusBanner(qaEvaluation = qaEvaluation)
        }

        // 3. Tank Visualizer Card (3m x 4m x 7m Wet Well)
        item {
            TankVisualizerCard(
                reading = sensorReading,
                status = cabinetStatus,
                onTogglePump = onTogglePump1
            )
        }

        // 4. Modbus RTU Telemetry Sensors Grid
        item {
            TelemetrySensorsSection(reading = sensorReading)
        }

        // 5. Motor Equipment Status & Contactor KM1-KM5
        item {
            EquipmentStatusSection(reading = sensorReading)
        }

        // 6. Control Cabinet Panel (AUTO / MANUAL / REMOTE & E-Stop)
        item {
            CabinetControlSection(
                status = cabinetStatus,
                onModeChange = onModeChange,
                onEmergencyStop = onEmergencyStop
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EmergencyStopActiveBanner(onReset: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "estop")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "estop_flash"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, ScadaDanger.copy(alpha = alpha), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0D11))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "E-Stop Active",
                    tint = ScadaDanger,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "EMERGENCY STOPPED (กดปุ่มหยุดฉุกเฉิน)",
                        color = ScadaDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "คอยล์แมกเนติกทุกตัวถูกตัดไฟ เพื่อความปลอดภัย",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = ScadaDanger),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("reset_estop_button")
            ) {
                Text("รีเซ็ต (RESET)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QaStatusBanner(qaEvaluation: QAEvaluation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (qaEvaluation.isCompliant) Color(0xFF0F261F) else Color(0xFF2E1515)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (qaEvaluation.isCompliant) ScadaSuccess else ScadaDanger,
                    ScadaPrimary
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (qaEvaluation.isCompliant) ScadaSuccess else ScadaDanger),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (qaEvaluation.isCompliant) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = qaEvaluation.statusText,
                    color = if (qaEvaluation.isCompliant) ScadaSuccess else ScadaDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = qaEvaluation.summaryNote,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TankVisualizerCard(
    reading: SensorReading,
    status: CabinetStatus,
    onTogglePump: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(ScadaBorder, Color.Transparent))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ถังพักน้ำเสีย (Wet Well Sump Pit)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "กว้าง 3.0m x ยาว 4.0m x ลึก 7.0m (84.0 m³)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    color = if (reading.isPumpP1Running) ScadaSuccess.copy(alpha = 0.2f) else ScadaWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (reading.isPumpP1Running) "● กำลังสูบ (RUN)" else "○ พัก (STOP)",
                        color = if (reading.isPumpP1Running) ScadaSuccess else ScadaWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Fluid Tank Graphic
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070F16))
                        .border(2.dp, ScadaBorder, RoundedCornerShape(8.dp))
                ) {
                    val targetPercent = (reading.waterLevelM / 7.0f).coerceIn(0f, 1f)
                    val animatedWaterLevel by animateFloatAsState(
                        targetValue = targetPercent,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label = "tank_water"
                    )

                    val infiniteTransition = rememberInfiniteTransition(label = "wave")
                    val wavePhase by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 6.28f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "wave_anim"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val waterH = h * animatedWaterLevel
                        val waterTopY = h - waterH

                        // Draw water body with gradient
                        val path = Path().apply {
                            moveTo(0f, h)
                            lineTo(w, h)
                            lineTo(w, waterTopY)

                            // Sine wave top
                            val steps = 20
                            for (i in steps downTo 0) {
                                val x = w * (i.toFloat() / steps)
                                val waveOffset = sin(wavePhase + (i * 0.4f)) * 3.5f
                                lineTo(x, waterTopY + waveOffset)
                            }
                            close()
                        }

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF005B94)),
                                startY = waterTopY,
                                endY = h
                            )
                        )

                        // 90% Line (Start Pump: 6.30m)
                        val line90Y = h * (1f - 0.90f)
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = Offset(0f, line90Y),
                            end = Offset(w, line90Y),
                            strokeWidth = 1.5f
                        )

                        // 80% Line (Stop Pump: 5.60m)
                        val line80Y = h * (1f - 0.80f)
                        drawLine(
                            color = Color(0xFFFFD600),
                            start = Offset(0f, line80Y),
                            end = Offset(w, line80Y),
                            strokeWidth = 1.5f
                        )
                    }

                    // Level Labels
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("7.0m", color = Color.White.copy(0.4f), fontSize = 9.sp)
                        Text("90% ▲ (6.3m)", color = Color(0xFFFF8A80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("80% ▼ (5.6m)", color = Color(0xFFFFFF8D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("0.0m", color = Color.White.copy(0.4f), fontSize = 9.sp)
                    }
                }

                // Readings Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniBox(
                        label = "ระดับน้ำในบ่อปัจจุบัน",
                        value = "${"%.2f".format(reading.waterLevelM)} m",
                        sub = "${(reading.waterLevelM / 7f * 100).toInt()}% ของความลึกถัง",
                        highlight = true
                    )
                    MetricMiniBox(
                        label = "ปริมาตรต่อรอบการสูบ (Δh 0.7m)",
                        value = "+8,400 ลิตร",
                        sub = "เทียบเท่า 8.40 ลูกบาศก์เมตร (m³)",
                        color = ScadaSuccess
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            MetricMiniBox(
                                label = "อัตราการสูบ",
                                value = "${reading.flowRateM3h.toInt()} m³/h",
                                sub = "2@Q=42 m³/h"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            MetricMiniBox(
                                label = "กำลังไฟฟ้า",
                                value = "${"%.1f".format(reading.powerKw)} kW",
                                sub = "Energy Meter"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniBox(
    label: String,
    value: String,
    sub: String,
    highlight: Boolean = false,
    color: Color = ScadaPrimary
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C1722),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text(
                text = value,
                color = if (highlight) ScadaPrimary else color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(sub, color = TextTertiary, fontSize = 9.sp)
        }
    }
}

@Composable
fun TelemetrySensorsSection(reading: SensorReading) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "พารามิเตอร์คุณภาพน้ำ (Modbus RS-485)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Polling 1,000ms",
                color = ScadaPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorCard(
                title = "ความเป็นกรด-ด่าง (pH)",
                value = String.format("%.2f", reading.ph),
                unit = "",
                stdText = "เกณฑ์: 5.50 - 9.00",
                progress = reading.ph / 14f,
                isPass = reading.ph in 5.50f..9.00f,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                title = "ออกซิเจนละลาย (DO)",
                value = String.format("%.2f", reading.dissolvedOxygenMgL),
                unit = "mg/L",
                stdText = "เกณฑ์: ≥ 2.00 mg/L",
                progress = (reading.dissolvedOxygenMgL / 8f).coerceIn(0f, 1f),
                isPass = reading.dissolvedOxygenMgL >= 2.00f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorCard(
                title = "ศักย์ฆ่าเชื้อ (ORP)",
                value = "${reading.orpMv.toInt()}",
                unit = "mV",
                stdText = "เกณฑ์: ≥ 650 mV (คลอรีน)",
                progress = (reading.orpMv / 900f).coerceIn(0f, 1f),
                isPass = reading.orpMv >= 650f,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                title = "ความขุ่น (Turbidity)",
                value = String.format("%.1f", reading.turbidityNtu),
                unit = "NTU",
                stdText = "เกณฑ์: ≤ 20.0 NTU",
                progress = (reading.turbidityNtu / 40f).coerceIn(0f, 1f),
                isPass = reading.turbidityNtu <= 20.0f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SensorCard(
    title: String,
    value: String,
    unit: String,
    stdText: String,
    progress: Float,
    isPass: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = if (isPass) ScadaPrimary else ScadaDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, color = TextSecondary, fontSize = 11.sp)
                }
            }
            Text(stdText, color = TextTertiary, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isPass) ScadaPrimary else ScadaDanger,
                trackColor = Color(0xFF1E354A),
            )
        }
    }
}

@Composable
fun EquipmentStatusSection(reading: SensorReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "สถานะการทำงานของมอเตอร์ (MDB Motor KM1-KM5)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            EquipmentItemRow(
                tag = "P1 / KM1",
                name = "ปั๊มสูบน้ำเสียเข้าบ่อปรับสภาพ",
                spec = "1.5 kW | 42 m³/h | TDH 8m",
                isRunning = reading.isPumpP1Running
            )
            EquipmentItemRow(
                tag = "P2 / KM2",
                name = "เครื่องเติมอากาศ (Aerator บ่อชีวภาพ)",
                spec = "5.5 kW | Timer 30น. เดิน / 15น. พัก",
                isRunning = reading.isPumpP2Running
            )
            EquipmentItemRow(
                tag = "P3 / KM3",
                name = "ปั๊มสูบกากตะกอนย้อนกลับ (Sludge Return)",
                spec = "0.75 kW | ควบคุมรอบตะกอนชีวภาพ",
                isRunning = reading.isPumpP3Running
            )
            EquipmentItemRow(
                tag = "P4 / KM4",
                name = "ปั๊มสูบระบายน้ำทิ้งสาธารณะ (Discharge)",
                spec = "1.5 kW | น้ำใสผ่านการฆ่าเชื้อ",
                isRunning = reading.isPumpP4Running
            )
            EquipmentItemRow(
                tag = "P5 / KM5",
                name = "ปั๊มจ่ายสารละลายคลอรีน (Dosing Pump)",
                spec = "ควบคุมตามค่า ORP ≥ 650 mV",
                isRunning = reading.isPumpP5Running
            )
        }
    }
}

@Composable
fun EquipmentItemRow(
    tag: String,
    name: String,
    spec: String,
    isRunning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF0C1622), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF1B2C3D),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = tag,
                    color = ScadaPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(spec, color = TextTertiary, fontSize = 10.sp)
            }
        }

        Surface(
            color = if (isRunning) ScadaSuccess.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isRunning) "RUN" else "STOP",
                color = if (isRunning) ScadaSuccess else TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun CabinetControlSection(
    status: CabinetStatus,
    onModeChange: (ControlMode) -> Unit,
    onEmergencyStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "แผงควบคุมตู้ MDB และสั่งการระบบ (Operation Control)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector
            Text("เลือกโหมดการควบคุมหน้าตู้:", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(ControlMode.AUTO, ControlMode.MANUAL, ControlMode.REMOTE).forEach { mode ->
                    val isSelected = status.controlMode == mode
                    Button(
                        onClick = { onModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) ScadaPrimary else Color(0xFF162534)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emergency Stop Push Button
            Button(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.buttonColors(containerColor = ScadaDanger),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("emergency_stop_button")
            ) {
                Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ปุ่มหยุดฉุกเฉิน (EMERGENCY STOP)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
