package com.example.remotecompose.server.components

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier

private const val DEFAULT_DENSITY = 2.625f

private fun dp(value: Int): Float = value * DEFAULT_DENSITY
private fun sp(value: Int): Float = value * DEFAULT_DENSITY
private fun argb(hex: String): Int = hex.removePrefix("#").toLong(16).let { rgb ->
    if (hex.length <= 7) (0xFF000000 or rgb).toInt() else rgb.toInt()
}

/**
 * Remote Compose component generated directly from Kotlin.
 *
 * Equivalent visual structure:
 *
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .padding(horizontal = 16.dp, vertical = 12.dp)
 *         .background(Color.Blue)
 * ) {
 *     Text(
 *         text = "Enviar Bizum",
 *         textAlign = TextAlign.Center,
 *         modifier = Modifier.fillMaxWidth()
 *     )
 * }
 */
fun buildBoxComponentByteArray(): ByteArray {
    val width = (400 * DEFAULT_DENSITY).toInt()
    val height = (72 * DEFAULT_DENSITY).toInt()
    val writer = RemoteComposeWriter(
        JvmRcPlatformServices(),
        RemoteComposeWriter.HTag(Header.DOC_WIDTH, width),
        RemoteComposeWriter.HTag(Header.DOC_HEIGHT, height),
    )

    val textId = writer.addText("Enviar Bizum")

    writer.root {
        writer.startBox(
            RecordingModifier()
                .fillMaxWidth()
                .height(dp(72))
                .background(argb("#FFFFFF")),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.startBox(
            RecordingModifier()
                .fillMaxWidth()
                .padding(dp(16), dp(12), dp(16), dp(12))
                .background(argb("#0000FF")),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.textComponent(
            RecordingModifier().fillMaxWidth(),
            textId,
            argb("#FFFFFF"),
            sp(16),
            0,
            500f,
            null,
            CoreText.TEXT_ALIGN_CENTER,
            0,
            1
        ) {}

        writer.endBox()
        writer.endBox()
    }

    return writer.encodeToByteArray()
}
