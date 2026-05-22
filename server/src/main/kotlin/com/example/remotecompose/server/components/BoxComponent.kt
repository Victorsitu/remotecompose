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

private class LinearGradientBackgroundModifier(
    private val width: Float,
    private val height: Float,
    private val startColor: Int,
    private val endColor: Int,
) : RecordingModifier.Element {
    override fun write(writer: RemoteComposeWriter) {
        writer.getRcPaint()
            .setLinearGradient(
                0f,
                height,
                width,
                0f,
                intArrayOf(startColor, endColor),
                floatArrayOf(0f, 1f),
                0
            )
            .commit()
        writer.drawRect(0f, 0f, width, height)
    }
}

private fun RecordingModifier.brandGradientBackground(width: Float, height: Float): RecordingModifier =
    then(
        LinearGradientBackgroundModifier(
            width = width,
            height = height,
            startColor = argb("#01EBBF"),
            endColor = argb("#01F685")
        )
    )

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
//background: linear-gradient(45deg, var(--QDS-brand-gradient-01-main-left, #01EBBF) 0%, var(--QDS-brand-gradient-01-main-right, #01F685) 100%);
    writer.root {
        writer.startBox(
            RecordingModifier()
                .fillMaxWidth()
                .height(dp(160))
                .background(argb("#FFFFFF"))
                .border(dp(0), dp(20), argb("#FFFFFF"), 1),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.startBox(
            RecordingModifier()
                .fillMaxSize()
                .padding(dp(16), dp(0), dp(16), dp(0))
                .brandGradientBackground(width = dp(400 - 32), height = dp(160)),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.textComponent(
            RecordingModifier().fillMaxWidth().height(dp(80)),
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
