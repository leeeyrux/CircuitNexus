package com.circuitnexus.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta HyperNexus exacta según especificación
object CircuitColors {

    // Fondos
    val Background = Color(0xFF000000)           // Negro puro OLED
    val Surface = Color(0xFF121417)              // Gris carbón mate para tarjetas
    val SurfaceVariant = Color(0xFF1A1D22)       // Variante ligeramente más clara
    val SurfaceContainer = Color(0xFF1E2228)     // Contenedores internos
    val Outline = Color(0xFF1E2228)              // Borde sutil 1dp

    // Acentos funcionales
    val Emerald = Color(0xFF00E676)              // Verde Esmeralda Neón - Encender/Crear/Óptimo
    val EmeraldContainer = Color(0xFF003D1A)     // Contenedor verde muy oscuro
    val EmeraldOn = Color(0xFF000000)            // Texto sobre verde (negro)

    val Amber = Color(0xFFFF9100)                // Ámbar cálido - Consejos/Advertencias suaves
    val AmberContainer = Color(0xFF3D2A00)       // Contenedor ámbar oscuro
    val AmberOn = Color(0xFF000000)              // Texto sobre ámbar (negro)

    val Cyan = Color(0xFF00B0FF)                 // Azul Cian - Mediciones
    val CyanContainer = Color(0xFF002B3D)        // Contenedor cian oscuro
    val CyanOn = Color(0xFF000000)               // Texto sobre cian (negro)

    val Coral = Color(0xFFFF5252)                // Rojo Coral - Peligros/Cortocircuitos
    val CoralContainer = Color(0xFF3D1414)       // Contenedor coral oscuro
    val CoralOn = Color(0xFFFFFFFF)              // Texto sobre coral (blanco)

    // Texto
    val OnBackground = Color(0xFFFFFFFF)         // Blanco puro para títulos
    val OnSurface = Color(0xFFFFFFFF)            // Blanco sobre superficies
    val OnSurfaceVariant = Color(0xFF9E9E9E)     // Gris suave #9E9E9E para subtítulos
    val OnSurfaceDisabled = Color(0xFF666666)    // Texto deshabilitado

    // Estados de conexión
    val WireConnected = Emerald
    val WireConnecting = Amber
    val WireError = Coral
    val WireIdle = Color(0xFF44484F)

    // Grid de mesa de trabajo
    val GridMajor = Color(0xFF1A1D22)
    val GridMinor = Color(0xFF0A0C0F)

    // Componentes específicos
    val ComponentBody = Color(0xFF1E2228)
    val ComponentPin = Color(0xFF00E676)
    val ComponentPinHover = Color(0xFF00FF88)
    val ComponentLabel = Color(0xFFB0B0B0)
}
