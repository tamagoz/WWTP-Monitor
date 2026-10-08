package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WwtpViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold()
            }
        }
    }
}

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("แดชบอร์ด", Icons.Default.Dashboard),
    PROCESS("ผังกระบวนการ", Icons.Default.AccountTree),
    ANALYTICS("สถิติ/PDF", Icons.Default.QueryStats),
    MANUAL("คู่มือ/FAQ", Icons.Default.MenuBook),
    GEMINI_AI("AI วิศวกร", Icons.Default.Psychology),
    WEB_SCADA("เว็บแอป", Icons.Default.Language)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: WwtpViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    val sensorReading by viewModel.sensorReading.collectAsStateWithLifecycle()
    val cabinetStatus by viewModel.cabinetStatus.collectAsStateWithLifecycle()
    val pumpCycles by viewModel.pumpCycles.collectAsStateWithLifecycle()
    val qaEvaluation by viewModel.qaEvaluation.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val manualSections by viewModel.manualSections.collectAsStateWithLifecycle()
    val ntfyTopic by viewModel.ntfyTopic.collectAsStateWithLifecycle()
    val ntfyEnabled by viewModel.ntfyEnabled.collectAsStateWithLifecycle()
    val reportIntervalHours by viewModel.reportIntervalHours.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ScadaPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "WWTP",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "บำบัดน้ำเสีย รพ.ชุมแพ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "2568-16-Chumpea-1-WaterPump",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = if (qaEvaluation.isCompliant) ScadaSuccess.copy(alpha = 0.15f) else ScadaDanger.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (qaEvaluation.isCompliant) ScadaSuccess else ScadaDanger)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (qaEvaluation.isCompliant) "QA PASS" else "QA FAIL",
                                color = if (qaEvaluation.isCompliant) ScadaSuccess else ScadaDanger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.exportPdfReport(context) },
                        modifier = Modifier.testTag("top_pdf_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Report",
                            tint = ScadaPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScadaCardBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ScadaCardBg,
                contentColor = TextPrimary
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = ScadaPrimary,
                            indicatorColor = ScadaPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    sensorReading = sensorReading,
                    cabinetStatus = cabinetStatus,
                    qaEvaluation = qaEvaluation,
                    onModeChange = { viewModel.setControlMode(it) },
                    onEmergencyStop = { viewModel.triggerEmergencyStop() },
                    onResetEmergencyStop = { viewModel.resetEmergencyStop() },
                    onTogglePump1 = { viewModel.togglePump1() }
                )
                NavigationTab.PROCESS -> ProcessFlowScreen(
                    pinDefinitions = viewModel.pinDefinitions
                )
                NavigationTab.ANALYTICS -> AnalyticsScreen(
                    reading = sensorReading,
                    cabinetStatus = cabinetStatus,
                    pumpCycles = pumpCycles,
                    ntfyTopic = ntfyTopic,
                    ntfyEnabled = ntfyEnabled,
                    reportIntervalHours = reportIntervalHours,
                    onNtfyTopicChange = { viewModel.setNtfyTopic(it) },
                    onNtfyEnabledChange = { viewModel.setNtfyEnabled(it) },
                    onReportIntervalChange = { viewModel.setReportIntervalHours(it) },
                    onTestNtfy = { viewModel.testSendNtfy(it) },
                    onSendReportNow = { viewModel.sendPeriodicReportNow(it) },
                    onExportPdf = { viewModel.exportPdfReport(it) }
                )
                NavigationTab.MANUAL -> ManualFaqScreen(
                    manualSections = manualSections,
                    troubleshootingItems = viewModel.troubleshootingItems,
                    searchKeyword = searchKeyword,
                    onSearchChange = { viewModel.setSearchKeyword(it) },
                    onExportPdf = { viewModel.exportPdfReport(it) }
                )
                NavigationTab.GEMINI_AI -> GeminiChatScreen(
                    chatMessages = chatMessages,
                    isAiThinking = isAiThinking,
                    onSendMessage = { text, highThinking -> viewModel.sendChatMessage(text, highThinking) },
                    onRequestDiagnostics = { viewModel.requestAiDiagnostics() }
                )
                NavigationTab.WEB_SCADA -> WebScadaScreen()
            }
        }
    }
}
