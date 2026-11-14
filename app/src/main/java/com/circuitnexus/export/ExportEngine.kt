package com.circuitnexus.export

import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Exportación e importación de circuitos.
 *
 * Formatos:
 *  - JSON propio (backup y compartir)
 *  - Netlist SPICE (.cir) para abrir en otros simuladores
 *  - Esquemático KiCad (.kicad_sch) para editar
 *  - Hoja de cálculo CSV con las mediciones
 *  - SVG vectorial del circuito
 *  - Markdown "receta" para documentar
 */
object ExportEngine {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // ===== JSON nativo =====

    @Serializable
    data class CircuitFile(
        val format: String = "circuitnexus",
        val version: Int = 1,
        val circuit: Circuit,
        val components: List<CircuitComponent>,
        val wires: List<CircuitWire>
    )

    fun toJson(circuit: Circuit, components: List<CircuitComponent>, wires: List<CircuitWire>): String {
        return json.encodeToString(CircuitFile(circuit = circuit, components = components, wires = wires))
    }

    fun fromJson(text: String): CircuitFile? = try {
        json.decodeFromString<CircuitFile>(text)
    } catch (e: Exception) {
        null
    }

    // ===== Netlist SPICE =====

    fun toSpice(circuit: Circuit, components: List<CircuitComponent>, wires: List<CircuitWire>): String {
        val sb = StringBuilder()
        val nodeMap = buildNodeMap(components, wires)

        sb.appendLine("* ${circuit.name}")
        sb.appendLine("* Generado por CircuitNexus")
        sb.appendLine()

        for (comp in components) {
            val n1 = nodeMap[comp.id to 0] ?: 0
            val n2 = nodeMap[comp.id to 1] ?: 0
            when (comp.type) {
                ComponentType.RESISTOR, ComponentType.POTENTIOMETER ->
                    sb.appendLine("R${comp.id} $n1 $n2 ${comp.properties.resistanceOhms ?: 330f}")
                ComponentType.CAPACITOR ->
                    sb.appendLine("C${comp.id} $n1 $n2 ${comp.properties.capacitanceFarads ?: 100e-6f}")
                ComponentType.INDUCTOR ->
                    sb.appendLine("L${comp.id} $n1 $n2 ${comp.properties.inductanceHenries ?: 1e-3f}")
                ComponentType.BATTERY ->
                    sb.appendLine("V${comp.id} $n1 $n2 DC ${comp.properties.voltageVolts ?: 9f}")
                ComponentType.CURRENT_SOURCE ->
                    sb.appendLine("I${comp.id} $n1 $n2 DC ${comp.properties.currentAmps ?: 0.02f}")
                ComponentType.DIODE ->
                    sb.appendLine("D${comp.id} $n1 $n2 DMOD")
                ComponentType.LED ->
                    sb.appendLine("D${comp.id} $n1 $n2 LEDMOD")
                ComponentType.ZENER_DIODE ->
                    sb.appendLine("D${comp.id} $n1 $n2 ZENERMOD")
                ComponentType.SWITCH ->
                    sb.appendLine("S${comp.id} $n1 $n2 1 2 SWMOD")
                else -> sb.appendLine("* ${comp.type.displayName} no exportado")
            }
        }

        sb.appendLine()
        sb.appendLine(".model DMOD D(IS=1e-14 N=1)")
        sb.appendLine(".model LEDMOD D(IS=1e-12 N=2 RS=10)")
        sb.appendLine(".model ZENERMOD D(IS=1e-14 N=1 BV=${components.find { it.type == ComponentType.ZENER_DIODE }?.properties?.breakdownVoltage ?: 5.1f})")
        sb.appendLine(".model SWMOD SW(RON=0.01 ROFF=1e9 VT=2.5 VH=0.5)")
        sb.appendLine()
        sb.appendLine(".tran 1u 100m")
        sb.appendLine(".end")
        return sb.toString()
    }

    // ===== Esquemático KiCad =====

    fun toKiCad(circuit: Circuit, components: List<CircuitComponent>, wires: List<CircuitWire>): String {
        val sb = StringBuilder()
        sb.appendLine("(kicad_sch (version 20231120) (generator circuitnexus)")
        sb.appendLine("  (uuid \"00000000-0000-0000-0000-000000000001\")")
        sb.appendLine("  (paper \"A4\")")
        sb.appendLine("  (title_block (title \"${circuit.name}\") (comment 1 \"Generado por CircuitNexus\"))")

        for (comp in components) {
            val ref = "${comp.type.name.first()}${comp.id}"
            sb.appendLine("  (symbol (lib_id \"CircuitNexus:${comp.type.name}\")")
            sb.appendLine("    (at ${comp.position.x} ${comp.position.y} ${comp.position.rotation})")
            sb.appendLine("    (unit 1)")
            sb.appendLine("    (property \"Reference\" \"$ref\" (at ${comp.position.x} ${comp.position.y + 5} 0))")
            sb.appendLine("    (property \"Value\" \"${comp.type.displayName}\" (at ${comp.position.x} ${comp.position.y + 8} 0))")
            sb.appendLine("  )")
        }

        sb.appendLine("  (sheet_instances (path \"/\" (page \"1\")))")
        sb.appendLine(")")
        return sb.toString()
    }

    // ===== CSV de mediciones =====

    fun toCsv(
        components: List<CircuitComponent>,
        state: SimulationState
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Pieza,Voltaje (V),Corriente (mA),Potencia (mW)")
        for (comp in components) {
            val current = state.componentCurrents[comp.id] ?: 0f
            val voltage = state.nodeVoltages.values.firstOrNull() ?: 0f
            val power = voltage * current
            sb.appendLine(
                "\"${comp.type.displayName}\",${"%.3f".format(voltage)},${"%.3f".format(current * 1000)},${"%.3f".format(power * 1000)}"
            )
        }
        return sb.toString()
    }

    // ===== SVG del circuito =====

    fun toSvg(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        width: Int = 1200,
        height: Int = 800
    ): String {
        val sb = StringBuilder()
        sb.appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">")
        sb.appendLine("  <rect width=\"100%\" height=\"100%\" fill=\"#000000\"/>")
        sb.appendLine("  <g stroke=\"#1E2228\" stroke-width=\"0.5\">")
        var gx = 0
        while (gx < width) { sb.appendLine("    <line x1=\"$gx\" y1=\"0\" x2=\"$gx\" y2=\"$height\"/>"); gx += 20 }
        var gy = 0
        while (gy < height) { sb.appendLine("    <line x1=\"0\" y1=\"$gy\" x2=\"$width\" y2=\"$gy\"/>"); gy += 20 }
        sb.appendLine("  </g>")

        // Cables
        for (wire in wires) {
            val from = components.find { it.id == wire.fromComponentId } ?: continue
            val to = components.find { it.id == wire.toComponentId } ?: continue
            sb.appendLine("  <line x1=\"${from.position.x}\" y1=\"${from.position.y}\" x2=\"${to.position.x}\" y2=\"${to.position.y}\" stroke=\"#00E676\" stroke-width=\"3\"/>")
        }

        // Piezas
        for (comp in components) {
            val x = comp.position.x
            val y = comp.position.y
            sb.appendLine("  <rect x=\"${x - 40}\" y=\"${y - 40}\" width=\"80\" height=\"80\" rx=\"20\" fill=\"#121417\" stroke=\"#1E2228\"/>")
            sb.appendLine("  <text x=\"${x}\" y=\"${y}\" fill=\"#FFFFFF\" font-size=\"16\" text-anchor=\"middle\" dominant-baseline=\"middle\">${escapeXml(comp.type.displayName)}</text>")
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    // ===== Markdown "receta" =====

    fun toMarkdown(
        circuit: Circuit,
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        state: SimulationState?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# ${circuit.name}")
        sb.appendLine()
        if (circuit.description.isNotBlank()) {
            sb.appendLine(circuit.description)
            sb.appendLine()
        }
        sb.appendLine("**Hecho con CircuitNexus** 🧡")
        sb.appendLine()
        sb.appendLine("## Piezas")
        sb.appendLine()
        for (comp in components) {
            sb.appendLine("- ${comp.type.emoji} **${comp.type.displayName}** — ${comp.type.description}")
        }
        sb.appendLine()
        sb.appendLine("## Conexiones")
        sb.appendLine()
        for (wire in wires) {
            val from = components.find { it.id == wire.fromComponentId }
            val to = components.find { it.id == wire.toComponentId }
            if (from != null && to != null) {
                val fromPin = CircuitWire.pinName(from.type, wire.fromPinIndex)
                val toPin = CircuitWire.pinName(to.type, wire.toPinIndex)
                sb.appendLine("- ${from.type.displayName} (${fromPin}) → ${to.type.displayName} (${toPin})")
            }
        }
        if (state != null) {
            sb.appendLine()
            sb.appendLine("## Medidas")
            sb.appendLine()
            sb.appendLine("| Pieza | Corriente |")
            sb.appendLine("|---|---|")
            for (comp in components) {
                val current = state.componentCurrents[comp.id] ?: 0f
                sb.appendLine("| ${comp.type.displayName} | ${"%.1f".format(current * 1000)} mA |")
            }
        }
        return sb.toString()
    }

    // ===== Utilidades =====

    private fun buildNodeMap(components: List<CircuitComponent>, wires: List<CircuitWire>): Map<Pair<Long, Int>, Int> {
        val map = HashMap<Pair<Long, Int>, Int>()
        var next = 0
        // Tierra = 0
        for (c in components) {
            if (c.type == ComponentType.GROUND) {
                map[c.id to 0] = 0
            }
        }
        for (c in components) {
            for (pin in 0 until c.type.pinCount) {
                val key = c.id to pin
                if (map.containsKey(key)) continue
                map[key] = ++next
            }
        }
        // simplificación: cada pin distinto; los cables no se colapsan en este export
        for (w in wires) {
            val node = map[w.fromComponentId to w.fromPinIndex] ?: continue
            map[w.toComponentId to w.toPinIndex] = node
        }
        return map
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
