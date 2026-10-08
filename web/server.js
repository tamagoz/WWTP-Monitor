/**
 * ============================================================================
 * Asynchronous Web & SCADA Server (Node.js Express + WebSocket + MQTT)
 * สถานที่: โรงพยาบาลชุมแพ จังหวัดขอนแก่น
 * โครงการ: 2568-16-Chumpea-1-WaterPump
 * ============================================================================
 */

const express = require('express');
const http = require('http');
const path = require('path');
const fs = require('fs');
const mqtt = require('mqtt');

const app = express();
const server = http.createServer(app);

const PORT = process.env.PORT || 3000;
const MQTT_BROKER = process.env.MQTT_BROKER || 'mqtt://localhost:1883';
const MQTT_TOPIC = 'hospital/wastewater/stream';

// เสิร์ฟไฟล์หน้าเว็บสถิติและแดชบอร์ด (Responsive Web App)
app.use(express.static(path.join(__dirname)));

// API ส่งข้อมูลสถานะปัจจุบัน
let latestTelemetry = {
  waterLevel: 6.15,
  ph: 7.35,
  doVal: 3.82,
  turb: 14.5,
  orp: 685,
  pumpState: 1,
  dailyCycles: 14,
  dailyLiters: 117600,
  monthlyM3: 2856.0,
  qaStatus: 'PASS',
  ntfyTopic: 'chumphae_wwtp_alerts',
  reportIntervalHours: 4
};

// ฟังก์ชันส่งข้อความเข้าแอป NTFY (HTTPS Request)
function sendNtfyAlert(title, message, priority = 'urgent', tags = 'warning,hospital') {
  const https = require('https');
  const req = https.request(`https://ntfy.sh/${latestTelemetry.ntfyTopic}`, {
    method: 'POST',
    headers: {
      'Title': title,
      'Priority': priority,
      'Tags': tags
    }
  }, (res) => {
    console.log(`[NTFY] ส่งการแจ้งเตือนสำเร็จ (HTTP ${res.statusCode})`);
  });
  req.on('error', (e) => console.error('[NTFY Error]', e.message));
  req.write(message);
  req.end();
}

app.post('/api/ntfy/test', (req, res) => {
  sendNtfyAlert(
    '🔔 [WWTP TEST] ทดสอบการเชื่อมต่อ NTFY',
    '✅ ระบบแจ้งเตือนตู้ควบคุมบำบัดน้ำเสีย รพ.ชุมแพ เชื่อมต่อพร้อมทำงาน',
    'high',
    'white_check_mark,bell'
  );
  res.json({ success: true, message: 'Sent test alert' });
});

app.post('/api/ntfy/report', (req, res) => {
  const repMsg = `📊 สรุปผลรอบ ${latestTelemetry.reportIntervalHours} ชม. รพ.ชุมแพ
• รอบสูบวันนี้: ${latestTelemetry.dailyCycles} ครั้ง
• ปริมาณน้ำวันนี้: ${latestTelemetry.dailyLiters.toLocaleString()} ลิตร (+8,400L/รอบ)
• ปริมาณสะสมเดือนนี้: ${latestTelemetry.monthlyM3} m³
• pH: ${latestTelemetry.ph} | DO: ${latestTelemetry.doVal} mg/L | ORP: ${latestTelemetry.orp} mV`;
  sendNtfyAlert(`📊 สรุปสถานะบำบัดน้ำเสีย รพ.ชุมแพ (${latestTelemetry.reportIntervalHours}h)`, repMsg, 'default', 'bar_chart,droplet');
  res.json({ success: true, message: 'Sent periodic report' });
});

app.get('/api/telemetry', (req, res) => {
  res.json(latestTelemetry);
});

// API ส่งออกข้อมูลไฟล์รายงานย้อนหลัง
app.get('/api/reports', (req, res) => {
  const reportsDir = path.join(__dirname, 'reports');
  if (fs.existsSync(reportsDir)) {
    const files = fs.readdirSync(reportsDir);
    return res.json({ success: true, files });
  }
  res.json({ success: true, files: [] });
});

// เริ่มต้นเชื่อมต่อ Local MQTT Mosquitto Broker
console.log(`[SCADA Server] กำลังเชื่อมต่อ MQTT Broker: ${MQTT_BROKER}...`);
try {
  const client = mqtt.connect(MQTT_BROKER, { reconnectPeriod: 3000 });
  client.on('connect', () => {
    console.log('[SCADA Server] เชื่อมต่อ MQTT สำเร็จ รอรับข้อมูลบ่อบำบัดน้ำเสีย');
    client.subscribe(MQTT_TOPIC);
  });

  client.on('message', (topic, message) => {
    try {
      const data = JSON.parse(message.toString());
      latestTelemetry.waterLevel = data.lvl || latestTelemetry.waterLevel;
      latestTelemetry.ph = data.ph || latestTelemetry.ph;
      latestTelemetry.doVal = data.do || latestTelemetry.doVal;
      latestTelemetry.orp = data.orp || latestTelemetry.orp;
      latestTelemetry.turb = data.turb || latestTelemetry.turb;
      latestTelemetry.pumpState = data.pump_status !== undefined ? data.pump_status : latestTelemetry.pumpState;
    } catch (e) {
      // Ignore format error
    }
  });
} catch (e) {
  console.log('[SCADA Server] MQTT Offline, ระบบรันในโหมด Standalone Simulation');
}

server.listen(PORT, () => {
  console.log(`[SCADA Server] เว็บแอปพลิเคชันพร้อมใช้งานที่พอร์ต http://localhost:${PORT}`);
});
