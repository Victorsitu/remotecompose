package com.example.remotecompose.server

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.BitmapData
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
import com.example.remotecompose.shared.ElementConfig
import com.example.remotecompose.shared.LayoutConfig
import com.example.remotecompose.shared.parseColorLong
import java.net.URI
import javax.imageio.ImageIO

private const val ACTION_BUTTON_CLICKED = 1001
private const val DOC_WIDTH_DP = 360
private const val DOC_HEIGHT_DP = 800

private fun parseArgb(hex: String): Int = parseColorLong(hex).toInt()

private fun density(): Float = 2.625f

private fun dp(value: Int): Float = value * density()
private fun dp(value: Float): Float = value * density()
private fun sp(value: Int): Float = value * density()

private val platform = JvmRcPlatformServices()

private data class PaddingSides(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun paddingSides(el: ElementConfig, defaultHorizontal: Int = 0, defaultVertical: Int = 0): PaddingSides {
    val horizontal = el.paddingH ?: defaultHorizontal
    val vertical = el.paddingV ?: defaultVertical
    return PaddingSides(
        left = el.paddingLeft ?: horizontal,
        top = el.paddingTop ?: vertical,
        right = el.paddingRight ?: horizontal,
        bottom = el.paddingBottom ?: vertical
    )
}

private fun boxAlignment(value: String?): Int = when (value) {
    "start" -> BoxLayout.START
    "end" -> BoxLayout.END
    else -> BoxLayout.CENTER
}

private fun textAlignment(value: String?): Int = when (value) {
    "start" -> CoreText.TEXT_ALIGN_START
    "end" -> CoreText.TEXT_ALIGN_END
    else -> CoreText.TEXT_ALIGN_CENTER
}

private fun fontStyle(el: ElementConfig): Int = if (el.italic == true) 1 else 0

private fun fontWeight(el: ElementConfig, default: Int = 400): Float = (el.fontWeight ?: default).toFloat()

private fun fontFamily(el: ElementConfig): String? = when (el.fontFamily) {
    null, "" -> null
    "sans" -> "sans-serif"
    else -> el.fontFamily
}

private fun renderStyledText(
    writer: RemoteComposeWriter,
    modifier: RecordingModifier,
    textId: Int,
    color: Int,
    fontSize: Float,
    el: ElementConfig,
    defaultWeight: Int = 400,
) {
    if (el.underline == true) {
        writer.textComponent(
            modifier,
            textId,
            color,
            0,
            fontSize,
            0f,
            0f,
            fontStyle(el),
            fontWeight(el, defaultWeight),
            fontFamily(el),
            textAlignment(el.align),
            0,
            Int.MAX_VALUE,
            0f,
            0f,
            1f,
            0,
            0,
            0,
            true,
            false,
            null,
            null,
            false,
            0
        ) {}
        return
    }

    writer.textComponent(
        modifier,
        textId,
        color,
        fontSize,
        fontStyle(el),
        fontWeight(el, defaultWeight),
        fontFamily(el),
        textAlignment(el.align),
        0,
        Int.MAX_VALUE
    ) {}
}

fun buildDocument(config: LayoutConfig): ByteArray {
    val width = (DOC_WIDTH_DP * density()).toInt()
    val height = (DOC_HEIGHT_DP * density()).toInt()

    val writer = RemoteComposeWriter(
        platform,
        RemoteComposeWriter.HTag(Header.DOC_WIDTH, width),
        RemoteComposeWriter.HTag(Header.DOC_HEIGHT, height),
    )

    val bgColor = parseArgb(config.backgroundColor)
    val padding = config.padding ?: 24

    val rootMod = RecordingModifier()
        .fillMaxSize()
        .background(bgColor)

    if (config.scrollable) {
        rootMod.verticalScroll()
    }

    rootMod.padding(dp(padding))

    val arrangement = if (config.scrollable) ColumnLayout.TOP else ColumnLayout.CENTER

    writer.root {
        writer.column(rootMod, ColumnLayout.CENTER, arrangement) {
            renderRootElements(writer, config)
        }
    }

    return writer.encodeToByteArray()
}

private fun renderRootElements(writer: RemoteComposeWriter, config: LayoutConfig) {
    if (config.scrollable) {
        config.elements.forEach { element ->
            renderElement(writer, element, insideRow = false)
        }
        return
    }

    val top = config.elements.filter { it.vAlign == "top" }
    val center = config.elements.filter { it.vAlign == null || it.vAlign == "center" }
    val bottom = config.elements.filter { it.vAlign == "bottom" }

    top.forEach { element ->
        renderElement(writer, element, insideRow = false)
    }
    renderWeightedSpacer(writer)
    center.forEach { element ->
        renderElement(writer, element, insideRow = false)
    }
    renderWeightedSpacer(writer)
    bottom.forEach { element ->
        renderElement(writer, element, insideRow = false)
    }
}

private fun renderWeightedSpacer(writer: RemoteComposeWriter) {
    writer.startBox(RecordingModifier().fillMaxWidth().verticalWeight(1f))
    writer.endBox()
}

private fun renderElement(writer: RemoteComposeWriter, el: ElementConfig, insideRow: Boolean) {
    if (!insideRow) {
        writer.startBox(RecordingModifier().fillMaxWidth(), boxAlignment(el.align), BoxLayout.CENTER)
    }

    when (el.type) {
        "text" -> renderText(writer, el)
        "button" -> renderButton(writer, el, fillWidth = !insideRow && el.align == null)
        "textfield" -> renderTextField(writer, el, fillWidth = !insideRow && el.align == null)
        "spacer" -> renderSpacer(writer, el)
        "hspacer" -> renderHSpacer(writer, el)
        "divider" -> renderDivider(writer, el)
        "image" -> renderImage(writer, el, fillWidth = !insideRow && el.align == null)
        "card" -> renderCard(writer, el)
        "row" -> renderRow(writer, el)
        "column" -> renderColumn(writer, el)
    }

    if (!insideRow) {
        writer.endBox()
    }
}

private fun renderText(writer: RemoteComposeWriter, el: ElementConfig) {
    val color = parseArgb(el.color ?: "#000000")
    val fontSize = sp(el.fontSize ?: 16)
    val textId = writer.addText(el.text ?: "")

    val pad = paddingSides(el)
    val mod = RecordingModifier()
    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod.padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))
    }

    renderStyledText(writer, mod, textId, color, fontSize, el)
}

private fun renderButton(writer: RemoteComposeWriter, el: ElementConfig, fillWidth: Boolean) {
    val radius = el.cornerRadius ?: 24
    val bgColor = parseArgb(el.color ?: "#6200EA")
    val textColor = parseArgb(el.textColor ?: "#FFFFFF")
    val borderW = el.borderWidth ?: 0
    val borderColor = if (el.borderColor != null && borderW > 0) parseArgb(el.borderColor!!) else bgColor
    val shape = RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius))
    val pad = paddingSides(el, defaultHorizontal = 32, defaultVertical = 14)

    val actionName = el.actionName ?: el.id.ifEmpty { el.text ?: "button" }

    val mod = RecordingModifier()
    if ((el.width ?: 0) > 0) {
        mod.width(dp(el.width!!))
    } else if (fillWidth) {
        mod.fillMaxWidth()
    }
    if ((el.height ?: 0) > 0) {
        mod.height(dp(el.height!!))
    }
    mod.clip(shape)
        .background(bgColor)
        .border(dp(if (borderW > 0) borderW else 1), dp(radius), borderColor, ShapeType.ROUNDED_RECTANGLE)
        .onClick(HostAction(ACTION_BUTTON_CLICKED, writer.addText(actionName)))
        .padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))

    val textId = writer.addText(el.text ?: "Button")

    writer.startBox(mod, BoxLayout.CENTER, BoxLayout.CENTER)
    renderStyledText(writer, RecordingModifier(), textId, textColor, sp(el.fontSize ?: 16), el, defaultWeight = 600)
    writer.endBox()
}

private fun renderTextField(writer: RemoteComposeWriter, el: ElementConfig, fillWidth: Boolean) {
    val radius = el.cornerRadius ?: 12
    val bgColor = parseArgb(el.color ?: "#FFFFFF")
    val borderW = el.borderWidth ?: 1
    val borderColor = parseArgb(el.borderColor ?: "#D0D0D0")
    val pad = paddingSides(el, defaultHorizontal = 12, defaultVertical = 10)
    val shape = RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius))
    val textId = writer.addText(el.text?.takeIf { it.isNotBlank() } ?: el.placeholder ?: "")
    val actionName = el.actionName?.takeIf { it.isNotBlank() } ?: el.id.ifEmpty { "textfield" }

    val mod = RecordingModifier()
    if (fillWidth) {
        mod.fillMaxWidth()
    } else {
        mod.width(dp(el.width ?: 280))
    }
    mod.clip(shape)
        .background(bgColor)
        .border(dp(borderW), dp(radius), borderColor, ShapeType.ROUNDED_RECTANGLE)
        .onClick(HostAction(ACTION_BUTTON_CLICKED, writer.addText(actionName)))
        .padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))

    writer.startBox(mod, BoxLayout.START, BoxLayout.CENTER)
    writer.textComponent(
        RecordingModifier(),
        textId,
        parseArgb(el.textColor ?: "#111111"),
        sp(el.fontSize ?: 14),
        0,
        600f,
        null,
        CoreText.TEXT_ALIGN_START,
        0,
        Int.MAX_VALUE
    ) {}
    writer.endBox()
}

private fun renderSpacer(writer: RemoteComposeWriter, el: ElementConfig) {
    val mod = RecordingModifier().height(dp(el.height ?: 16))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderHSpacer(writer: RemoteComposeWriter, el: ElementConfig) {
    val mod = RecordingModifier().width(dp(el.width ?: 16))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderDivider(writer: RemoteComposeWriter, el: ElementConfig) {
    val color = parseArgb(el.color ?: "#CCCCCC")
    val mod = RecordingModifier()
        .fillMaxWidth()
        .height(dp(el.height ?: 1))
        .background(color)
        .padding(0f, dp(8), 0f, dp(8))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderImage(writer: RemoteComposeWriter, el: ElementConfig, fillWidth: Boolean) {
    val source = el.src ?: el.text
    if (source.isNullOrBlank()) {
        return
    }

    val imageId = loadRemoteBitmap(writer, source)
    val radius = el.cornerRadius ?: 0
    val pad = paddingSides(el)
    val mod = RecordingModifier()

    if (fillWidth) {
        mod.fillMaxWidth()
    } else {
        mod.width(dp(el.width ?: 160))
    }

    mod.height(dp(el.height ?: 180))

    if (radius > 0) {
        mod.clip(RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius)))
    }

    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod.padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))
    }

    writer.image(mod, imageId, RemoteComposeWriter.IMAGE_SCALE_FIT, 1f)
}

private fun loadRemoteBitmap(writer: RemoteComposeWriter, source: String): Int {
    val image = runCatching {
        URI(source).toURL().openStream().use { ImageIO.read(it) }
    }.getOrNull()

    if (image != null) {
        return writer.addBitmap(image)
    }

    val imageId = writer.nextId()
    BitmapData.apply(
        writer.buffer.buffer,
        imageId,
        BitmapData.TYPE_PNG,
        1,
        BitmapData.ENCODING_URL,
        1,
        source.toByteArray(Charsets.UTF_8)
    )
    return imageId
}

private fun renderCard(writer: RemoteComposeWriter, el: ElementConfig) {
    val radius = el.cornerRadius ?: 16
    val cardBg = parseArgb(el.color ?: "#FFFFFF")
    val borderW = el.borderWidth ?: 0
    val cardBorderColor = el.borderColor

    val alignment = when (el.align) {
        "start" -> ColumnLayout.START
        "end" -> ColumnLayout.END
        else -> ColumnLayout.CENTER
    }

    val pad = paddingSides(el, defaultHorizontal = 16, defaultVertical = 16)
    val shape = RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius))

    val mod = RecordingModifier().fillMaxWidth()

    if (cardBorderColor != null && borderW > 0) {
        mod.border(dp(borderW), dp(radius), parseArgb(cardBorderColor), ShapeType.ROUNDED_RECTANGLE)
    }

    if (radius > 0) {
        mod.clip(shape)
    }

    mod.background(cardBg)

    val cardAction = el.actionName
    if (cardAction != null) {
        mod.onClick(HostAction(ACTION_BUTTON_CLICKED, writer.addText(cardAction)))
    }

    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod.padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))
    }

    writer.startBox(mod)
    val innerMod = RecordingModifier().fillMaxWidth()
    writer.column(innerMod, alignment, ColumnLayout.TOP) {
        el.children?.forEach { child -> renderElement(writer, child, insideRow = false) }
    }
    writer.endBox()
}

private fun renderRow(writer: RemoteComposeWriter, el: ElementConfig) {
    val pad = paddingSides(el)
    val mod = RecordingModifier()
        .fillMaxWidth()
        .padding(0f, dp(4), 0f, dp(4))

    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod.padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))
    }

    writer.row(mod, RowLayout.SPACE_EVENLY, RowLayout.CENTER) {
        el.children?.forEach { child -> renderElement(writer, child, insideRow = true) }
    }
}

private fun renderColumn(writer: RemoteComposeWriter, el: ElementConfig) {
    val alignment = when (el.align) {
        "start" -> ColumnLayout.START
        "end" -> ColumnLayout.END
        else -> ColumnLayout.CENTER
    }
    val pad = paddingSides(el)
    val mod = RecordingModifier()
        .fillMaxWidth()
        .padding(0f, dp(4), 0f, dp(4))

    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod.padding(dp(pad.left), dp(pad.top), dp(pad.right), dp(pad.bottom))
    }

    writer.column(mod, alignment, ColumnLayout.TOP) {
        el.children?.forEach { child -> renderElement(writer, child, insideRow = false) }
    }
}
