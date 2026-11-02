package com.example.wordstore.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** WordMint's own outline set: 24 units, 1.6 stroke, round caps and joins. */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    /** A glass tube with a liquid level: the app's own symbol for "today". */
    val Today = icon("today", "M9 3.5h6", "M10 3.5v14.5a2 2 0 0 0 4 0V3.5", "M10 12c1.3-.8 2.7.8 4 0")
    val Dictionary = icon("dictionary", "M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13z", "M15.3 15.3 20 20", "M8 10.5h5")
    val Words = icon("words", "M5 4.5h11a3 3 0 0 1 3 3V20H8a3 3 0 0 1-3-3V4.5z", "M5 17a3 3 0 0 1 3-3h11", "M9 8.5h6")
    val Progress = icon("progress", "M4 20h16", "M7 20v-6", "M12 20V5", "M17 20v-9")
    val Settings = icon(
        "settings",
        "M4 7h9", "M18 7h2", "M4 17h2", "M11 17h9",
        "M15.5 9.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
        "M8.5 19.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z"
    )
    val Search = icon("search", "M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13z", "M15.3 15.3 20 20")
    val Close = icon("close", "M6 6l12 12", "M18 6 6 18")
    val Back = icon("back", "M10 5l-7 7 7 7", "M3 12h18")
    val Forward = icon("forward", "M9 5l7 7-7 7")
    val Plus = icon("plus", "M12 5v14", "M5 12h14")
    val Minus = icon("minus", "M5 12h14")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val Speaker = icon(
        "speaker",
        "M4 9.5v5h3.5L12 18.5v-13L7.5 9.5H4z",
        "M15.5 9a4 4 0 0 1 0 6",
        "M18 6.5a8 8 0 0 1 0 11"
    )
    val SpeakerOff = icon("speaker_off", "M4 9.5v5h3.5L12 18.5v-13L7.5 9.5H4z", "M16 9.5l5 5", "M21 9.5l-5 5")
    val Swap = icon("swap", "M4 8.5h14", "M14.5 5l3.5 3.5-3.5 3.5", "M20 15.5H6", "M9.5 12 6 15.5 9.5 19")
    val Edit = icon("edit", "M4.5 19.5l1-4L16 5a2 2 0 0 1 2.8 0l.2.2a2 2 0 0 1 0 2.8L8.5 18.5l-4 1z", "M14.5 6.5l3 3")
    val Delete = icon("delete", "M4.5 7h15", "M9.5 7V4.5h5V7", "M6.5 7l1 13h9l1-13", "M10 11v5.5", "M14 11v5.5")
    val Bell = icon("bell", "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15l1.5-2z", "M10 21h4")
    val Clock = icon("clock", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z", "M12 7.5V12l3 2")
    val Download = icon("download", "M12 4v11", "M7.5 11 12 15.5l4.5-4.5", "M5 20h14")
    val Sort = icon("sort", "M4 7h16", "M7 12h10", "M10 17h4")
    val Hint = icon("hint", "M9 17.5h6", "M10 21h4", "M12 3a6.5 6.5 0 0 0-3.5 12c.4.3.5.7.5 1.2v1.3h6v-1.3c0-.5.1-.9.5-1.2A6.5 6.5 0 0 0 12 3z")
    val Warning = icon("warning", "M12 4 2.5 20h19L12 4z", "M12 10v4.5", "M12 17.5v.1")
    val Sun = icon(
        "sun",
        "M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z",
        "M12 3v1.5", "M12 19.5V21", "M3 12h1.5", "M19.5 12H21",
        "M5.6 5.6l1 1", "M17.4 17.4l1 1", "M5.6 18.4l1-1", "M17.4 6.6l1-1"
    )
}
