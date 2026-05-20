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
import androidx.compose.ui.text.font.FontWeight
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
    when (el.type) {
        "text" -> TextElement(el)
        "button" -> ButtonElement(el)
        "spacer" -> SpacerElement(el)
        "divider" -> DividerElement(el)
        "image" -> ImageElement(el)
        "card" -> CardElement(el)
        "row" -> RowElement(el)
    }
}

@Composable
private fun TextElement(el: ElementConfig) {
    val padH = el.paddingH ?: 0
    val padV = el.paddingV ?: 0
    Text(
        text = el.text ?: "",
        fontSize = (el.fontSize ?: 16).sp,
        color = parseColor(el.color ?: "#000000"),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .then(if (padH > 0 || padV > 0) Modifier.padding(horizontal = padH.dp, vertical = padV.dp) else Modifier)
    )
}

@Composable
private fun ButtonElement(el: ElementConfig, fillWidth: Boolean = true) {
    val radius = el.cornerRadius ?: 24
    val bgColor = parseColor(el.color ?: "#6200EA")
    val shape = RoundedCornerShape(radius.dp)

    var mod = if (fillWidth) Modifier.fillMaxWidth() else Modifier
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
            horizontal = (el.paddingH ?: 32).dp,
            vertical = (el.paddingV ?: 14).dp
        )

    Box(
        modifier = mod,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = el.text ?: "Button",
            fontSize = (el.fontSize ?: 16).sp,
            color = parseColor(el.textColor ?: "#FFFFFF"),
            fontWeight = FontWeight.SemiBold
        )
    }
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
private fun ImageElement(el: ElementConfig) {
    val source = el.text
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
    val modifier = Modifier
        .fillMaxWidth()
        .height((el.height ?: 180).dp)
        .clip(shape)
        .background(Color(0xFFE8F5E9))
        .border(1.dp, Color(0xFFA5D6A7), shape)

    val bitmap = image
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
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

    val padH = el.paddingH ?: 16
    val padV = el.paddingV ?: 16
    if (padH > 0 || padV > 0) {
        mod = mod.padding(horizontal = padH.dp, vertical = padV.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        el.children?.forEach { child ->
            when (child.type) {
                "button" -> ButtonElement(child, fillWidth = false)
                else -> RenderElement(child)
            }
        }
    }
}
