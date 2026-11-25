/* ALERTA EC · Web App oficial para iPhone (sin dependencias salvo Leaflet, incluido en vendor/). */
"use strict";

const CFG = window.ALERTA_EC_CONFIG || {};
const VERSION = CFG.version || "web";
const EC_BOUNDS = { latMin: -6, latMax: 2, lonMin: -92.5, lonMax: -75 }; // mismos límites que la base de datos

const CITIES = [
  ["Quito", -0.1807, -78.4678], ["Atucucho (Quito)", -0.1265, -78.515], ["Guayaquil", -2.1709, -79.9224],
  ["Cuenca", -2.9001, -79.0059], ["Santo Domingo", -0.253, -79.1754], ["Machala", -3.2581, -79.9554],
  ["Manta", -0.9677, -80.7089], ["Portoviejo", -1.0546, -80.4545], ["Ambato", -1.2491, -78.6168],
  ["Riobamba", -1.6636, -78.6546], ["Loja", -3.9931, -79.2042], ["Esmeraldas", 0.9682, -79.6517],
  ["Ibarra", 0.3517, -78.1223], ["Latacunga", -0.9352, -78.6155], ["Quevedo", -1.0225, -79.4604],
  ["Babahoyo", -1.8022, -79.5344], ["Santa Elena", -2.2262, -80.859], ["Tulcán", 0.8117, -77.7173],
  ["Guaranda", -1.5926, -79.001], ["Azogues", -2.7397, -78.8486], ["Tena", -0.9938, -77.8129],
  ["Puyo", -1.4924, -78.0024], ["Macas", -2.3087, -78.1114], ["Nueva Loja", 0.0847, -76.8828],
  ["Francisco de Orellana", -0.4664, -76.9871], ["Zamora", -4.0692, -78.9567],
  ["Puerto Ayora (Galápagos)", -0.7432, -90.3138], ["Puerto Baquerizo Moreno (Galápagos)", -0.9017, -89.6103],
];

/* Códigos WMO de Open-Meteo, en español. */
const WMO = {
  0: ["☀️", "Despejado"], 1: ["🌤️", "Mayormente despejado"], 2: ["⛅", "Parcialmente nublado"], 3: ["☁️", "Nublado"],
  45: ["🌫️", "Niebla"], 48: ["🌫️", "Niebla con escarcha"],
  51: ["🌦️", "Llovizna ligera"], 53: ["🌦️", "Llovizna"], 55: ["🌧️", "Llovizna intensa"],
  56: ["🌧️", "Llovizna helada"], 57: ["🌧️", "Llovizna helada intensa"],
  61: ["🌦️", "Lluvia ligera"], 63: ["🌧️", "Lluvia"], 65: ["🌧️", "Lluvia fuerte"],
  66: ["🌧️", "Lluvia helada"], 67: ["🌧️", "Lluvia helada fuerte"],
  71: ["🌨️", "Nieve ligera"], 73: ["🌨️", "Nieve"], 75: ["❄️", "Nieve fuerte"], 77: ["🌨️", "Granos de nieve"],
  80: ["🌦️", "Chubascos ligeros"], 81: ["🌧️", "Chubascos"], 82: ["⛈️", "Chubascos violentos"],
  85: ["🌨️", "Chubascos de nieve"], 86: ["🌨️", "Chubascos de nieve fuertes"],
  95: ["⛈️", "Tormenta eléctrica"], 96: ["⛈️", "Tormenta con granizo"], 99: ["⛈️", "Tormenta fuerte con granizo"],
};
const wmo = (c) => WMO[c] || ["🌡️", "Sin descripción"];

/* MET Norway (respaldo): símbolos a emoji y texto. */
function metSymbol(s) {
  s = s || "";
  if (s.includes("thunder")) return ["⛈️", "Tormenta eléctrica"];
  if (s.includes("heavyrain")) return ["🌧️", "Lluvia fuerte"];
  if (s.includes("lightrain")) return ["🌦️", "Lluvia ligera"];
  if (s.includes("rain")) return ["🌧️", "Lluvia"];
  if (s.includes("sleet") || s.includes("snow")) return ["🌨️", "Aguanieve o nieve"];
  if (s.includes("fog")) return ["🌫️", "Niebla"];
  if (s.includes("partlycloudy")) return ["⛅", "Parcialmente nublado"];
  if (s.includes("fair")) return ["🌤️", "Mayormente despejado"];
  if (s.includes("clearsky")) return ["☀️", "Despejado"];
  if (s.includes("cloudy")) return ["☁️", "Nublado"];
  return ["🌡️", "Sin descripción"];
}

/* ---------- Índice de riesgo de lluvia (misma fórmula que AlertaEcRainRisk.kt) ---------- */
const LEVELS = {
  CALM: ["🟢", "Tranquilo", "Sin señales de lluvia fuerte."],
  WATCH: ["🟡", "Atento", "Puede llover; mantente pendiente del cielo y de la app."],
  CAUTION: ["🟠", "Precaución", "Lluvia intensa posible: evita quebradas y zonas bajas."],
  DANGER: ["🔴", "Peligro", "Lluvia fuerte probable o en curso: no cruces calles inundadas y sigue avisos oficiales."],
};
const levelFor = (s) => (s >= 75 ? "DANGER" : s >= 50 ? "CAUTION" : s >= 25 ? "WATCH" : "CALM");
const fmt1 = (n) => n.toLocaleString("es-EC", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

function rainRisk(i) {
  const reasons = [];
  let score = 0;
  if (i.probabilityPercent != null) {
    const p = Math.max(0, Math.min(100, Math.round(i.probabilityPercent)));
    score += p * 0.35;
    reasons.push(`Probabilidad de lluvia: ${p} %`);
  }
  if (i.forecastMm3h != null && isFinite(i.forecastMm3h) && i.forecastMm3h >= 0) {
    score += Math.min(i.forecastMm3h / 20, 1) * 30;
    if (i.forecastMm3h > 0) reasons.push(`Pronóstico: ${fmt1(i.forecastMm3h)} mm en 3 h`);
  }
  if (i.rainingWithinMinutes != null) {
    const m = i.rainingWithinMinutes;
    score += m <= 5 ? 10 : 5;
    reasons.push(m <= 5 ? "Lluvia ahora o en minutos (minuto a minuto)" : `Lluvia en ~${m} min`);
  }
  if (i.communityReports > 0) {
    score += Math.min(i.communityReports * 5, 15);
    reasons.push(`Reportes de la comunidad cerca: ${i.communityReports}`);
  }
  const total = Math.max(0, Math.min(100, Math.round(score)));
  if (!reasons.length) reasons.push("Sin datos suficientes todavía");
  return { score: total, level: levelFor(total), reasons };
}

/* ---------- Utilidades ---------- */
const $ = (id) => document.getElementById(id);
function el(tag, attrs, ...kids) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (k === "class") n.className = v;
    else if (k === "text") n.textContent = v;
    else if (k.startsWith("on")) n.addEventListener(k.slice(2), v);
    else n.setAttribute(k, v);
  }
  for (const kid of kids) if (kid != null) n.append(kid);
  return n;
}
async function getJson(url, opts = {}, ms = 12000) {
  const ctl = new AbortController();
  const t = setTimeout(() => ctl.abort(), ms);
  try {
    const r = await fetch(url, { ...opts, signal: ctl.signal });
    if (!r.ok) throw new Error("HTTP " + r.status);
    return await r.json();
  } finally {
    clearTimeout(t);
  }
}
const store = {
  get(k, d) { try { const v = localStorage.getItem("alertaec." + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem("alertaec." + k, JSON.stringify(v)); } catch { /* modo privado */ } },
};
const round2 = (n) => Math.round(n * 100) / 100;
const inEcuador = (lat, lon) =>
  lat >= EC_BOUNDS.latMin && lat <= EC_BOUNDS.latMax && lon >= EC_BOUNDS.lonMin && lon <= EC_BOUNDS.lonMax;
const hourLabel = (iso) => iso.slice(11, 13) + "h";
function dayLabel(isoDate, idx) {
  if (idx === 0) return "Hoy";
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString("es-EC", { weekday: "short", day: "numeric" });
}

/* ---------- Estado ---------- */
let place = store.get("place", { name: "Quito", lat: -0.1807, lon: -78.4678 });
let reports = [];
let map, placeMarker, reportLayer;

/* ---------- Clima ---------- */
async function fetchOpenMeteo(lat, lon) {
  const u = new URL("https://api.open-meteo.com/v1/forecast");
  u.search = new URLSearchParams({
    latitude: lat, longitude: lon, timezone: "auto", forecast_days: 7, forecast_hours: 24, forecast_minutely_15: 8,
    wind_speed_unit: "kmh",
    current: "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m",
    minutely_15: "precipitation",
    hourly: "temperature_2m,precipitation_probability,precipitation,weather_code",
    daily: "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max",
  });
  const j = await getJson(u);
  const now = j.current.time;
  const minutely = (j.minutely_15?.time || []).map((t, i) => ({ t, mm: j.minutely_15.precipitation[i] }));
  const firstWet = minutely.find((m) => m.mm >= 0.1);
  const rainingWithinMinutes = firstWet
    ? Math.max(0, Math.round((Date.parse(firstWet.t + "Z") - Date.parse(now + "Z")) / 60000))
    : null;
  const hourly = j.hourly.time.map((t, i) => ({
    t, temp: j.hourly.temperature_2m[i], prob: j.hourly.precipitation_probability[i],
    mm: j.hourly.precipitation[i], icon: wmo(j.hourly.weather_code[i])[0],
  }));
  const next3 = hourly.slice(0, 3);
  return {
    source: "Open-Meteo",
    current: {
      temp: j.current.temperature_2m, feels: j.current.apparent_temperature, humidity: j.current.relative_humidity_2m,
      mm: j.current.precipitation, wind: j.current.wind_speed_10m, sky: wmo(j.current.weather_code),
    },
    hourly,
    daily: j.daily.time.map((d, i) => ({
      d, max: j.daily.temperature_2m_max[i], min: j.daily.temperature_2m_min[i], mm: j.daily.precipitation_sum[i],
      prob: j.daily.precipitation_probability_max[i], icon: wmo(j.daily.weather_code[i])[0],
    })),
    risk: {
      probabilityPercent: next3.length ? Math.max(...next3.map((h) => h.prob ?? 0)) : null,
      forecastMm3h: next3.reduce((a, h) => a + (h.mm || 0), 0),
      rainingWithinMinutes: rainingWithinMinutes != null && rainingWithinMinutes <= 60 ? rainingWithinMinutes : null,
    },
  };
}

async function fetchMetNo(lat, lon) {
  const j = await getJson(`https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${lat.toFixed(3)}&lon=${lon.toFixed(3)}`);
  // MET Norway trabaja en UTC; Ecuador continental es UTC-5.
  const local = (iso) => new Date(Date.parse(iso) - 5 * 3600e3).toISOString().slice(0, 16);
  const ts = j.properties.timeseries;
  const hourly = ts.slice(0, 24).map((s) => ({
    t: local(s.time), temp: s.data.instant.details.air_temperature, prob: null,
    mm: s.data.next_1_hours?.details?.precipitation_amount ?? 0,
    icon: metSymbol(s.data.next_1_hours?.summary?.symbol_code)[0],
  }));
  const byDay = new Map();
  for (const s of ts) {
    const t = local(s.time), d = t.slice(0, 10), x = s.data.instant.details.air_temperature;
    const day = byDay.get(d) || { d, max: -99, min: 99, mm: 0, prob: null, icon: "🌡️" };
    day.max = Math.max(day.max, x); day.min = Math.min(day.min, x);
    day.mm += s.data.next_1_hours?.details?.precipitation_amount ?? 0;
    if (t.slice(11, 13) === "12") day.icon = metSymbol((s.data.next_1_hours || s.data.next_6_hours)?.summary?.symbol_code)[0];
    byDay.set(d, day);
  }
  const first = ts[0].data;
  return {
    source: "MET Norway",
    current: {
      temp: first.instant.details.air_temperature, feels: null, humidity: first.instant.details.relative_humidity,
      mm: first.next_1_hours?.details?.precipitation_amount ?? 0, wind: first.instant.details.wind_speed * 3.6,
      sky: metSymbol(first.next_1_hours?.summary?.symbol_code),
    },
    hourly,
    daily: [...byDay.values()].slice(0, 7),
    risk: { probabilityPercent: null, forecastMm3h: hourly.slice(0, 3).reduce((a, h) => a + h.mm, 0), rainingWithinMinutes: null },
  };
}

async function loadWeather() {
  $("now").replaceChildren(el("p", { class: "muted", text: `Cargando el clima de ${place.name}…` }));
  let data;
  try {
    data = await fetchOpenMeteo(place.lat, place.lon);
  } catch {
    try {
      data = await fetchMetNo(place.lat, place.lon);
    } catch {
      $("now").replaceChildren(
        el("p", { class: "err", text: "No se pudo obtener el clima. Revisa tu conexión e inténtalo otra vez." }),
        el("button", { class: "btn", type: "button", text: "Reintentar", onclick: loadWeather })
      );
      return;
    }
  }
  renderNow(data);
  renderHours(data.hourly);
  renderDays(data.daily);
  window.__lastRisk = data.risk;
  renderRisk();
}

function renderNow(d) {
  const c = d.current;
  const t = (n) => (n == null ? "—" : Math.round(n) + "°");
  const cell = (v, label) => el("div", null, el("b", { text: v }), el("span", { text: label }));
  $("now").replaceChildren(
    el("p", { class: "where", text: "📍 " + place.name }),
    el("div", { class: "head" },
      el("span", { class: "icon", text: c.sky[0] }),
      el("div", null, el("div", { class: "temp", text: t(c.temp) }), el("div", { class: "muted", text: c.sky[1] }))),
    el("div", { class: "grid" },
      cell(t(c.feels), "Sensación"),
      cell(c.mm == null ? "—" : fmt1(c.mm) + " mm", "Lluvia ahora"),
      cell(c.humidity == null ? "—" : Math.round(c.humidity) + " %", "Humedad"),
      cell(c.wind == null ? "—" : Math.round(c.wind) + " km/h", "Viento"),
      cell(d.hourly[0]?.prob == null ? "—" : d.hourly[0].prob + " %", "Prob. lluvia"),
      cell(fmt1(d.daily[0]?.mm ?? 0) + " mm", "Lluvia hoy")),
    navigator.onLine ? null : el("p", { class: "offline", text: "Sin conexión: se muestran los últimos datos guardados." }),
    el("p", { class: "muted small", text: "Fuente: " + d.source })
  );
}

function renderHours(hours) {
  $("hours").replaceChildren(...hours.map((h) =>
    el("div", { role: "listitem" },
      el("span", { text: hourLabel(h.t) }), el("span", { class: "e", text: h.icon }),
      el("b", { text: Math.round(h.temp) + "°" }),
      el("span", { class: "p", text: h.prob != null ? `💧${h.prob}%` : `💧${fmt1(h.mm)}` }))));
}

function renderDays(days) {
  $("days").replaceChildren(...days.map((d, i) =>
    el("div", { role: "listitem" },
      el("span", { text: dayLabel(d.d, i) }), el("span", { text: d.icon }),
      el("span", { class: "p", text: `💧 ${d.prob != null ? d.prob + " % · " : ""}${fmt1(d.mm)} mm` }),
      el("span", { class: "t" }, el("b", { text: Math.round(d.max) + "°" }), el("span", { text: " / " + Math.round(d.min) + "°" })))));
}

function nearbyReports(hours = 12) {
  const since = Date.now() - hours * 3600e3;
  return reports.filter((r) => r.latitude != null && Date.parse(r.created_at) >= since &&
    Math.abs(r.latitude - place.lat) <= 0.5 && Math.abs(r.longitude - place.lon) <= 0.5);
}

function renderRisk() {
  if (!window.__lastRisk) return;
  const r = rainRisk({ ...window.__lastRisk, communityReports: nearbyReports().length });
  const [emoji, label, advice] = LEVELS[r.level];
  const box = $("risk");
  box.hidden = false;
  box.className = "card risk lv-" + r.level;
  const marker = el("i");
  marker.style.left = r.score + "%";
  box.replaceChildren(
    el("h2", { text: "Índice de riesgo de lluvia" }),
    el("div", { class: "row" },
      el("span", { class: "score", text: String(r.score) }),
      el("div", null, el("div", { class: "level", text: `${emoji} ${label}` }), el("div", { class: "muted small", text: advice }))),
    el("div", { class: "bar", role: "img", "aria-label": `Índice ${r.score} de 100` }, marker),
    el("div", { class: "scale" }, ...["Tranquilo", "Atento", "Precaución", "Peligro"].map((s) => el("span", { text: s }))),
    el("ul", null, ...r.reasons.map((x) => el("li", { text: x }))),
    el("p", { class: "muted small", text: "En la app de Android el índice suma además pluviómetros EPMAPS y el modelo INAMHI." })
  );
}

/* ---------- Lugar ---------- */
function setPlace(p, { fly = true } = {}) {
  place = { name: p.name, lat: +p.lat, lon: +p.lon };
  store.set("place", place);
  const sel = $("city");
  sel.value = CITIES.some(([n]) => n === place.name) ? place.name : "__custom";
  const custom = sel.querySelector('option[value="__custom"]');
  custom.textContent = sel.value === "__custom" ? "📍 " + place.name : "Otro lugar…";
  if (map) {
    placeMarker.setLatLng([place.lat, place.lon]);
    if (fly) map.setView([place.lat, place.lon], Math.max(map.getZoom(), 9));
  }
  loadWeather();
  renderReports();
}

async function placeName(lat, lon) {
  try {
    const j = await getJson(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=14&accept-language=es&lat=${lat}&lon=${lon}`, {}, 8000);
    const a = j.address || {};
    const small = a.neighbourhood || a.suburb || a.quarter || a.village || a.town;
    const big = a.city || a.town || a.county || a.state;
    if (small && big && small !== big) return `${small}, ${big}`;
    return small || big || "Mi ubicación";
  } catch {
    return "Mi ubicación";
  }
}

function useGps() {
  if (!("geolocation" in navigator)) return alert("Este navegador no permite usar la ubicación.");
  const btn = $("btn-gps");
  btn.textContent = "📍 Buscando…";
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      const { latitude: lat, longitude: lon } = pos.coords;
      btn.textContent = "📍 Mi ubicación";
      setPlace({ name: await placeName(lat, lon), lat, lon });
      store.set("gps", true);
    },
    (err) => {
      btn.textContent = "📍 Mi ubicación";
      alert(err.code === 1
        ? "Permiso de ubicación denegado. Actívalo en Ajustes → Privacidad → Localización → Safari, o elige una ciudad."
        : "No se pudo obtener tu ubicación. Elige una ciudad de la lista.");
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 300000 }
  );
}

async function search(ev) {
  ev.preventDefault();
  const q = $("q").value.trim();
  const list = $("results");
  if (q.length < 2) return;
  list.hidden = false;
  list.replaceChildren(el("li", null, el("button", { type: "button", text: "Buscando…" })));
  let found = [];
  try {
    const j = await getJson(`https://geocoding-api.open-meteo.com/v1/search?count=8&language=es&countryCode=EC&name=${encodeURIComponent(q)}`);
    found = (j.results || []).map((r) => ({ name: [r.name, r.admin2 || r.admin1].filter(Boolean).join(", "), lat: r.latitude, lon: r.longitude }));
  } catch { /* se intenta con Nominatim */ }
  if (!found.length) {
    try {
      const j = await getJson(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=8&countrycodes=ec&accept-language=es&q=${encodeURIComponent(q)}`);
      found = j.map((r) => ({ name: r.display_name.split(",").slice(0, 2).join(",").trim(), lat: +r.lat, lon: +r.lon }));
    } catch { /* sin resultados */ }
  }
  list.replaceChildren(...(found.length ? found : [null]).map((f) =>
    el("li", null, el("button", {
      type: "button", text: f ? f.name : "Sin resultados en Ecuador",
      onclick: () => { list.hidden = true; if (f) { $("q").value = ""; setPlace(f); } },
    }))));
}

/* ---------- Comunidad (Supabase, clave pública anon) ---------- */
const sbReady = () => !!(CFG.supabaseUrl && CFG.supabaseKey);
function sbHeaders(json) {
  const h = { apikey: CFG.supabaseKey, Accept: "application/json" };
  if (!CFG.supabaseKey.startsWith("sb_publishable_")) h.Authorization = "Bearer " + CFG.supabaseKey;
  if (json) { h["Content-Type"] = "application/json"; h.Prefer = "return=minimal"; }
  return h;
}
function sbError(code) {
  return ({ 401: "El servidor rechazó la solicitud (permisos).", 403: "El servidor rechazó la solicitud (permisos).",
    404: "La tabla comunitaria no está disponible.", 429: "Demasiados envíos seguidos; inténtalo en unos minutos." })[code]
    || ([400, 409, 422].includes(code) ? "El servidor rechazó el contenido (revisa la longitud del texto)." : `Servidor comunitario no disponible (código ${code}).`);
}
async function sbPost(table, body) {
  const r = await fetch(`${CFG.supabaseUrl}/rest/v1/${table}`, { method: "POST", headers: sbHeaders(true), body: JSON.stringify(body) });
  if (!r.ok) throw new Error(sbError(r.status));
}

async function loadReports() {
  if (!sbReady()) {
    $("reports").replaceChildren(el("p", { class: "muted small", text: "Los reportes de la comunidad no están disponibles en esta publicación." }));
    return;
  }
  const since = new Date(Date.now() - 24 * 3600e3).toISOString();
  try {
    reports = await getJson(
      `${CFG.supabaseUrl}/rest/v1/community_reports?select=id,severity,category,description,latitude,longitude,created_at` +
      `&created_at=gte.${encodeURIComponent(since)}&order=created_at.desc&limit=100`, { headers: sbHeaders(false) });
  } catch {
    reports = [];
    $("reports").replaceChildren(el("p", { class: "muted small", text: "No se pudieron cargar los reportes ahora." }));
  }
  drawReports();
  renderReports();
  renderRisk();
}

const SEV_COLOR = { Leve: "#2ecc71", Moderada: "#f1c40f", Alta: "#ff8c1a", "Crítica": "#ff3b30" };
const SEV_MARK = { Leve: "🟢", Moderada: "🟡", Alta: "🟠", "Crítica": "🔴" };
const FLAG_REASONS = ["Información falsa o engañosa", "Muestra rostros, placas o documentos", "Lenguaje ofensivo", "Publicidad o spam", "Otro motivo"];

function reportNode(r) {
  const when = new Date(r.created_at).toLocaleString("es-EC", { hour: "2-digit", minute: "2-digit", day: "numeric", month: "short" });
  const flagged = store.get("flagged", []);
  const reason = el("select", null, ...FLAG_REASONS.map((x) => el("option", { text: x })));
  const status = el("span", { class: "small" });
  const flag = flagged.includes(r.id)
    ? el("p", { class: "muted small", text: "Ya denunciaste este reporte." })
    : el("details", null, el("summary", { text: "Denunciar" }), reason,
        el("button", { class: "chip", type: "button", text: "Enviar denuncia", onclick: async (e) => {
          e.target.disabled = true;
          try {
            await sbPost("report_flags", { report_id: r.id, reason: reason.value.slice(0, 60) });
            store.set("flagged", [...flagged, r.id]);
            status.textContent = " Gracias, lo revisaremos.";
            status.className = "small ok";
          } catch (err) {
            status.textContent = " " + err.message;
            status.className = "small err";
            e.target.disabled = false;
          }
        } }), status);
  return el("div", { class: "report" },
    el("b", { text: `${SEV_MARK[r.severity] || "⚪"} ${r.category} · ${r.severity}` }),
    el("span", { class: "muted small", text: " · " + when }),
    el("p", { text: r.description }), flag);
}

function renderReports() {
  if (!sbReady()) return;
  const near = nearbyReports(24);
  const box = $("reports");
  if (!reports.length) return box.replaceChildren(el("p", { class: "muted small", text: "Sin reportes en las últimas 24 horas. ¡Sé el primero en avisar!" }));
  box.replaceChildren(
    el("p", { class: "small", text: near.length ? `${near.length} reporte(s) cerca de ${place.name}:` : `Sin reportes cerca de ${place.name}. Últimos en Ecuador:` }),
    ...(near.length ? near : reports).slice(0, 8).map(reportNode));
}

function drawReports() {
  if (!map) return;
  reportLayer.clearLayers();
  for (const r of reports) {
    if (r.latitude == null) continue;
    L.circleMarker([r.latitude, r.longitude], { radius: 8, color: "#07131f", weight: 2, fillColor: SEV_COLOR[r.severity] || "#9aa", fillOpacity: .95 })
      .bindPopup(reportNode(r), { maxWidth: 260 })
      .addTo(reportLayer);
  }
}

async function sendReport(ev) {
  ev.preventDefault();
  const st = $("r-status");
  const text = $("r-text").value.trim();
  if (!sbReady()) { st.className = "small err"; st.textContent = "Los reportes no están disponibles en esta publicación."; return; }
  if (text.length < 5) { st.className = "small err"; st.textContent = "Escribe al menos 5 caracteres."; return; }
  const last = store.get("lastReport", 0);
  if (Date.now() - last < 60000) { st.className = "small err"; st.textContent = "Espera un minuto antes de enviar otro reporte."; return; }
  const body = {
    category: $("r-category").value.slice(0, 60),
    severity: $("r-severity").value,
    description: text.slice(0, 1000),
    app_version: ("web-iphone-" + VERSION).slice(0, 40),
  };
  if ($("r-gps").checked && inEcuador(place.lat, place.lon)) {
    body.latitude = round2(place.lat);
    body.longitude = round2(place.lon);
  }
  const btn = ev.submitter || ev.target.querySelector("button");
  btn.disabled = true;
  st.className = "small"; st.textContent = "Enviando…";
  try {
    await sbPost("community_reports", body);
    store.set("lastReport", Date.now());
    st.className = "small ok"; st.textContent = "✅ Reporte enviado. ¡Gracias por avisar a tu comunidad!";
    $("r-text").value = "";
    loadReports();
  } catch (err) {
    st.className = "small err";
    st.textContent = err.message.includes("fetch") ? "Sin conexión con el servidor comunitario. Comprueba Internet." : err.message;
  } finally {
    btn.disabled = false;
  }
}

/* ---------- Mapa ---------- */
function initMap() {
  if (!window.L) {
    $("map").replaceChildren(el("p", { class: "muted small", text: "El mapa no se pudo cargar." }));
    return;
  }
  map = L.map("map", { zoomControl: true, attributionControl: true, maxBounds: [[-7, -94], [3, -73]], minZoom: 5 })
    .setView([place.lat, place.lon], 7);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 18, attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);
  placeMarker = L.circleMarker([place.lat, place.lon], { radius: 10, color: "#00e5ff", weight: 3, fillOpacity: .25 }).addTo(map);
  reportLayer = L.layerGroup().addTo(map);
  map.on("click", (e) => {
    const { lat, lng } = e.latlng;
    if (!inEcuador(lat, lng)) return;
    placeName(lat, lng).then((name) => setPlace({ name, lat, lon: lng }, { fly: false }));
  });
}

/* ---------- Guía de instalación (iPhone/iPad) ---------- */
function initInstallGuide() {
  const ua = navigator.userAgent;
  const iOS = /iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  const standalone = navigator.standalone === true || matchMedia("(display-mode: standalone)").matches;
  if (!iOS || standalone || store.get("installHidden", false)) return;
  const box = $("install");
  box.hidden = false;
  if (/CriOS|FxiOS|EdgiOS|FBAN|FBAV|Instagram|WhatsApp|Line\//.test(ua)) $("install-safari").hidden = false;
  box.querySelector(".close").addEventListener("click", () => { box.hidden = true; store.set("installHidden", true); });
}

/* ---------- Inicio ---------- */
function init() {
  const sel = $("city");
  sel.replaceChildren(...CITIES.map(([n]) => el("option", { value: n, text: n })), el("option", { value: "__custom", text: "Otro lugar…" }));
  sel.addEventListener("change", () => {
    const c = CITIES.find(([n]) => n === sel.value);
    if (c) setPlace({ name: c[0], lat: c[1], lon: c[2] });
    else $("q").focus();
  });
  $("btn-gps").addEventListener("click", useGps);
  $("search").addEventListener("submit", search);
  $("report-form").addEventListener("submit", sendReport);
  initInstallGuide();
  initMap();
  setPlace(place, { fly: false });
  loadReports();
  if (store.get("gps", false)) useGps();
  if ("serviceWorker" in navigator) navigator.serviceWorker.register("service-worker.js").catch(() => {});
  // Refresca al volver a la app (las PWA de iOS se quedan suspendidas en segundo plano).
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible" && Date.now() - (window.__lastLoad || 0) > 10 * 60000) {
      window.__lastLoad = Date.now();
      loadWeather();
      loadReports();
    }
  });
  window.__lastLoad = Date.now();
}
document.addEventListener("DOMContentLoaded", init);
