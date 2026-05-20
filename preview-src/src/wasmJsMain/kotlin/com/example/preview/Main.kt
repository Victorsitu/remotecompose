package com.example.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.example.remotecompose.shared.LayoutConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

external interface MessageEventData : JsAny {
    val data: JsString?
}

@JsFun("(event) => { const d = event.data; return (typeof d === 'string') ? d : (typeof d === 'object' ? JSON.stringify(d) : ''); }")
external fun getMessageData(event: JsAny): String

var globalConfig: LayoutConfig = LayoutConfig()
var globalVerticalAlignments: Map<String, String> = emptyMap()
var configVersion: Int = 0

@JsFun("(callback) => { window.addEventListener('message', (e) => { callback(e); }); }")
external fun addMessageListener(callback: (JsAny) -> Unit)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: return

    addMessageListener { event ->
        try {
            val str = getMessageData(event)
            if (str.startsWith("{")) {
                globalVerticalAlignments = extractVerticalAlignments(str)
                globalConfig = json.decodeFromString<LayoutConfig>(str)
                configVersion++
            }
        } catch (_: Exception) {}
    }

    ComposeViewport(body) {
        var config by remember { mutableStateOf(globalConfig) }
        var version by remember { mutableIntStateOf(configVersion) }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            while (true) {
                if (configVersion != version) {
                    config = globalConfig
                    version = configVersion
                }
                kotlinx.coroutines.delay(50)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            PreviewRenderer(config, globalVerticalAlignments)
        }
    }
}

private fun extractVerticalAlignments(rawJson: String): Map<String, String> {
    val root = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return emptyMap()
    val result = mutableMapOf<String, String>()

    fun visit(element: JsonObject) {
        val id = (element["id"] as? JsonPrimitive)?.contentOrNull()
        val vAlign = (element["vAlign"] as? JsonPrimitive)?.contentOrNull()
        if (!id.isNullOrBlank() && !vAlign.isNullOrBlank()) {
            result[id] = vAlign
        }
        (element["children"] as? JsonArray)?.forEach { child ->
            (child as? JsonObject)?.let(::visit)
        }
    }

    (root["elements"] as? JsonArray)?.forEach { element ->
        (element as? JsonObject)?.let(::visit)
    }

    return result
}

private fun JsonPrimitive.contentOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()
