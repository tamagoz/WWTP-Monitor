package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ManualSection
import com.example.model.TroubleshootingItem
import com.example.ui.theme.*

@Composable
fun ManualFaqScreen(
    manualSections: List<ManualSection>,
    troubleshootingItems: List<TroubleshootingItem>,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onExportPdf: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("ทั้งหมด") }
    var expandedSectionId by remember { mutableStateOf<String?>(null) }
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    val categories = listOf("ทั้งหมด", "คู่มือมาตรฐานกรมควบคุมมลพิษ", "ตู้ควบคุม MDB", "ไฮดรอลิกส์", "ความปลอดภัย", "แก้ไขปัญหา", "คำถามที่พบบ่อย (FAQ)")

    val officialPcdChapters = remember {
        listOf(
            ManualSection(
                id = "pcd_ch1",
                title = "บทที่ ๑: บทนำและกฎหมายควบคุมน้ำเสียโรงพยาบาล",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "การจัดการน้ำเสียจากการรักษาผู้เจ็บป่วย ป้องกันการแพร่กระจายเชื้อโรคสู่สิ่งแวดล้อม",
                keywords = listOf("บทนำ", "กฎหมาย", "เชื้อโรค", "bod", "tss", "tkn", "กรมควบคุมมลพิษ"),
                contentMarkdown = """• **ความสำคัญ:** โรงพยาบาลเป็นแหล่งกำเนิดน้ำเสียที่มีเชื้อโรคปะปนจากของเสียทางการแพทย์ และน้ำชะล้างทำความสะอาดร่างกาย
• **สถิติระดับประเทศ:** ประเทศไทยมีโรงพยาบาลกว่า ๑,๒๒๑ แห่ง (EDX) น้ำเสียต้องถูกบำบัดและฆ่าเชื้อโรคก่อนระบายออกสู่สิ่งแวดล้อม
• **พารามิเตอร์ที่มักเกินมาตรฐาน:** บีโอดี (BOD), สารแขวนลอย (TSS), และไนโตรเจนรวม (TKN)
• **เป้าหมายของคู่มือ:** เป็นแนวทางให้ผู้ประกอบการตรวจสอบระบบบำบัดให้มีประสิทธิภาพสูงสุดตามเกณฑ์กระทรวงทรัพยากรธรรมชาติและสิ่งแวดล้อม"""
            ),
            ManualSection(
                id = "pcd_ch2",
                title = "บทที่ ๒: แหล่งกำเนิดและลักษณะน้ำเสียจากโรงพยาบาล",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "การจัดประเภทอาคาร ก (≥30 เตียง), ข (10-29 เตียง) อัตรา 800 ลิตร/เตียง/วัน",
                keywords = listOf("แหล่งกำเนิด", "ประเภท ก", "ประเภท ข", "เตียง", "800 ลิตร", "อัตราใช้น้ำ", "ซักผ้า", "ห้องแล็บ"),
                contentMarkdown = """• **การจำแนกประเภทอาคารตามกฎหมาย:**
  - **อาคารประเภท ก:** โรงพยาบาลที่มีเตียงรับผู้ป่วยค้างคืนตั้งแต่ ๓๐ เตียงขึ้นไป
  - **อาคารประเภท ข:** โรงพยาบาลที่มีเตียงรับผู้ป่วยค้างคืนตั้งแต่ ๑๐ เตียง แต่ไม่ถึง ๓๐ เตียง
• **กิจกรรมที่เป็นแหล่งกำเนิดน้ำเสีย:**
  ๑. แผนกผู้ป่วยนอก (OPD): ห้องน้ำ ห้องตรวจ
  ๒. แผนกผู้ป่วยใน (IPD): การชะล้างบาดแผล น้ำยาฆ่าเชื้อโรค
  ๓. โรงซักผ้า: เสื้อผ้า ปลอกหมอน ผ้าห่ม (ปนเปื้อนน้ำยาซักผ้าและสารเคมี)
  ๔. โรงครัวและห้องอาหาร: เศษอาหาร ไขมัน น้ำมัน
  ๕. ห้องผ่าตัด ห้องคลอด ห้องเก็บศพ: เลือด น้ำยาฟอร์มาลิน สารเคมี
  ๖. ห้องปฏิบัติการ (Lab) และห้องยา: สารเคมี อาหารเลี้ยงเชื้อ
• **การคำนวณปริมาณน้ำเสีย:**
  - อัตราการใช้น้ำมาตรฐาน = ๘๐๐ ลิตร/เตียงผู้ป่วย/วัน
  - ปริมาณน้ำเสียที่เกิดขึ้น = ร้อยละ ๘๐ ของน้ำใช้จริง (ตัวอย่าง: 100 เตียง = 64,000 ลิตร/วัน)"""
            ),
            ManualSection(
                id = "pcd_ch3",
                title = "บทที่ ๓: แนวทางในการลดน้ำเสียและความสกปรก (Waste Minimization)",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "การใช้อุปกรณ์ประหยัดน้ำ ระบบอัตโนมัติ การนำน้ำกลับมาใช้ใหม่ (Gray Water) และถังดักไขมัน",
                keywords = listOf("ลดน้ำเสีย", "ประหยัดน้ำ", "gray water", "ถังดักไขมัน", "ตะแกรง", "อัตโนมัติ"),
                contentMarkdown = """• **การลดปริมาณการใช้น้ำ:**
  - ติดตั้งก๊อกน้ำเซนเซอร์อัตโนมัติหรือแบบกดหน่วงเวลา, ชักโครกประหยัดน้ำ 3/6 ลิตร
  - การนำน้ำล้างที่สะอาดกลับมาใช้ซ้ำ (Gray Water Reuse) สำหรับรดน้ำต้นไม้หรือชักโครกหลังผ่านการฆ่าเชื้อ
  - ตรวจสอบการรั่วไหลของระบบท่อส่งน้ำหลักเป็นประจำ
• **การลดความสกปรกที่แหล่งกำเนิด:**
  - ติดตั้งตะแกรงดักขยะหยาบและละเอียดที่ท่อระบายน้ำ
  - ติดตั้งบ่อดักไขมันที่มีประสิทธิภาพในโรงครัว ดักไขมันได้ >60% และตักไขมันทิ้งทุกสัปดาห์
• **การปรับปรุงด้วยเทคโนโลยีอัตโนมัติ:**
  - นำระบบ IoT และไมโครคอนโทรลเลอร์ (ESP32 / SCADA) เข้ามาควบคุมระดับน้ำและเวลาเดินปั๊ม เพื่อให้ระบบทำงานต่อเนื่องและมีประสิทธิภาพสูงสุด"""
            ),
            ManualSection(
                id = "pcd_ch4",
                title = "บทที่ ๔: การดำเนินการตามกฎกระทรวงฯ ตามมาตรา ๘๐",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "การจดบันทึกสถิติประจำวัน แบบ ทส.๑ (เก็บ 2 ปี) และส่งรายงานประจำเดือน แบบ ทส.๒",
                keywords = listOf("มาตรา 80", "ทส.1", "ทส.2", "กฎกระทรวง", "รายงาน", "เจ้าพนักงานท้องถิ่น"),
                contentMarkdown = """• **หน้าที่ตามกฎหมายของผู้ครอบครองแหล่งกำเนิดมลพิษ:**
  ๑. **จัดทำบันทึกสถิติประจำวันตามแบบ ทส.๑:** บันทึกข้อมูลการทำงานของระบบบำบัดน้ำเสีย เครื่องสูบน้ำ ปริมาณน้ำเสีย และสารเคมีทุกวัน และต้องเก็บเอกสารไว้ ณ สถานที่ตั้งเป็นเวลาอย่างน้อย ๒ ปี
  ๒. **สรุปผลการทำงานรายเดือนตามแบบ ทส.๒:** รวบรวมสรุปผลส่งต่อเจ้าพนักงานท้องถิ่น (เทศบาล / อบต.) ภายในวันที่ ๑๕ ของเดือนถัดไป
• **ระบบตู้ควบคุมอัจฉริยะช่วยสนับสนุนมาตรา ๘๐:** บันทึกข้อมูลดิจิทัลอัตโนมัติลงไฟล์ Excel CSV (UTF-8 BOM) และสร้างรายงาน PDF ได้ทันที ไม่ต้องจดบันทึกด้วยมือ"""
            ),
            ManualSection(
                id = "pcd_ch5",
                title = "บทที่ ๕: หลักการบำบัดน้ำเสียและเกณฑ์มาตรฐานน้ำทิ้ง",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "ตารางมาตรฐานน้ำทิ้งประเภท ก/ข, ระบบแอกติเวทเต็ดสลัดจ์, คลองวนเวียน, และการฆ่าเชื้อด้วยคลอรีน",
                keywords = listOf("มาตรฐานน้ำทิ้ง", "ประเภท ก", "activated sludge", "คลองวนเวียน", "คลอรีน", "orp", "do"),
                contentMarkdown = """• **ตารางมาตรฐานควบคุมน้ำทิ้งอาคารประเภท ก (≥30 เตียง):**
  - **pH:** ๕ - ๙
  - **บีโอดี (BOD):** ไม่เกิน ๒๐ มก./ล. (ประเภท ข ไม่เกิน ๓๐ มก./ล.)
  - **สารแขวนลอย (SS):** ไม่เกิน ๓๐ มก./ล.
  - **ตะกอนหนัก:** ไม่เกิน ๐.๕ มล./ล.
  - **ซัลไฟด์ (Sulfide):** ไม่เกิน ๑.๐ มก./ล.
  - **ทีเคเอ็น (TKN):** ไม่เกิน ๓๕ มก./ล.
  - **น้ำมันและไขมัน:** ไม่เกิน ๒๐ มก./ล.
• **ระบบบำบัดทางชีวภาพ:**
  - ระบบแอกติเวทเต็ดสลัดจ์ (Activated Sludge): ประกอบด้วยถังเติมอากาศและถังตกตะกอน ควบคุมค่า DO อยู่ที่ 1-3 mg/L
  - การฆ่าเชื้อโรค (Disinfection): ใช้สารละลายคลอรีน ควบคุมค่า ORP ≥ 650 mV เพื่อทำลายเชื้อโรคก่อนระบายออกสู่สาธารณะ"""
            ),
            ManualSection(
                id = "pcd_ch6_9",
                title = "บทที่ ๖-๙: การวิเคราะห์ปัญหา การบำรุงรักษาเครื่องสูบน้ำ และตรวจสอบ",
                category = "คู่มือมาตรฐานกรมควบคุมมลพิษ",
                summary = "การตรวจลักษณะทางกายภาพ สี/กลิ่น/ฟอง, การดูแลรักษาเครื่องสูบน้ำ, และความปลอดภัย",
                keywords = listOf("บำรุงรักษา", "เครื่องสูบน้ำ", "ฟองสีขาว", "ฟองสีน้ำตาล", "ตะกอนลอย", "air blower", "ลูกลอย"),
                contentMarkdown = """• **การตรวจสอบทางกายภาพถังเติมอากาศ:**
  - **สีตะกอนที่ดี:** สีน้ำตาลเข้ม กลิ่นคล้ายดิน
  - **ฟองสีขาว:** อายุตะกอนจุลินทรีย์น้อยเกินไป
  - **ฟองสีน้ำตาล:** อายุตะกอนสูงเกินไป ต้องระบายสลัดจ์ทิ้ง
• **การบำรุงรักษาเครื่องสูบน้ำ (Pumps):**
  - ล้างทำความสะอาดลูกลอยและสายปรับระดับเดือนละครั้ง
  - ตรวจเช็คระดับน้ำมันหล่อลื่นแมคคานิคอลซีล (หากน้ำมันเป็นสีขาวขุ่นแสดงว่าน้ำรั่วเข้าซีล ต้องเปลี่ยนทันที)
  - ปรับระดับลูกลอยให้ทำงานเฉลี่ย 15 นาที และพัก 15 นาที
• **ความปลอดภัยหน้างาน:** ตัดวงจรไฟฟ้าก่อนลงบ่อสูบ, เปิดฝาบ่อทิ้งไว้อย่างน้อย 30 นาทีเพื่อระบายก๊าซมีเทนและไข่เน่า, และผูกเชือกนิรภัยที่เอวเสมอ"""
            )
        )
    }

    val combinedSections = remember(manualSections, officialPcdChapters) {
        officialPcdChapters + manualSections
    }

    val displayedSections = remember(combinedSections, selectedCategory, searchKeyword) {
        combinedSections.filter { sec ->
            val matchCategory = when (selectedCategory) {
                "ทั้งหมด" -> true
                "คู่มือมาตรฐานกรมควบคุมมลพิษ" -> sec.category == "คู่มือมาตรฐานกรมควบคุมมลพิษ"
                "ตู้ควบคุม MDB" -> sec.category == "ฮาร์ดแวร์" || sec.category == "วงจรและการต่อสาย"
                "ไฮดรอลิกส์" -> sec.category == "ไฮดรอลิกส์"
                "ความปลอดภัย" -> sec.category == "ความปลอดภัย"
                "แก้ไขปัญหา" -> sec.category == "การแก้ไขปัญหา"
                else -> true
            }

            val matchQuery = if (searchKeyword.isBlank()) true else {
                val q = searchKeyword.trim().lowercase()
                sec.title.lowercase().contains(q) ||
                sec.summary.lowercase().contains(q) ||
                sec.contentMarkdown.lowercase().contains(q) ||
                sec.keywords.any { it.lowercase().contains(q) }
            }

            matchCategory && matchQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScadaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Export Header
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
                                text = "คู่มือการใช้งานและจัดการน้ำเสียโรงพยาบาล",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "OP Manual Rev.2 & คู่มือกำกับมาตรฐาน กรมควบคุมมลพิษ",
                                color = ScadaPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { onExportPdf(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = ScadaPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_manual_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("พิมพ์คู่มือ PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Keyword Search Field
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_keyword_search"),
                        placeholder = { Text("🔍 ค้นหาคีย์เวิร์ด (เช่น ลูกลอย, BOD, มาตรา 80, Overload, 8400 ลิตร)...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScadaPrimary,
                            unfocusedBorderColor = ScadaBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ScadaPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0C1622),
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // Manual Chapters / Sections
        items(displayedSections) { section ->
            val isExpanded = expandedSectionId == section.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedSectionId = if (isExpanded) null else section.id
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) Color(0xFF142433) else ScadaCardBg
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFF1B2C3D),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = section.category,
                                    color = ScadaPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = section.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = ScadaPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(section.summary, color = TextSecondary, fontSize = 11.5.sp)

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = ScadaBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = section.contentMarkdown,
                                color = Color(0xFFE0EBF5),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Troubleshooting Matrix Section
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
                            text = "คู่มือการแก้ไขปัญหาเบื้องต้นหน้างาน (Troubleshooting)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = ScadaWarning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Field Matrix",
                                color = ScadaWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    troubleshootingItems.forEach { item ->
                        TroubleshootingItemCard(item = item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Frequently Asked Questions (FAQ) Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScadaCardBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "คำถามที่พบบ่อย (Frequently Asked Questions - FAQ)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val faqs = listOf(
                        Pair(
                            "หากเน็ตตัดหรือไม่มี Wi-Fi ตู้ควบคุมยังทำงานอัตโนมัติได้หรือไม่?",
                            "ทำงานได้ตามปกติ 100% เนื่องจากลอจิกการตัดสินใจ (Autonomous Logic) ทำงานอยู่บนชิป ESP32-S3 โดยตรงภายในตู้ MDB และไม่พึ่งพาอินเทอร์เน็ตในการสั่งสตาร์ทหรือตัดปั๊มน้ำ ข้อมูลสถิติจะถูกเก็บไว้ในหน่วยความจำ RAM และส่งขึ้นคลาวด์อัตโนมัติเมื่อระบบต่อเน็ตได้"
                        ),
                        Pair(
                            "ปริมาตร 8,400 ลิตรต่อรอบ ได้มาอย่างไรและแม่นยำแค่ไหน?",
                            "คำนวณจากพื้นที่หน้าตัดถังพักน้ำเสียจริง กว้าง 3.0 m x ยาว 4.0 m = 12.0 m² เมื่อระดับน้ำลดลงจาก 90% (6.30 m) สู่ 80% (5.60 m) ระยะทางลดลง Δh = 0.70 m ดังนั้นปริมาตรน้ำสุทธิต่อรอบ = 12.0 x 0.70 = 8.40 m³ = 8,400 ลิตร มีความแม่นยำสูงกว่า 98% ของการตรวจวัดเชิงปริมาตร"
                        ),
                        Pair(
                            "ทำไมค่า ORP ต้องคุมให้ได้มากกว่าหรือเท่ากับ 650 mV?",
                            "ค่า ORP (Oxidation Reduction Potential) เป็นตัวชี้วัดความสามารถในการฆ่าเชื้อโรคของสารคลอรีนในทันที หากค่า ORP ≥ 650 mV จะสามารถทำลายเยื่อหุ้มเซลล์ของเชื้อแบคทีเรีย ไวรัส และโคลิฟอร์มในน้ำทิ้งโรงพยาบาลได้ภายในเวลาไม่กี่วินาทีตามมาตรฐานกระทรวงสาธารณสุข"
                        ),
                        Pair(
                            "การสั่งงานผ่านมือถือจะทำได้ในกรณีใดบ้างเพื่อความปลอดภัย?",
                            "สั่งงานได้เมื่อซีเล็คเตอร์สวิตช์หน้าตู้บิดอยู่ที่ AUTO เท่านั้น หากสวิตช์ถูกบิดไปที่ตำแหน่ง HAND หรือ OFF คำสั่งจากมือถือจะไม่สามารถแทรกแซงได้ ทั้งนี้เพื่อความปลอดภัยของช่างผู้ปฏิบัติงานหน้างานตามหลักวิศวกรรม"
                        )
                    )

                    faqs.forEachIndexed { idx, faq ->
                        val isFaqExpanded = expandedFaqIndex == idx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C1622), RoundedCornerShape(8.dp))
                                .clickable {
                                    expandedFaqIndex = if (isFaqExpanded) -1 else idx
                                }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q: ${faq.first}",
                                    color = ScadaPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isFaqExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (isFaqExpanded) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "A: ${faq.second}",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
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
fun TroubleshootingItemCard(item: TroubleshootingItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C1622),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.symptom,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (item.urgencyLevel == "CRITICAL") ScadaDanger.copy(alpha = 0.2f) else ScadaWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.urgencyLevel,
                        color = if (item.urgencyLevel == "CRITICAL") ScadaDanger else ScadaWarning,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("สาเหตุที่เป็นไปได้: ${item.possibleCauses.joinToString(", ")}", color = TextTertiary, fontSize = 10.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("วิธีดำเนินการแก้ไข:", color = ScadaPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            item.correctiveActions.forEach { action ->
                Text("• $action", color = TextSecondary, fontSize = 10.5.sp)
            }
        }
    }
}
