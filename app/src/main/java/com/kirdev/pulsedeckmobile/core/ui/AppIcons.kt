package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Line icons drawn for PulseDesk (24×24 viewBox), the same set as the PC app. No icon font. */
object AppIcons {
    private fun icon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (data in paths) {
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    fill = if (filled) SolidColor(Color.White) else null,
                    stroke = if (filled) null else SolidColor(Color.White),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Settings = icon("settings", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6",
        "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1")
    val Back = icon("back", "M14.5 6l-6 6l6 6")
    val Chevron = icon("chevron", "M9.5 6l6 6l-6 6")
    val Monitor = icon("monitor", "M5 4h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M8 20h8M12 16v4")
    val Cpu = icon("cpu", "M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M9.5 9.5h5v5h-5z",
        "M9 2.5V6M15 2.5V6M9 18v3.5M15 18v3.5M2.5 9H6M2.5 15H6M18 9h3.5M18 15h3.5")
    val Gpu = icon("gpu", "M4.5 6h15a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-15a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
        "M9 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6", "M15 10h3M15 14h3")
    val Ram = icon("ram", "M4 7h16a1.5 1.5 0 0 1 1.5 1.5v7A1.5 1.5 0 0 1 20 17H4a1.5 1.5 0 0 1-1.5-1.5v-7A1.5 1.5 0 0 1 4 7z",
        "M6.5 10.5v3M10.5 10.5v3M14.5 10.5v3M18 10.5v3")
    val Disk = icon("disk", "M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M3 14h18", "M16.5 17h1")
    val Network = icon("network", "M2.5 9a14 14 0 0 1 19 0", "M5.5 12.5a9.5 9.5 0 0 1 13 0", "M8.7 16a5 5 0 0 1 6.6 0",
        "M12 19.2a0.8 0.8 0 1 0 0 1.6a0.8 0.8 0 1 0 0-1.6")
    val Music = icon("music", "M9 18V5l11-2v13", "M6.5 15.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5",
        "M17.5 13.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5")
    val Volume = icon("volume", "M4 9.5h3.5L12 5.5v13l-4.5-4H4z", "M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11")
    val Play = icon("play", "M8 5v14l11-7z", filled = true)
    val Pause = icon("pause", "M7 5h3.5v14H7zM13.5 5H17v14h-3.5z", filled = true)
    val Next = icon("next", "M5 5v14l10-7z", "M16.5 5h2v14h-2z", filled = true)
    val Prev = icon("prev", "M19 5v14L9 12z", "M5.5 5h2v14h-2z", filled = true)

    // Navigation and screens of the phone app
    val Home = icon("home", "M4 10.5L12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z")
    val Pulse = icon("pulse", "M2.5 12h4l2.5-6l4 12l2.5-6h6")
    val Control = icon("control", "M12 3l7 3v5.5c0 4.3-3 7.9-7 9.5c-4-1.6-7-5.2-7-9.5V6z", "M12 8.5v4.5")
    val Folder = icon("folder", "M3.5 7a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z")
    val Menu = icon("menu", "M4 7h16M4 12h16M4 17h16")
    val Power = icon("power", "M12 3v8", "M7.2 6.2a7.5 7.5 0 1 0 9.6 0")
    val Restart = icon("restart", "M4.5 12a7.5 7.5 0 1 0 2.2-5.3", "M4.5 4.5v4h4")
    val Moon = icon("moon", "M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z")
    val Lock = icon("lock", "M6.5 11h11a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z", "M8.5 11V8a3.5 3.5 0 0 1 7 0v3")
    val Window = icon("window", "M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z", "M3 9h18")
    val Apps = icon("apps", "M5 5h5v5H5zM14 5h5v5h-5zM5 14h5v5H5zM14 14h5v5h-5z")
    val Bell = icon("bell", "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
    val Devices = icon("devices", "M3 5h14v10H3z", "M7 19h6M10 15v4", "M18 9h3v10h-3z")
    val Brush = icon("brush", "M14.5 4.5l5 5l-8 8l-5-5z", "M6.5 12.5l-2 6l6-2")
    val Search = icon("search", "M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13", "M15.5 15.5L20 20")
    val File = icon("file", "M6 3h8l4 4v14H6z", "M14 3v4h4")
    val Upload = icon("upload", "M12 16V4M7 9l5-5l5 5", "M4.5 16v3.5h15V16")
    val Download = icon("download", "M12 4v12M7 11l5 5l5-5", "M4.5 16v3.5h15V16")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Trash = icon("trash", "M4.5 7h15", "M9 7V4.5h6V7", "M6.5 7l1 13h9l1-13")
    val Edit = icon("edit", "M15 5l4 4l-10 10H5v-4z")
    val Link = icon("link", "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1", "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val Image = icon("image", "M4 5h16v14H4z", "M4 16l5-5l4 4l3-3l4 4", "M15.5 8.5a1 1 0 1 0 0 2a1 1 0 1 0 0-2")
    val Close = icon("close", "M6 6l12 12M18 6L6 18")
    val Refresh = icon("refresh", "M19.5 12a7.5 7.5 0 1 1-2.2-5.3", "M19.5 4.5v4h-4")
    val Logout = icon("logout", "M10 4H5v16h5", "M14 8l4 4l-4 4M18 12H9")
    val Info = icon("info", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17", "M12 11v5.5M12 7.8v0.4")
    val Usb = icon("usb", "M12 3v13", "M9.5 6L12 3l2.5 3", "M7 10v2.5l5 3l5-3V10", "M12 16a2 2 0 1 0 0 4a2 2 0 1 0 0-4")
}
