# CircuitNexus ⚡

![Compilar APK](https://github.com/TU_USUARIO/CircuitNexus/actions/workflows/build.yml/badge.svg)

**Simulador de electrónica para principiantes: cálido, minimalista y sin tecnicismos.**

> *"Cero tecnicismos, máxima claridad. Nunca debes sentirte en una cabina de avión."*

---

## 🎯 La idea

CircuitNexus nace de una pregunta: **¿por qué aprender electrónica tiene que dar miedo?**

Aquí no hay "netlist", "GND", "VCC" ni "nodos". Hay **Batería**, **Negativo/Tierra**,
**Perilla Reguladora** y **Revisar mi Circuito**. El usuario arma circuitos de verdad,
los ve encenderse de verdad, y entiende el porqué en palabras de todos los días.

---

## ✨ Qué hace (v1.0)

### 🏠 Mis Circuitos
- Al abrir por primera vez ya hay **3 circuitos de ejemplo** listos para explorar
- Tarjetas carbón mate con nombre, fecha y acciones (abrir, copiar, borrar, favorito)
- Buscador y filtro de favoritos
- Botón principal verde esmeralda: **+ Empezar Nuevo Circuito**

### 🛠️ Mesa de trabajo 2D
- Lienzo OLED negro con cuadrícula sutil, pan y zoom suaves
- Piezas con puntos de conexión: tocas un punto y luego otro y **se crea el cable**
- Arrastra piezas; se alinean a la cuadrícula
- Cables curvos que **cambian de color** cuando pasa energía
- **Partículas de energía** recorriendo los cables; se frenan en resistencias y brillan en LEDs
- **Panel deslizable de piezas** (bottom sheet) con esquinas de 30 dp, vidrio carbón,
  píldora de arrastre, piezas en cápsulas y **ajustes rápidos inline** (resistencia,
  color del LED, voltaje, pulso del 555, capacitor). Toca o saca una pieza para
  colocarla al instante en la mesa, con vibración sutil
- Piezas esenciales con nombres claros ("Cerebro de Pulsos" para el 555, "Perilla Reguladora"…)
- **Plantillas** de circuitos famosos desde el mismo panel
- Panel de propiedades al tocar una pieza (voltaje, resistencia, frecuencia, texto de pantalla…)
- **Deshacer / Rehacer** de 50 acciones
- **Girar piezas** 90° con el botón ↻ (los pines y los cables giran con ella)
- **Mantén pulsado un cable** para borrarlo (con opción de deshacer)
- **Autoguardado** con indicador "Guardado ✓" y guardado al salir
- **Zoom con botones** (+ / − / ⌖ reencuadrar) además del gesto de pellizcar
- Navegación que recuerda la pestaña donde estabas

### 📈 Osciloscopio
- 2–4 canales de colores
- Base de tiempo ajustable (10 µs → 5 s)
- Escala vertical ajustable
- **Disparo** por flanco con nivel
- **Persistencia de fósforo** (estela analógica)
- Modo **XY** (figuras de Lissajous)
- **Cursores** de tiempo y voltaje con lectura ΔT / ΔV / frecuencia
- Medidas automáticas: Vpp, Vrms, Vmedio, frecuencia

### 📐 Multímetro
- Voltaje continuo y alterno
- Corriente (mA/A)
- Resistencia (Ω) y **continuidad** con aviso sonoro visual
- Diodo y frecuencia
- **HOLD**, **relativo (REL)**, **MIN/MAX**
- Barra analógica + **gráfico de historial**
- Se abre tocando cualquier cable

### 🔍 Revisar mi Circuito (DRC)
Revisión automática con lenguaje cotidiano:
- Pines sueltos y piezas sin conectar
- Cortocircuitos positivo/negativo
- LEDs sin resistencia
- Chips sin alimentación
- Polaridad de capacitores
- Puntaje 0–100 y sugerencia para cada problema

### 📚 Biblioteca de piezas
Más de 50 piezas, con búsqueda y hoja de datos sencilla:
- **Energía**: batería, fuente de corriente, tierra
- **Pasivas**: resistencia, **perilla reguladora**, capacitor, bobina, transformador, sensor de calor, sensor de luz, protector de picos
- **Semiconductores**: LED, diodo, Zener, Schottky, transistores NPN/PNP, interruptores de energía N/P, JFET, IGBT
- **Control**: interruptor, relé
- **Chips**: 555, amplificador operacional, comparador, reguladores, compuerta lógica, contador, desplazador
- **Salidas**: motor, servomotor, motor paso a paso, zumbador, electroimán
- **Sensores**: temperatura, distancia, movimiento
- **Pantallas**: números de **4, 8 y 10 dígitos**, texto LCD, OLED, tira de luces de colores
- **Conectores**: pines, bornera, audio, USB

### 📤 Compartir / Guardar
- JSON (copia completa, para reimportar)
- Netlist SPICE (.cir) para otros simuladores
- Esquemático KiCad
- SVG vectorial del circuito
- CSV con las medidas
- Markdown "receta" del circuito

### 🎓 Tutorial interactivo
"Primeros Pasos" (10 pasos) y "Parpadeo con 555", con progreso y validación automática.

### 🤖 Asistente Inteligente (opcional)
Con tu propia clave de **Groq**, **OpenRouter** o **NVIDIA** (gratis):
explica qué hace tu circuito y qué le falta, en palabras sencillas.

### ⚡ Rendimiento
- Matriz del motor reconstruida solo cuando cambia el circuito (hash barato, sin crear textos)
- Pasos intermedios de simulación sin calcular estado ni historial
- UI a ~30 fps estables (simulación interna a 60), partículas suaves y batería cuidada
- Piezas virtualizadas y dibujo por capas (rejilla, partículas, cables, piezas)

### 📳 Detalles que se sienten
- Vibración distinta al conectar, al fallar y al completar
- 60 fps con virtualización de dibujo
- Todo funciona **sin internet** (la IA es opcional)

---

## 🧠 Motor de simulación

- **Análisis Nodal Modificado (MNA)** en Kotlin puro
- Modelos compañeros de **Euler hacia atrás** (estables)
- Memoria de capacitores y bobinas
- Diodos, LEDs y Zener por tramos
- 555 como generador de onda cuadrada
- Amplificador operacional como fuente controlada
- Punto de operación en continua + simulación transitoria
- Resolución con eliminación gaussiana y pivoteo parcial
- Corre en un hilo de fondo y alimenta osciloscopio y multímetro a ~60 fps

---

## 🎨 Identidad visual

| Uso | Color |
|---|---|
| Fondo | Negro OLED `#000000` |
| Tarjetas | Carbón mate `#121417` |
| Borde | `#1E2228` |
| Crear / correcto | **Esmeralda** `#00E676` |
| Consejo / aviso | **Ámbar** `#FF9100` |
| Medición / info | **Cian** `#00B0FF` |
| Peligro | **Coral** `#FF5252` |
| Texto suave | Gris `#9E9E9E` |

Tarjetas de 20 dp de radio, botones y navegación en píldora de 50 dp.

---

## 🏗️ Estructura

```
app/src/main/java/com/circuitnexus/
├── model/        Circuito, piezas, propiedades, cables
├── data/         Room (DAO, base de datos), repositorio, DataStore
├── engine/       Motor DC, motor transitorio, ayuda offline, IA, DRC
├── export/       JSON, SPICE, KiCad, SVG, CSV, Markdown
├── command/      Deshacer/Rehacer (Command Pattern)
├── particles/    Partículas de energía y brillo
├── templates/    Plantillas de circuitos famosos
├── viewmodel/    Home, Workbench, Learn, Parts, Settings, Tutorial
├── ui/
│   ├── home/ learn/ parts/ settings/
│   ├── workbench/    Lienzo + instrumentos
│   ├── oscilloscope/ Osciloscopio
│   ├── multimeter/   Multímetro
│   ├── library/      Biblioteca de piezas
│   ├── tutorial/     Tutorial
│   ├── theme/        Paleta, tipografía, formas
│   └── common/       Componentes compartidos
├── navigation/   NavHost + barra flotante
├── di/           Inyección manual
├── util/         Vibración
├── CircuitNexusApplication.kt
└── MainActivity.kt
```

---

## 🚀 Cómo compilar (incluso sin PC)

Requisitos en local: Android Studio Koala+ (AGP 8.2.2), JDK 17, SDK 34, mínimo Android 8.0 (API 26).

### Opción A — Sin PC: GitHub compila el APK por ti

1. Sube el proyecto a GitHub (`main`).
2. Entra en la pestaña **Actions → Compilar APK**.
3. Al terminar, abre el job y descarga el artefacto **CircuitNexus-debug**.
4. Dentro está `app-debug.apk`: pásalo al teléfono e instálalo (permitiendo "instalar desde esta fuente").

También puedes crear una versión con nombre:
```bash
git tag v1.0.0
git push origin v1.0.0
```
Eso dispara **Publicar versión** y deja el APK adjunto en *Releases*.

### Opción B — Con PC
```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```

> El proyecto ya incluye `gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`.

---

## 🗺️ Siguiente fase

- Estabilidad y rendimiento en dispositivos reales
- Más modelos de chips (74HC, LCD real, servos con PWM)
- Vista de protoboard / 3D reutilizando el mismo motor
- Retos y laboratorio guiado (fase educativa)
- Compartir circuitos con enlace

---

Hecho con cariño para gente curiosa. 💚
