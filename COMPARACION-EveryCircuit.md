# CircuitNexus vs. EveryCircuit
### Comparación honesta tras la versión 1.0

EveryCircuit es el referente móvil del sector: años de pulido, motor SPICE-like,
osciloscopio maduro y una comunidad grande. Esta comparación es para saber **dónde
estamos fuertes, dónde nos falta y qué atacar después**.

---

## 📊 Tabla comparativa

| Aspecto | EveryCircuit | **CircuitNexus 1.0** | Ventaja |
|---|---|---|---|
| **Público objetivo** | Estudiantes e ingenieros | **Principiantes absolutos y curiosos** | 🔥 Nosotros |
| **Lenguaje de la interfaz** | Términos técnicos (GND, VCC, netlist) | **Todo en palabras cotidianas** | 🔥 Nosotros |
| **Estética** | Esquemático clásico | **HyperNexus OLED + partículas + brillo** | 🔥 Nosotros |
| **Curva de aprendizaje** | Media/alta | **Tutorial de 10 pasos + ayuda contextual** | 🔥 Nosotros |
| **Multímetro** | Sonda manual, ventana aparte | **Flotante al tocar el cable**, HOLD/REL/MIN-MAX, historial | 🔥 Nosotros |
| **Osciloscopio** | Muy completo (varios canales, FFT, triggers) | 2–4 canales, disparo, fósforo, XY, cursores, medidas auto | ⚖️ Empate parcial |
| **Deshacer/Rehacer** | Limitado | **50 acciones con Command Pattern** | 🔥 Nosotros |
| **Plantillas** | Básicas | **9 circuitos famosos con objetivos de aprendizaje** | 🔥 Nosotros |
| **Ayuda instantánea sin internet** | ❌ | **✅ Motor de reglas en español** | 🔥 Nosotros |
| **Asistente IA opcional** | ❌ | **✅ Groq / OpenRouter / NVIDIA con tu clave** | 🔥 Nosotros |
| **Revisión automática (DRC)** | Parcial | **✅ "Revisar mi Circuito" con puntaje y consejos** | 🔥 Nosotros |
| **Exportar** | Imagen, SPICE, datos | JSON, SPICE, KiCad, SVG, CSV, Markdown | ⚖️ Empate |
| **Vibración/hápticos** | ❌ | **✅ En cada interacción** | 🔥 Nosotros |
| **50+ piezas** | ~80–100 | **50+ con nombres amigables** | ⚖️ Ellos (más variedad) |
| **Modelos de semiconductores** | SPICE completo (BJT, MOS, diodos) | Modelos simplificados por tramos | ⚖️ Ellos |
| **Simulación transitoria** | Madura y precisa | Euler hacia atrás, precisa para docencia | ⚖️ Ellos (profundidad) |
| **Comunidad / compartir** | Grande | No hay nube todavía | ⚖️ Ellos |
| **Precio** | Pago | **Gratis, código abierto** | 🔥 Nosotros |
| **Funciona sin internet** | Parcial | **✅ Todo salvo la IA opcional** | 🔥 Nosotros |

---

## 🏆 Dónde ganamos claramente

1. **Traducción total a lenguaje humano.** Ninguna app del sector habla así.
2. **Onboarding.** Un principiante arma su primer circuito en menos de 2 minutos.
3. **Instrumentos integrados en el lienzo.** Tocar un cable y ver el multímetro es
   inmediato; no hay que abrir ventanas ni configurar sondas.
4. **Ayuda offline.** El motor de reglas explica el error antes de que el usuario
   se frustre, sin depender de red.
5. **Estética.** OLED, esmeralda, partículas: se siente moderno y cálido, no a CAD.
6. **Revisión con puntaje.** Convierte el error en juego: "82/100, arregla esto".
7. **Plantillas con intención didáctica**, no solo ejemplos sueltos.

## ⚠️ Dónde nos falta (honestidad)

1. **Profundidad de modelos.** EveryCircuit usa SPICE con modelos reales; nosotros
   usamos tramos lineales y modelos compañeros. Para docencia es suficiente, para
   ingeniería todavía no.
2. **Variedad de chips.** Tenemos 555, op-amp, compuerta, contador y desplazador;
   falta una familia 74HC completa, LCD real, servos con PWM, etc.
3. **Osciloscopio:** nos falta FFT, disparo por ancho de pulso y decodificación
   de protocolos.
4. **Protoboard / 3D**: no existe todavía (está en el roadmap reutilizando el motor).
5. **Nube y comunidad**: no hay compartir por enlace ni galería pública.
6. **Precisión numérica** en circuitos muy stiff (conmutación rápida) puede requerir
   pasos más pequeños.

---

## 🎯 Lo que haría para superarles de verdad

| Prioridad | Acción |
|---|---|
| 1 | **Estabilidad + rendimiento** en móviles reales (lo siguiente acordado) |
| 2 | Familia 74HC + LCD 16x2 + servo con PWM |
| 3 | FFT y disparo avanzado en el osciloscopio |
| 4 | Vista de protoboard reutilizando el mismo motor |
| 5 | Retos y laboratorio guiado (fase educativa) |
| 6 | Compartir circuitos por enlace (nube opcional) |

---

## Conclusión

**EveryCircuit sigue siendo más profundo técnicamente.**
**CircuitNexus es más humano y mucho más fácil de empezar.**

Para el principiante que abandona EveryCircuit en la primera pantalla, CircuitNexus
es la puerta de entrada que faltaba. Si en las próximas versiones igualamos modelos
y añadimos protoboard, la combinación *potencia + cercanía* es difícil de igualar.

*Comparación cualitativa basada en la experiencia de usuario documentada de cada app.
No se han copiado datos privados ni se ha usado su código.*
