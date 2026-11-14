package com.circuitnexus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object CircuitShapes {

    // Esquinas redondeadas amplias (20.dp) para tarjetas
    val CardShape = RoundedCornerShape(20.dp)

    // Esquinas totalmente curvas (50.dp) para barra de navegación y botones píldora
    val PillShape = RoundedCornerShape(50.dp)

    // Esquinas medias para contenedores internos
    val MediumShape = RoundedCornerShape(12.dp)

    // Esquinas pequeñas para chips y elementos menores
    val SmallShape = RoundedCornerShape(8.dp)

    // Esquinas extra grandes para modales y drawers
    val ExtraLargeShape = RoundedCornerShape(28.dp)

    // Círculo perfecto para indicadores de estado
    val CircleShape = RoundedCornerShape(50.dp)
}

val CircuitShapesInstance = Shapes(
    extraSmall = CircuitShapes.SmallShape,
    small = CircuitShapes.MediumShape,
    medium = CircuitShapes.CardShape,
    large = CircuitShapes.ExtraLargeShape,
    extraLarge = CircuitShapes.ExtraLargeShape
)