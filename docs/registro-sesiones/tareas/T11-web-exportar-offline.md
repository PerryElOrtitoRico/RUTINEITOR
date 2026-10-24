# T11 — App del celular: exportar y funcionar sin conexión

## Reglas para las tareas de la app web
Las mismas de T09.

## Objetivo
1. Exportar el registro como archivo, para pasarlo a la PC.
2. Que la app funcione sin internet una vez abierta, y se pueda "agregar a la pantalla de inicio".

## Archivos
- Modificar `celular/index.html` (botón "Exportar registro" y registro del service worker).
- Crear `celular/manifest.webmanifest`, `celular/sw.js` y `celular/icono.png`.

## Exportar registro
- Al tocar **"Exportar registro"**, se toma el registro del plan activo y se genera `JSON.stringify(registro, null, 2)` con nombre `Logica.nombreArchivoRegistro(plan)`.
- Si el navegador tiene `navigator.canShare` y `navigator.canShare({ files: [archivo] })`, se usa `navigator.share({ files: [archivo] })`: en Android abre el menú de compartir (Drive, WhatsApp…).
- Si no, se descarga con un `<a download>` sobre un `Blob` (`application/json`); en Android queda en "Descargas".
- Si el registro no tiene sesiones, se avisa "Todavía no anotaste ninguna sesión" y no se exporta.

## Sin conexión e instalable
- `manifest.webmanifest`:
  - `name: "RUTINEITOR Registro"`, `short_name: "Rutineitor"`;
  - `start_url: "./"`, `scope: "./"`, `display: "standalone"`;
  - `background_color` y `theme_color` a elección;
  - `icons` con `icono.png` (`"sizes": "512x512"`, `"type": "image/png"`).
- `icono.png`: **copiá** `docs/icono/rutineitor-512.png` (el ícono del programa de la PC, 512 × 512); no dibujes uno nuevo.
- `index.html`: `<link rel="manifest" href="manifest.webmanifest">`, `<meta name="theme-color" …>` y, al cargar, `navigator.serviceWorker.register("sw.js")` si existe `serviceWorker` (dentro de `try/catch`, y sin romper nada si se abre como `file://`).
- `sw.js`:
  - en `install`, guarda en `caches` los archivos `./`, `./index.html`, `./logica.js`, `./manifest.webmanifest` y `./icono.png` (cache con nombre versionado, por ejemplo `rutineitor-v1`);
  - en `activate`, borra los caches de versiones anteriores;
  - en `fetch`, responde primero desde el cache y, si no está, va a la red.
- Todas las rutas son **relativas** (`./`), porque la app se publica en `/rutineitor/`, no en la raíz del dominio.

## Cómo verificar
- En la PC: con la app abierta desde un servidor local (`python3 -m http.server 8000` dentro de `celular/` y Chrome en `http://localhost:8000`), F12 → Application:
  - "Manifest" sin errores;
  - "Service workers" con `sw.js` activado;
  - con "Offline" tildado, recargar: la app sigue abriendo.
- Cargar el plan de ejemplo, anotar dos sesiones y exportar: el archivo descargado se lee sin errores con `JsonRegistro.leerRegistro` (T05) y tiene la forma de la sección 6.
