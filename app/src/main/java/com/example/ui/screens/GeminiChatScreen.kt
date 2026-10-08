package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.*

@Composable
fun GeminiChatScreen(
    chatMessages: List<ChatMessage>,
    isAiThinking: Boolean,
    onSendMessage: (String, Boolean) -> Unit,
    onRequestDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var useHighThinking by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size, isAiThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScadaDarkBg)
    ) {
        // AI Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ScadaCardBg,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ScadaPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI วิศวกรผู้เชี่ยวชาญ",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (useHighThinking) "Model: gemini-3.1-pro-preview (Thinking: HIGH)" else "Model: gemini-3.5-flash",
                            color = ScadaPrimary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // High Thinking Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "High Thinking",
                        color = if (useHighThinking) ScadaPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = useHighThinking,
                        onCheckedChange = { useHighThinking = it },
                        modifier = Modifier.testTag("toggle_high_thinking"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ScadaPrimary,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF1E354A)
                        )
                    )
                }
            }
        }

        // Suggested Action Chips
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0C1622)
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ActionChip(
                        icon = Icons.Default.Analytics,
                        label = "วิเคราะห์สถานะระบบทันที",
                        onClick = onRequestDiagnostics
                    )
                }
                item {
                    ActionChip(
                        icon = Icons.Default.WaterDrop,
                        label = "ประเมินค่า pH, DO และ ORP",
                        onClick = {
                            onSendMessage("ช่วยประเมินความสัมพันธ์ระหว่างค่า pH, DO และ ORP ต่อการทำงานของระบบบำบัดน้ำเสียโรงพยาบาล", useHighThinking)
                        }
                    )
                }
                item {
                    ActionChip(
                        icon = Icons.Default.Warning,
                        label = "วิเคราะห์สาเหตุ Overload มอเตอร์",
                        onClick = {
                            onSendMessage("หากปั๊มสูบน้ำเสีย P1 เกิดอาการ Overload Trip บ่อยครั้ง มีสาเหตุและขั้นตอนตรวจสอบแก้ไขอย่างไร?", useHighThinking)
                        }
                    )
                }
                item {
                    ActionChip(
                        icon = Icons.Default.Calculate,
                        label = "คำนวณปริมาณคลอรีนฆ่าเชื้อ",
                        onClick = {
                            onSendMessage("โปรดคำนวณอัตราการจ่ายสารละลายคลอรีนสำหรับน้ำทิ้งปริมาตร 8,400 ลิตร เพื่อให้ได้ค่า ORP ≥ 650 mV", useHighThinking)
                        }
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages) { message ->
                ChatBubble(message = message)
            }

            if (isAiThinking) {
                item {
                    ThinkingIndicatorBubble(useHighThinking = useHighThinking)
                }
            }
        }

        // Input Field Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ScadaCardBg,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gemini_chat_input"),
                    placeholder = { Text("พิมพ์คำถามหรือปรึกษาปัญหาตู้ควบคุม...", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScadaPrimary,
                        unfocusedBorderColor = ScadaBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim(), useHighThinking)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ScadaPrimary)
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF162737),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ScadaPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF007299) else ScadaCardBg,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = if (!isUser) CardDefaults.outlinedCardBorder() else null,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = ScadaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "วิศวกรผู้เชี่ยวชาญ SCADA",
                            color = ScadaPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ThinkingIndicatorBubble(useHighThinking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "think")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "think_alpha"
    )

    Surface(
        color = ScadaCardBg,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = ScadaPrimary.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (useHighThinking) "กำลังคิดและวิเคราะห์เชิงลึก (Thinking: HIGH)..." else "กำลังประมวลผลคำตอบ...",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ใช้ gemini-3.1-pro-preview เพื่อวิเคราะห์พารามิเตอร์และสาเหตุปัญหา",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
