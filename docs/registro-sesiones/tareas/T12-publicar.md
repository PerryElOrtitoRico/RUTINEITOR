# T12 — Publicar la app en GitHub Pages

Esta tarea es de pasos, no de código. La puede hacer el usuario, el LLM o Claude. Hacen falta acceso SSH a GitHub (ya configurado: cuenta `PerryElOrtitoRico`) y el repositorio vacío `rutineitor` creado en github.com.

## Qué se publica
**Solo la carpeta `celular/`.** El resto del repo (planillas, foto de la rutina, base de datos) no se sube. Por eso se publica desde una rama propia, armada con `git subtree split`, que contiene únicamente lo de `celular/`.

## Pasos (desde la raíz de `ProyectoRutina`, con `celular/` ya commiteado en `main`)
1. Crear la rama solo con la app:
   ```bash
   git subtree split --prefix celular -b pages
   ```
2. Comprobar que no se cuela nada más: `git ls-tree -r --name-only pages` tiene que listar solo `index.html`, `logica.js`, `manifest.webmanifest`, `sw.js` e `icono.svg`.
3. Agregar el remoto, solo la primera vez:
   ```bash
   git remote add pages git@github.com:PerryElOrtitoRico/rutineitor.git
   ```
4. Subir la rama `pages` como `main` del repositorio público:
   ```bash
   git push pages pages:main
   ```
5. En github.com → repositorio `rutineitor` → **Settings → Pages**: *Source: Deploy from a branch*, *Branch: `main` / `(root)`* → **Save**.
6. Esperar 1–2 minutos y abrir `https://perryelortitorico.github.io/rutineitor/`.

## Actualizar después de un cambio en `celular/`
```bash
git subtree split --prefix celular -b pages
git push pages pages:main
```
Subí también el número del cache en `sw.js` (`rutineitor-v2`, `v3`…). Si no, el celular sigue mostrando la versión vieja.

## En el celular
1. Abrir la dirección en Chrome.
2. Menú ⋮ → **"Agregar a la pantalla principal"** (o "Instalar app").
3. Abrirla una vez con internet. Desde ahí funciona sin conexión.
4. Pasar el `plan-*.json` exportado por la PC al celular (Drive, WhatsApp, cable) y cargarlo con **"Cargar plan"**.
