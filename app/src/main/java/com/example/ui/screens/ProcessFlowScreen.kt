package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PinDefinition
import com.example.ui.theme.*

@Composable
fun ProcessFlowScreen(
    pinDefinitions: List<PinDefinition>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStageIndex by remember { mutableIntStateOf(0) }

    val filteredPins = remember(searchQuery, pinDefinitions) {
        if (searchQuery.isBlank()) pinDefinitions
        else {
            val q = searchQuery.trim().lowercase()
            pinDefinitions.filter {
                it.pinName.lowercase().contains(q) ||
                it.signalName.lowercase().contains(q) ||
                it.connectedDevice.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScadaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Blueprint Header
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
                        Column {
                            Text(
                                text = "แบบแปลนแสดงผังกระบวนการ (PROCESS FLOW DIAGRAM)",
                                color = ScadaPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "โรงพยาบาลชุมแพ จังหวัดขอนแก่น | โครงการ 2568-16-Chumpea-1-WaterPump",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = Color(0xFF1B2C3D),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MDB IoT System",
                                color = ScadaPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Process Stages Sequence Explorer
        item {
            Text(
                text = "ลำดับขั้นตอนกระบวนการบำบัดน้ำเสีย (Treatment Stages):",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val stages = listOf(
                ProcessStageItem(
                    num = "1",
                    title = "บ่อดักขยะ (SCREEN CHAMBER)",
                    subtitle = "ดักเศษขยะและสิ่งปฏิกูล",
                    details = "ดักจับเศษผ้า ก๊อซ ขยะติดเชื้อหยาบ และสิ่งปฏิกูลด้วยตะแกรงสแตนเลส ก่อนน้ำเสียไหลล้นเข้าสู่บ่อสูบน้ำเสีย เพื่อป้องกันใบพัดปั๊มอุดตัน"
                ),
                ProcessStageItem(
                    num = "2",
                    title = "บ่อสูบน้ำเสีย (SUMP PIT - ถังพัก 84 m³)",
                    subtitle = "ปั๊มสูบ PUMP 2@Q=42 m³/hr, TDH=8m",
                    details = "ถังพักน้ำเสียขนาด 3.0m x 4.0m x ลึก 7.0m ปริมาตรรวม 84.0 m³ มีสวิตช์ลูกลอย FS-H (90% = 6.30m) สั่งเดินปั๊ม และ FS-L (80% = 5.60m) สั่งหยุดปั๊ม ระบายน้ำรอบละ 8,400 ลิตร"
                ),
                ProcessStageItem(
                    num = "3",
                    title = "ถังปรับสภาพน้ำ (EQUALIZATION TANK)",
                    subtitle = "ปรับสมดุลการไหลและค่า pH",
                    details = "กักพักเพื่อลดความแปรปรวนของอัตราการไหลและเจือจางความเข้มข้นของสารเคมีจากแผนกแล็บ/ซักล้าง ควบคุมค่า pH ให้อยู่ระหว่าง 6.5 - 8.0"
                ),
                ProcessStageItem(
                    num = "4",
                    title = "ถังเติมอากาศ (AERATION TANK)",
                    subtitle = "AERATOR 5.5 kW (บ่อชีวภาพ)",
                    details = "เติมอากาศเพื่อเพิ่มออกซิเจนละลาย (DO ≥ 2.0 mg/L) ให้แก่แบคทีเรียและจุลินทรีย์ชนิดใช้ออกซิเจน ย่อยสลายสารอินทรีย์ BOD/COD"
                ),
                ProcessStageItem(
                    num = "5",
                    title = "ถังตกตะกอน (SEDIMENTATION TANK)",
                    subtitle = "แยกน้ำใสและตะกอนชีวภาพ",
                    details = "น้ำเสียที่ผ่านการบำบัดจะไหลล้นเข้ามาตกตะกอน น้ำใสจะล้นผ่าน Weirs ไปยังถังคลอรีน ส่วนกากตะกอนด้านล่างจะถูกสูบย้อนกลับ (Sludge Return) และสูบเข้า Sludge Holding Tank"
                ),
                ProcessStageItem(
                    num = "6",
                    title = "ถังสัมผัสคลอรีน (CHLORINE CONTACT)",
                    subtitle = "ฆ่าเชื้อโรคก่อนระบายสู่ท่อสาธารณะ",
                    details = "ฉีดพ่นสารละลายคลอรีนควบคุมด้วย ORP ≥ 650 mV ทำลายเชื้อโรค แบคทีเรีย และไวรัสในน้ำทิ้งโรงพยาบาล ก่อนระบายลงสู่ท่อระบายน้ำทิ้งสาธารณะอย่างปลอดภัย"
                ),
                ProcessStageItem(
                    num = "7",
                    title = "ถังกักตะกอน & ลานตาก (SLUDGE SYSTEM)",
                    subtitle = "SLUDGE PUMP 2@Q=42 m³/hr, TDH=6m",
                    details = "สูบตะกอนส่วนเกินเข้าสู่ Sludge Holding Tank และส่งต่อไปยังลานตากตะกอน (Sludge Drying Bed) เพื่อระบายน้ำและตากแห้งก่อนนำไปกำจัดอย่างถูกหลักสุขาภิบาล"
                )
            )

            stages.forEachIndexed { index, item ->
                StageCard(
                    stage = item,
                    isSelected = selectedStageIndex == index,
                    onClick = { selectedStageIndex = index }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Circuit Breakers Map
        item {
            Text(
                text = "ผังอุปกรณ์และเบรกเกอร์ไฟฟ้าตู้ MDB (Power Circuit):",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            BreakersSummaryCard()
        }

        // Pinout Mapping Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ตารางตำแหน่งขาบอร์ดควบคุม (Pinout Mapping)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ESP32-S3 / ESP32-WROOM-32E, Optocoupler, Relays & RS-485",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_search_field"),
                        placeholder = { Text("🔍 ค้นหาขาหรือสัญญาณ (เช่น GPIO32, FS-H, RS485)...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScadaPrimary,
                            unfocusedBorderColor = ScadaBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    filteredPins.forEach { pin ->
                        PinItemRow(pin = pin)
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

data class ProcessStageItem(
    val num: String,
    val title: String,
    val subtitle: String,
    val details: String
)

@Composable
fun StageCard(
    stage: ProcessStageItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF132536) else Color(0xFF0C1622)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) ScadaPrimary else ScadaBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ScadaPrimary else Color(0xFF1E354A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stage.num,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(stage.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(stage.subtitle, color = ScadaPrimary, fontSize = 11.sp)
                }
            }
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stage.details,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun BreakersSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BreakerChip(label = "Q1 (Main MCCB)", sub = "100A 3P メイン", modifier = Modifier.weight(1f))
                BreakerChip(label = "Q2 (ปั๊มน้ำเสีย P1)", sub = "16A 3P + KM1 + OL1", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BreakerChip(label = "Q3 (Aerator P2)", sub = "20A 3P + KM2 + OL2", modifier = Modifier.weight(1f))
                BreakerChip(label = "Q4 (Sludge P3)", sub = "10A 3P + KM3 + OL3", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BreakerChip(label = "Q5 (น้ำทิ้ง P4)", sub = "16A 3P + KM4 + OL4", modifier = Modifier.weight(1f))
                BreakerChip(label = "Q6 (คลอรีน P5)", sub = "6A 1P + KM5 + OL5", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BreakerChip(label = "Q7 (วงจรควบคุม)", sub = "10A 1P 24VDC PSU", modifier = Modifier.weight(1f))
                BreakerChip(label = "E-Stop Safety", sub = "ปุ่มหยุดฉุกเฉิน Safety Bus", isAlert = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BreakerChip(label: String, sub: String, isAlert: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (isAlert) Color(0xFF261014) else Color(0xFF0C1622),
        shape = RoundedCornerShape(6.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, color = if (isAlert) ScadaDanger else ScadaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = TextTertiary, fontSize = 9.5.sp)
        }
    }
}

@Composable
fun PinItemRow(pin: PinDefinition) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF1B2C3D),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = pin.pinName,
                        color = ScadaPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pin.signalName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = pin.electricalSpec,
                color = ScadaSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "เชื่อมต่อ: ${pin.connectedDevice} | ${pin.description}",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
