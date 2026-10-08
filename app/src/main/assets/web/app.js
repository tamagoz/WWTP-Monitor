// State Management
let telemetry = {
  waterLevel: 6.15,
  ph: 7.35,
  doVal: 3.82,
  turb: 14.5,
  orp: 685,
  powerKw: 7.2,
  flowRate: 42.0,
  pumpP1: true,
  pumpP2: true,
  pumpP3: false,
  pumpP4: true,
  pumpP5: true,
  cyclesToday: 14,
  litersToday: 117600,
  monthlyM3: 2856.0,
  qaPass: true,
  cycleDir: -1
};

const historyData = [];

document.addEventListener('DOMContentLoaded', () => {
  initClock();
  initTableData();
  startSimulationLoop();
  initChart();
});

function initClock() {
  setInterval(() => {
    const now = new Date();
    document.getElementById('liveClock').innerText = now.toLocaleTimeString('th-TH');
  }, 1000);
}

function switchTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));

  event.target.classList.add('active');
  const targetPane = document.getElementById('tab-' + tabId);
  if (targetPane) targetPane.classList.add('active');
}

function startSimulationLoop() {
  setInterval(() => {
    if (telemetry.pumpP1) {
      telemetry.waterLevel -= 0.012;
      if (telemetry.waterLevel <= 5.60) {
        telemetry.waterLevel = 5.60;
        telemetry.pumpP1 = false;
        telemetry.cyclesToday += 1;
        telemetry.litersToday += 8400;
        telemetry.monthlyM3 += 8.4;
        addHistoryRow(8400);
      }
    } else {
      telemetry.waterLevel += 0.015;
      if (telemetry.waterLevel >= 6.30) {
        telemetry.waterLevel = 6.30;
        telemetry.pumpP1 = true;
      }
    }

    telemetry.ph = +(7.30 + Math.sin(Date.now() / 20000) * 0.15).toFixed(2);
    telemetry.doVal = +(3.80 + Math.cos(Date.now() / 15000) * 0.25).toFixed(2);
    telemetry.orp = Math.round(680 + Math.sin(Date.now() / 25000) * 15);
    telemetry.turb = +(14.0 + Math.random() * 1.5).toFixed(1);
    telemetry.powerKw = +(telemetry.pumpP1 ? 7.2 + Math.random() * 0.3 : 5.6).toFixed(1);

    updateUI();
  }, 1000);
}

function updateUI() {
  const pct = Math.min(100, Math.max(0, (telemetry.waterLevel / 7.0) * 100));
  document.getElementById('tankFluid').style.height = pct.toFixed(1) + '%';
  document.getElementById('tankLevelPctBadge').innerText = `ระดับ ${pct.toFixed(1)}%`;
  document.getElementById('waterLevelVal').innerHTML = `${telemetry.waterLevel.toFixed(2)} <small>m</small>`;
  document.getElementById('flowRateVal').innerHTML = `${telemetry.pumpP1 ? '42.0' : '0.0'} <small>m³/ชม.</small>`;
  document.getElementById('powerVal').innerHTML = `${telemetry.powerKw} <small>kW</small>`;

  document.getElementById('val-ph').innerText = telemetry.ph.toFixed(2);
  document.getElementById('bar-ph').style.width = ((telemetry.ph / 14) * 100) + '%';

  document.getElementById('val-do').innerHTML = `${telemetry.doVal.toFixed(2)} <small>mg/L</small>`;
  document.getElementById('bar-do').style.width = ((telemetry.doVal / 8) * 100) + '%';

  document.getElementById('val-orp').innerHTML = `${telemetry.orp} <small>mV</small>`;
  document.getElementById('bar-orp').style.width = Math.min(100, (telemetry.orp / 900) * 100) + '%';

  document.getElementById('val-turb').innerHTML = `${telemetry.turb} <small>NTU</small>`;
  document.getElementById('bar-turb').style.width = ((telemetry.turb / 40) * 100) + '%';

  document.getElementById('cycleCounter').innerText = telemetry.cyclesToday;
  document.getElementById('dailyVolume').innerText = telemetry.litersToday.toLocaleString();
  document.getElementById('repDailyCycles').innerText = telemetry.cyclesToday;
  document.getElementById('repDailyLiters').innerText = telemetry.litersToday.toLocaleString();
  document.getElementById('repMonthlyM3').innerText = telemetry.monthlyM3.toFixed(1);

  const p1El = document.getElementById('pumpP1Status');
  if (telemetry.pumpP1) {
    p1El.className = 'status-badge running';
    p1El.innerText = 'กำลังทำงาน (RUN)';
  } else {
    p1El.className = 'status-badge stopped';
    p1El.innerText = 'หยุด (STANDBY 80%)';
  }

  historyData.push({
    time: new Date().toLocaleTimeString('th-TH'),
    ph: telemetry.ph,
    do: telemetry.doVal,
    lvl: telemetry.waterLevel
  });
  if (historyData.length > 30) historyData.shift();

  drawChart();
}

function initTableData() {
  const tbody = document.getElementById('reportTableBody');
  const now = new Date();
  for (let i = 8; i >= 1; i--) {
    const t = new Date(now.getTime() - i * 3600000);
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>${t.toLocaleTimeString('th-TH')}</td>
      <td>5.62</td>
      <td>7.34</td>
      <td>3.88</td>
      <td>685</td>
      <td>13.8</td>
      <td><span class="status-badge stopped">STOP</span></td>
      <td>+8,400 ลิตร</td>
      <td><span class="status-badge running">PASS</span></td>
    `;
    tbody.appendChild(row);
  }
}

function addHistoryRow(volumeLiters) {
  const tbody = document.getElementById('reportTableBody');
  const row = document.createElement('tr');
  const now = new Date();
  row.innerHTML = `
    <td>${now.toLocaleTimeString('th-TH')}</td>
    <td>${telemetry.waterLevel.toFixed(2)}</td>
    <td>${telemetry.ph.toFixed(2)}</td>
    <td>${telemetry.doVal.toFixed(2)}</td>
    <td>${telemetry.orp}</td>
    <td>${telemetry.turb}</td>
    <td><span class="status-badge stopped">STOP</span></td>
    <td><strong>+${volumeLiters.toLocaleString()} ลิตร</strong></td>
    <td><span class="status-badge running">PASS</span></td>
  `;
  tbody.insertBefore(row, tbody.firstChild);
}

let canvas, ctx;
function initChart() {
  canvas = document.getElementById('trendCanvas');
  if (canvas) ctx = canvas.getContext('2d');
}

function drawChart() {
  if (!ctx || historyData.length < 2) return;
  const w = canvas.width;
  const h = canvas.height;

  ctx.clearRect(0, 0, w, h);

  ctx.strokeStyle = '#1b2d3e';
  ctx.lineWidth = 1;
  for (let y = 30; y < h; y += 40) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }

  const drawLine = (prop, minVal, maxVal, color) => {
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;

    historyData.forEach((pt, idx) => {
      const x = (idx / (historyData.length - 1)) * (w - 40) + 20;
      const norm = (pt[prop] - minVal) / (maxVal - minVal);
      const y = h - 20 - norm * (h - 50);
      if (idx === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();
  };

  drawLine('ph', 6.0, 9.0, '#00d2ff');
  drawLine('do', 1.0, 6.0, '#00e676');
  drawLine('lvl', 5.0, 7.0, '#ffd600');
}

function filterManual() {
  const q = document.getElementById('manualSearchInput').value.toLowerCase().trim();
  const sections = document.querySelectorAll('.manual-section');
  sections.forEach(sec => {
    const text = (sec.innerText + ' ' + (sec.dataset.keywords || '')).toLowerCase();
    if (!q || text.includes(q)) {
      sec.style.display = 'block';
    } else {
      sec.style.display = 'none';
    }
  });
}

function downloadCSV() {
  let csv = '\uFEFFวันที่,เวลา,ระดับน้ำ (m),pH,DO (mg/L),ORP (mV),ความขุ่น (NTU),ปริมาตรรอบ (ลิตร),สถานะ QA\n';
  const rows = document.querySelectorAll('#reportTableBody tr');
  rows.forEach(r => {
    const cols = Array.from(r.querySelectorAll('td')).map(c => `"${c.innerText.replace(/"/g, '""')}"`);
    if (cols.length) csv += cols.join(',') + '\n';
  });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `WWTP_Chumphae_QA_${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
}

function exportReportPDF() {
  window.print();
}
