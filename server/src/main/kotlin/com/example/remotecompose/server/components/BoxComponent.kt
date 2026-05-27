package com.example.remotecompose.server.components

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape

private const val DEFAULT_DENSITY = 2.625f
private const val ACTION_BANNER_CLICKED = 1001

private fun dp(value: Int): Float = value * DEFAULT_DENSITY
private fun sp(value: Int): Float = value * DEFAULT_DENSITY
private fun argb(hex: String): Int = hex.removePrefix("#").toLong(16).let { rgb ->
    if (hex.length <= 7) (0xFF000000 or rgb).toInt() else rgb.toInt()
}

private class SoftShadowModifier(
    private val width: Float,
    private val height: Float,
    private val radius: Float,
) : RecordingModifier.Element {
    override fun write(writer: RemoteComposeWriter) {
        writer.getRcPaint()
            .setColor(argb("#1F000000"))
            .setAntiAlias(true)
            .commit()
        writer.drawRoundRect(0f, dp(6), width, height + dp(6), radius, radius)
    }
}

private class ThumbnailModifier(
    private val width: Float,
    private val height: Float,
) : RecordingModifier.Element {
    override fun write(writer: RemoteComposeWriter) {
        writer.getRcPaint()
            .setLinearGradient(
                0f,
                0f,
                0f,
                height,
                intArrayOf(argb("#6FA1C2"), argb("#D9F2F4"), argb("#4F7E91")),
                floatArrayOf(0f, 0.52f, 1f),
                0
            )
            .setAntiAlias(true)
            .commit()
        writer.drawRect(0f, 0f, width, height)

        writer.getRcPaint().setColor(argb("#EEF5EA")).setAntiAlias(true).commit()
        writer.drawOval(dp(-14), dp(27), dp(62), dp(54))

        writer.getRcPaint().setColor(argb("#8CC0D1")).setStrokeWidth(dp(2)).setStyle(1).commit()
        writer.drawLine(dp(0), dp(30), dp(47), dp(25))
        writer.drawLine(dp(0), dp(37), dp(47), dp(32))

        writer.getRcPaint().setColor(argb("#9A633D")).setStyle(0).commit()
        writer.drawRect(dp(28), dp(35), width, height)

        writer.getRcPaint().setColor(argb("#4C6670")).setAntiAlias(true).commit()
        writer.drawRoundRect(dp(22), dp(17), dp(30), dp(33), dp(7), dp(7))
        writer.drawRect(dp(25), dp(22), dp(33), dp(33))
        writer.drawRect(dp(33), dp(30), dp(46), dp(33))
    }
}

private class CloseIconModifier(private val size: Float) : RecordingModifier.Element {
    override fun write(writer: RemoteComposeWriter) {
        val center = size / 2f
        writer.getRcPaint().setColor(argb("#707070")).setAntiAlias(true).commit()
        writer.drawCircle(center, center, center)

        writer.getRcPaint()
            .setColor(argb("#FFFFFF"))
            .setStrokeWidth(dp(2))
            .setStrokeCap(1)
            .setAntiAlias(true)
            .commit()
        writer.drawLine(dp(8), dp(8), size - dp(8), size - dp(8))
        writer.drawLine(size - dp(8), dp(8), dp(8), size - dp(8))
    }
}

/**
 * Remote Compose component generated directly from Kotlin.
 *
 * Equivalent visual structure:
 *
 * A centered mint card with a thumbnail, title/subtitle text block, and close affordance.
 */
fun buildBoxComponentByteArray(): ByteArray {
    val width = (400 * DEFAULT_DENSITY).toInt()
    val height = (90 * DEFAULT_DENSITY).toInt()
    val writer = RemoteComposeWriter(
        JvmRcPlatformServices(),
        RemoteComposeWriter.HTag(Header.DOC_WIDTH, width),
        RemoteComposeWriter.HTag(Header.DOC_HEIGHT, height),
    )

    val titleId = writer.addText("Enviar Bizum")
    val subtitleId = writer.addText("Test Component")
    val bannerActionId = writer.addText("banner_click:navigateCMN")
    val cardWidth = dp(368)
    val cardHeight = dp(78)
    val cardRadius = dp(6)

    writer.root {
        writer.startBox(
            RecordingModifier()
                .fillMaxWidth()
                .height(dp(90))
                .background(argb("#FFFFFF")),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.startBox(
            RecordingModifier()
                .fillMaxWidth()
                .height(cardHeight)
                .then(SoftShadowModifier(cardWidth, cardHeight, cardRadius)),
            BoxLayout.CENTER,
            BoxLayout.CENTER
        )

        writer.row(
            RecordingModifier()
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedRectShape(cardRadius, cardRadius, cardRadius, cardRadius))
                .background(argb("#2538B8"))
                .border(dp(1), cardRadius, argb("#2538B8"), ShapeType.ROUNDED_RECTANGLE)
                .onClick(HostAction(ACTION_BANNER_CLICKED, bannerActionId))
                .padding(dp(16), dp(16), dp(10), dp(15))
                .spacedBy(dp(12)),
            RowLayout.START,
            RowLayout.CENTER
        ) {
            writer.startBox(
                RecordingModifier()
                    .width(dp(47))
                    .height(dp(47))
                    .clip(RoundedRectShape(0f, 0f, 0f, 0f))
                    .then(ThumbnailModifier(dp(47), dp(47)))
            )
            writer.endBox()

            writer.column(
                RecordingModifier()
                    .horizontalWeight(1f)
                    .height(dp(48)),
                ColumnLayout.START,
                ColumnLayout.CENTER
            ) {
                writer.textComponent(
                    RecordingModifier().fillMaxWidth().height(dp(24)),
                    titleId,
                    argb("#222222"),
                    sp(15),
                    0,
                    700f,
                    null,
                    CoreText.TEXT_ALIGN_START,
                    0,
                    1
                ) {}

                writer.textComponent(
                    RecordingModifier().fillMaxWidth().height(dp(22)),
                    subtitleId,
                    argb("#444444"),
                    sp(13),
                    0,
                    400f,
                    null,
                    CoreText.TEXT_ALIGN_START,
                    0,
                    1
                ) {}
            }

            writer.startBox(
                RecordingModifier()
                    .width(dp(24))
                    .height(dp(24))
                    .then(CloseIconModifier(dp(24)))
            )
            writer.endBox()
        }

        writer.endBox()
        writer.endBox()
    }

    return writer.encodeToByteArray()
}
