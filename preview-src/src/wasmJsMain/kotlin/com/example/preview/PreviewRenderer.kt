package com.example.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remotecompose.shared.ElementConfig
import com.example.remotecompose.shared.LayoutConfig
import com.example.remotecompose.shared.parseColorLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.toInt
import org.jetbrains.skia.Image as SkiaImage

private fun parseColor(hex: String): Color = Color(parseColorLong(hex))

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

private fun containerAlignment(value: String?): Alignment = when (value) {
    "start" -> Alignment.CenterStart
    "end" -> Alignment.CenterEnd
    else -> Alignment.Center
}

private fun textAlignment(value: String?): TextAlign = when (value) {
    "start" -> TextAlign.Start
    "end" -> TextAlign.End
    else -> TextAlign.Center
}

private fun fontWeight(el: ElementConfig): FontWeight = FontWeight(el.fontWeight ?: 400)

private fun fontStyle(el: ElementConfig): FontStyle =
    if (el.italic == true) FontStyle.Italic else FontStyle.Normal

private fun textDecoration(el: ElementConfig): TextDecoration? =
    if (el.underline == true) TextDecoration.Underline else null

private fun fontFamily(el: ElementConfig): FontFamily? = when (el.fontFamily) {
    "Roboto" -> robotoFontFamily
    "sans", "sans-serif" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> null
}

@JsFun(
    """(url, onSuccess, onFailure) => {
        fetch(url)
            .then((response) => {
                if (!response.ok) throw new Error("HTTP " + response.status);
                return response.arrayBuffer();
            })
            .then((buffer) => {
                const source = new Uint8Array(buffer);
                const bytes = new Array(source.length);
                for (let i = 0; i < source.length; i++) bytes[i] = source[i];
                onSuccess(bytes);
            })
            .catch(() => onFailure());
    }"""
)
external fun fetchImageBytes(
    url: String,
    onSuccess: (JsArray<JsNumber>) -> Unit,
    onFailure: () -> Unit
)

@JsFun(
    """(url) => {
        if (!url) return "";
        if (/^(https?:|data:|blob:)/.test(url)) return url;
        if (url.startsWith("assets/")) return new URL("../" + url, window.location.href).href;
        if (url.startsWith("/assets/")) return new URL(".." + url, window.location.href).href;
        return new URL(url, window.location.href).href;
    }"""
)
external fun resolveImageUrl(url: String): String

@JsFun(
    """(url) => {
        const request = new XMLHttpRequest();
        request.open("GET", url, false);
        request.responseType = "arraybuffer";
        request.send(null);
        if (request.status < 200 || request.status >= 300) return [];
        const source = new Uint8Array(request.response);
        const bytes = new Array(source.length);
        for (let i = 0; i < source.length; i++) bytes[i] = source[i];
        return bytes;
    }"""
)
external fun loadBytesSync(url: String): JsArray<JsNumber>

private fun loadByteArraySync(url: String): ByteArray {
    val data = loadBytesSync(url)
    val bytes = ByteArray(data.length)
    for (i in 0 until data.length) {
        bytes[i] = (data[i]?.toInt() ?: 0).toByte()
    }
    return bytes
}

private val robotoFontFamily: FontFamily by lazy {
    runCatching {
        FontFamily(Font("Roboto", getData = { loadByteArraySync("fonts/Roboto-Regular.ttf") }))
    }.getOrDefault(FontFamily.SansSerif)
}

@Composable
fun PreviewRenderer(config: LayoutConfig, verticalAlignments: Map<String, String> = emptyMap()) {
    val bgColor = parseColor(config.backgroundColor)
    val scrollState = rememberScrollState()

    var modifier = Modifier.fillMaxSize().background(bgColor)
    if (config.scrollable) {
        modifier = modifier.verticalScroll(scrollState)
    }
    modifier = modifier.padding((config.padding ?: 24).dp)

    Column(
        modifier = modifier,
        verticalArrangement = if (config.scrollable) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (config.scrollable) {
            config.elements.forEach { element ->
                RenderElement(element)
            }
        } else {
            val top = config.elements.filter { verticalAlignments[it.id] == "top" }
            val center = config.elements.filter {
                verticalAlignments[it.id] == null || verticalAlignments[it.id] == "center"
            }
            val bottom = config.elements.filter { verticalAlignments[it.id] == "bottom" }

            top.forEach { element ->
                RenderElement(element)
            }
            Spacer(modifier = Modifier.weight(1f))
            center.forEach { element ->
                RenderElement(element)
            }
            Spacer(modifier = Modifier.weight(1f))
            bottom.forEach { element ->
                RenderElement(element)
            }
        }
    }
}

@Composable
private fun RenderElement(el: ElementConfig) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = containerAlignment(el.align)
    ) {
        RenderElementContent(el, fillWidth = el.align == null)
    }
}

@Composable
private fun RenderElementContent(el: ElementConfig, fillWidth: Boolean) {
    when (el.type) {
        "text" -> TextElement(el)
        "button" -> ButtonElement(el, fillWidth = fillWidth)
        "textfield" -> TextFieldElement(el, fillWidth = fillWidth)
        "spacer" -> SpacerElement(el)
        "divider" -> DividerElement(el)
        "image" -> ImageElement(el, fillWidth = fillWidth)
        "card" -> CardElement(el)
        "row" -> RowElement(el)
        "column" -> ColumnElement(el)
    }
}

@Composable
private fun TextElement(el: ElementConfig) {
    val pad = paddingSides(el)
    Text(
        text = el.text ?: "",
        fontSize = (el.fontSize ?: 16).sp,
        color = parseColor(el.color ?: "#000000"),
        fontWeight = fontWeight(el),
        fontFamily = fontFamily(el),
        fontStyle = fontStyle(el),
        textDecoration = textDecoration(el),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp),
        textAlign = textAlignment(el.align)
    )
}

@Composable
private fun ButtonElement(el: ElementConfig, fillWidth: Boolean = true) {
    val radius = el.cornerRadius ?: 24
    val bgColor = parseColor(el.color ?: "#6200EA")
    val shape = RoundedCornerShape(radius.dp)
    val pad = paddingSides(el, defaultHorizontal = 32, defaultVertical = 14)

    var mod = when {
        (el.width ?: 0) > 0 -> Modifier.width(el.width!!.dp)
        fillWidth -> Modifier.fillMaxWidth()
        else -> Modifier.widthIn(min = 120.dp)
    }
    if ((el.height ?: 0) > 0) {
        mod = mod.height(el.height!!.dp)
    }
    mod = mod
        .clip(shape)
        .background(bgColor)

    if (el.borderColor != null && (el.borderWidth ?: 0) > 0) {
        mod = mod.border(
            width = (el.borderWidth ?: 1).dp,
            color = parseColor(el.borderColor!!),
            shape = shape
        )
    }

    mod = mod
        .clickable { }
        .padding(
            start = pad.left.dp,
            top = pad.top.dp,
            end = pad.right.dp,
            bottom = pad.bottom.dp
        )

    Box(
        modifier = mod,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = el.text ?: "Button",
            fontSize = (el.fontSize ?: 16).sp,
            color = parseColor(el.textColor ?: "#FFFFFF"),
            fontWeight = fontWeight(el).takeUnless { el.fontWeight == null } ?: FontWeight.SemiBold,
            fontFamily = fontFamily(el),
            fontStyle = fontStyle(el),
            textDecoration = textDecoration(el),
            textAlign = textAlignment(el.align)
        )
    }
}

@Composable
private fun TextFieldElement(el: ElementConfig, fillWidth: Boolean = true) {
    var value by remember(el.id, el.text) { mutableStateOf(el.text.orEmpty()) }
    val pad = paddingSides(el, defaultHorizontal = 12, defaultVertical = 10)

    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 220.dp, max = 360.dp))
            .padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp),
        placeholder = { Text(text = el.placeholder.orEmpty()) },
        singleLine = true
    )
}

@Composable
private fun SpacerElement(el: ElementConfig) {
    Spacer(modifier = Modifier.height((el.height ?: 16).dp))
}

@Composable
private fun DividerElement(el: ElementConfig) {
    HorizontalDivider(
        thickness = (el.height ?: 1).dp,
        color = parseColor(el.color ?: "#CCCCCC")
    )
}

@Composable
private fun ImageElement(el: ElementConfig, fillWidth: Boolean = true) {
    val source = el.text?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    var image by remember(source) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(source) { mutableStateOf(false) }

    LaunchedEffect(source) {
        image = null
        failed = false
        if (!source.isNullOrBlank()) {
            runCatching { loadRemoteImage(source) }
                .onSuccess { image = it }
                .onFailure { failed = true }
        }
    }

    val radius = el.cornerRadius ?: 0
    val shape = RoundedCornerShape(radius.dp)
    val pad = paddingSides(el)
    val modifier = Modifier
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width((el.width ?: 320).dp))
        .height((el.height ?: 180).dp)
        .padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp)
        .clip(shape)
        .background(Color(0xFFE8F5E9))
        .border(1.dp, Color(0xFFA5D6A7), shape)

    val bitmap = image
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (failed) "Image failed to load" else source ?: "Image",
                fontSize = 11.sp,
                color = Color(0xFF2E7D32),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private suspend fun loadRemoteImage(url: String): ImageBitmap {
    val data = suspendCoroutine<JsArray<JsNumber>> { continuation ->
        fetchImageBytes(
            url = url,
            onSuccess = { continuation.resume(it) },
            onFailure = { continuation.resumeWithException(IllegalStateException("Image failed to load")) }
        )
    }
    val bytes = ByteArray(data.length)
    for (i in 0 until data.length) {
        bytes[i] = (data[i]?.toInt() ?: 0).toByte()
    }
    return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}

@Composable
private fun CardElement(el: ElementConfig) {
    val radius = el.cornerRadius ?: 16
    val cardBg = parseColor(el.color ?: "#FFFFFF")
    val shape = RoundedCornerShape(radius.dp)

    val alignment = when (el.align) {
        "start" -> Alignment.Start
        "end" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    var mod = Modifier.fillMaxWidth()

    if (radius > 0) {
        mod = mod.clip(shape)
    }

    mod = mod.background(cardBg, shape)

    if (el.borderColor != null && (el.borderWidth ?: 0) > 0) {
        mod = mod.border(
            width = (el.borderWidth ?: 1).dp,
            color = parseColor(el.borderColor!!),
            shape = shape
        )
    }

    if (el.actionName != null) {
        mod = mod.clickable { }
    }

    val pad = paddingSides(el, defaultHorizontal = 16, defaultVertical = 16)
    if (pad.left > 0 || pad.top > 0 || pad.right > 0 || pad.bottom > 0) {
        mod = mod.padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp)
    }

    Box(modifier = mod) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            el.children?.forEach { child ->
                RenderElement(child)
            }
        }
    }
}

@Composable
private fun RowElement(el: ElementConfig) {
    val pad = paddingSides(el)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        el.children?.forEach { child ->
            RenderElementContent(child, fillWidth = false)
        }
    }
}

@Composable
private fun ColumnElement(el: ElementConfig) {
    val pad = paddingSides(el)
    val alignment = when (el.align) {
        "start" -> Alignment.Start
        "end" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp),
        horizontalAlignment = alignment
    ) {
        el.children?.forEach { child ->
            RenderElement(child)
        }
    }
}
