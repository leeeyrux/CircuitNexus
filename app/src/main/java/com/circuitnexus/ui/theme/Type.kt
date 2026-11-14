package com.circuitnexus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object CircuitTypography {

    // Familia tipográfica limpia y legible
    val FontFamily = FontFamily.Default

    // Títulos principales - Limpios, blancos, mucho aire
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = -0.5.sp,
        textAlign = TextAlign.Center
    )

    val DisplayMedium = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = -0.3.sp,
        textAlign = TextAlign.Center
    )

    val DisplaySmall = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = -0.2.sp
    )

    // Headlines para secciones
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )

    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )

    val HeadlineSmall = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    // Títulos de tarjetas y elementos
    val TitleLarge = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val TitleMedium = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val TitleSmall = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    // Cuerpo de texto - legible, cálido
    val BodyLarge = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val BodyMedium = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )

    val BodySmall = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )

    // Labels para botones y chips
    val LabelLarge = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val LabelMedium = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    )

    val LabelSmall = TextStyle(
        fontFamily = FontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
}

val CircuitTypographyInstance = Typography(
    displayLarge = CircuitTypography.DisplayLarge,
    displayMedium = CircuitTypography.DisplayMedium,
    displaySmall = CircuitTypography.DisplaySmall,
    headlineLarge = CircuitTypography.HeadlineLarge,
    headlineMedium = CircuitTypography.HeadlineMedium,
    headlineSmall = CircuitTypography.HeadlineSmall,
    titleLarge = CircuitTypography.TitleLarge,
    titleMedium = CircuitTypography.TitleMedium,
    titleSmall = CircuitTypography.TitleSmall,
    bodyLarge = CircuitTypography.BodyLarge,
    bodyMedium = CircuitTypography.BodyMedium,
    bodySmall = CircuitTypography.BodySmall,
    labelLarge = CircuitTypography.LabelLarge,
    labelMedium = CircuitTypography.LabelMedium,
    labelSmall = CircuitTypography.LabelSmall
)