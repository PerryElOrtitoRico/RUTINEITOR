# Registro de sesiones — paquete para un LLM local

Este paquete especifica la funcionalidad "registro de sesiones" para que la programe **un LLM local**, y trae tests ocultos para medir qué tan bien lo hizo. **No contiene la implementación.**

```
registro-sesiones/
├── README.md               ← este archivo
├── 00-especificacion.md    ← el contrato: se le da al LLM en TODAS las tareas
├── ejemplos/
│   ├── plan-ejemplo.json       (lo que exporta la PC)
│   └── registro-ejemplo.json   (lo que exporta el celular)
├── tareas/                 ← una por conversación con el LLM, en orden
│   ├── T01-escalas.md            Java · fácil
│   ├── T02-notacion.md           Java · media (parser)
│   ├── T03-armar-plan.md         Java · difícil (usa el código existente)
│   ├── T04-json-plan.md          Java · fácil
│   ├── T05-json-registro.md      Java · media (validación, @SerializedName)
│   ├── T06-guardar-registro.md   Java · fácil (SQLite)
│   ├── T07-analisis.md           Java · difícil (cálculo)
│   ├── T08-interfaz-pc.md        Java Swing · media · prueba manual
│   ├── T09-web-logica.md         JavaScript · media
│   ├── T10-web-pantalla.md       HTML/JS · media · prueba manual
│   ├── T11-web-exportar-offline.md  JS/PWA · media · prueba manual
│   └── T12-publicar.md           pasos de GitHub Pages (sin código)
└── evaluacion/             ← NO se le muestra al LLM
    ├── java/…/T0xOcultoTest.java   41 tests ocultos de T01–T07
    └── web/pruebas.html             15 pruebas ocultas de T09
```

Dependencias entre tareas: T01 y T02 son independientes. T03 necesita T02; T04 necesita T03; T05 necesita T01 y T04; T06 necesita T05; T07 necesita T02, T03 y T05; T08 necesita todas las Java. T09 no depende de nada; T10 necesita T09; T11 necesita T10; T12 necesita T11.

## 1. Preparar el LLM local (una sola vez)

Tu PC: RTX 2060 de **6 GB**, 15 GB de RAM. Entran bien modelos de unos 7B cuantizados.

1. **Ollama** (corre el modelo). Instalación oficial en https://ollama.com/download/linux:
   ```bash
   curl -fsSL https://ollama.com/install.sh | sh
   ```
2. **Modelo** recomendado para código en 6 GB (~4,7 GB de descarga):
   ```bash
   ollama pull qwen2.5-coder:7b
   ```
   Si va muy lento o no entra, `qwen2.5-coder:3b`. Para comparar, se puede repetir todo con otro modelo.
3. **Aider** (asistente de terminal que edita los archivos del repo y puede correr los tests):
   ```bash
   python3 -m pip install --user aider-chat
   ```
4. **Contexto:** la tarea más el contrato suman unos 5–7 mil tokens. Ollama usa por defecto un contexto chico, así que subilo a 16 384 tokens. Por ejemplo, en `~/.aider.model.settings.yml`:
   ```yaml
   - name: ollama_chat/qwen2.5-coder:7b
     extra_params:
       num_ctx: 16384
   ```

## 2. Esconder los tests ocultos

Aider arma un mapa de todo el repo, así que **antes de empezar**:
- creá `.aiderignore` en la raíz del repo con la línea `docs/registro-sesiones/evaluacion/`, **o**
- mové la carpeta `evaluacion/` afuera del repo hasta la hora de evaluar.

## 3. Correr cada tarea

```bash
cd ~/Escritorio/Hobbies/programacion/Proyectos/ProyectoRutina
git switch -c llm/registro-sesiones          # todo el experimento en una rama aparte
aider --model ollama_chat/qwen2.5-coder:7b
```
Por cada tarea, **en una conversación nueva** (`/clear`):
1. `/read docs/registro-sesiones/00-especificacion.md docs/registro-sesiones/tareas/T0N-….md`
2. `/add` los archivos que la tarea dice crear o modificar.
3. Escribile: *"Hacé la tarea T0N tal como está escrita."*
4. Cuando termine: `/run mvn -q test`. Si falla, pegale el error y dejalo corregir, contando cuántas veces lo hiciste.
5. Anotá los resultados en la tabla de la sección 5 y hacé commit: `/commit`, o `git commit` afuera.

Si preferís chat sin Aider (`ollama run qwen2.5-coder:7b`), pegá el contrato y la tarea, y copiá a mano los archivos que te devuelva.

## 4. Evaluar

**Tests ocultos de Java (T01–T07)**, con el código del LLM ya commiteado:
```bash
cp -r docs/registro-sesiones/evaluacion/java/com src/test/java/
mvn test -Dtest='T0*OcultoTest' -Dsurefire.failIfNoSpecifiedTests=false
```
- El resultado por tarea está en `target/surefire-reports/com.rutina.*.T0N*OcultoTest.txt`.
- Al final, borrá los tests copiados, así no quedan en la rama del LLM.
- Los tests ocultos usan las firmas **exactas** de las tareas: que no compile también es un resultado (el LLM no siguió la interfaz).

**Pruebas ocultas web (T09):**
```bash
python3 -m http.server 8000
```
Después, en Chrome: `http://localhost:8000/docs/registro-sesiones/evaluacion/web/pruebas.html`. Tiene que decir "Todas las pruebas pasaron (15)".

**Pruebas manuales:** la sección "Prueba manual" o "Cómo verificar" de T08, T10 y T11.

Verificado al armar el paquete:
- los tests ocultos compilan contra las firmas de las tareas;
- con clases vacías fallan 40 de 41 (el que pasa comprueba que no se rompieron las tablas que ya existían);
- la página web marca cada prueba por separado.

## 5. Planilla de resultados

| Tarea | ¿Compila? | Tests propios | Ocultos | Correcciones pedidas | Tiempo | Notas |
|---|---|---|---|---|---|---|
| T01 | | | /3 | | | |
| T02 | | | /10 | | | |
| T03 | | | /7 | | | |
| T04 | | | /5 | | | |
| T05 | | | /6 | | | |
| T06 | | | /4 | | | |
| T07 | | | /6 | | | |
| T08 | | — | manual | | | |
| T09 | — | — | /15 | | | |
| T10 | — | — | manual | | | |
| T11 | — | — | manual | | | |

Qué mirar además de los números:
- si respetó las firmas exactas;
- si tocó archivos que no debía;
- si agregó dependencias;
- si inventó APIs del código existente;
- si los tests propios prueban algo de verdad.

## 6. Después del experimento

- Si el resultado sirve: merge de la rama `llm/registro-sesiones` a `main`, y publicar con T12.
- Si no: la rama se descarta, y el paquete sigue sirviendo para otro modelo o para Claude.
