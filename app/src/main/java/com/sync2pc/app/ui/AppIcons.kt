package com.sync2pc.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Eigene Symbole, die im Basis-Iconset (material-icons-core) fehlen */
object AppIcons {
    /** Pause: zwei senkrechte Balken (wie Material "Pause", 24 × 24) */
    val Pause: ImageVector by lazy {
        ImageVector.Builder(name = "Pause", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f); horizontalLineTo(10f); verticalLineTo(5f); horizontalLineTo(6f); close()
                moveTo(14f, 5f); verticalLineTo(19f); horizontalLineTo(18f); verticalLineTo(5f); close()
            }
            .build()
    }

    /** Balkendiagramm (wie Material "BarChart", 24 × 24) – seit 0.9.0 für die Statistik */
    val BarChart: ImageVector by lazy {
        ImageVector.Builder(name = "BarChart", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 9.2f); horizontalLineTo(8f); verticalLineTo(19f); horizontalLineTo(5f); close()
                moveTo(10.6f, 5f); horizontalLineToRelative(2.8f); verticalLineToRelative(14f); horizontalLineToRelative(-2.8f); close()
                moveTo(16.2f, 13f); horizontalLineTo(19f); verticalLineToRelative(6f); horizontalLineToRelative(-2.8f); close()
            }
            .build()
    }

    /** Smartphone (wie Material "Smartphone", 24 × 24) – seit 0.6.1 für "Speicher freigeben" */
    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(name = "Smartphone", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 1.01f); lineTo(7f, 1f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(18f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(10f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(3f)
                curveToRelative(0f, -1.1f, -0.9f, -1.99f, -2f, -1.99f)
                close()
                moveTo(17f, 19f); horizontalLineTo(7f); verticalLineTo(5f); horizontalLineToRelative(10f); verticalLineToRelative(14f); close()
            }
            .build()
    }
}
