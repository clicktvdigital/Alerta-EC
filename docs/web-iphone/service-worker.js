/* ALERTA EC · Web App para iPhone: caché básica.
 * - La interfaz (HTML, CSS, JS, íconos, Leaflet) se sirve desde la caché y se actualiza en segundo plano.
 * - El clima y los reportes van siempre a la red; la última respuesta del clima queda guardada
 *   para mostrarla si no hay conexión.
 * - El mapa base (teselas de OpenStreetMap) no se guarda.
 */
const VERSION = "__ALERTAEC_VERSION__";
const SHELL = "alertaec-shell-" + VERSION;
const DATA = "alertaec-datos-v1";
const SHELL_FILES = [
  "./",
  "index.html",
  "app.css?v=" + VERSION,
  "app.js?v=" + VERSION,
  "config.js?v=" + VERSION,
  "manifest.webmanifest",
  "icons/apple-touch-icon.png",
  "icons/icon-192.png",
  "vendor/leaflet/leaflet.css",
  "vendor/leaflet/leaflet.js",
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(SHELL).then((c) => c.addAll(SHELL_FILES)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith("alertaec-shell-") && k !== SHELL).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);

  // Clima: red primero; si falla, la última respuesta guardada.
  if (url.hostname === "api.open-meteo.com" || url.hostname === "api.met.no") {
    event.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) caches.open(DATA).then((c) => c.put(req, res.clone()));
          return res;
        })
        .catch(() => caches.match(req).then((hit) => hit || Response.error()))
    );
    return;
  }

  // Interfaz propia: caché primero y actualización en segundo plano.
  if (url.origin === self.location.origin && url.pathname.includes("/iphone/")) {
    event.respondWith(
      caches.open(SHELL).then((cache) =>
        cache.match(req).then((hit) => {
          const net = fetch(req)
            .then((res) => {
              if (res.ok) cache.put(req, res.clone());
              return res;
            })
            .catch(() => hit || cache.match("index.html"));
          return hit || net;
        })
      )
    );
  }
});
