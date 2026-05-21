package com.example.remotecompose.shared

import kotlinx.serialization.Serializable

@Serializable
data class LayoutConfig(
    val backgroundColor: String = "#F3E5F5",
    val scrollable: Boolean = false,
    val padding: Int? = null,
    val elements: List<ElementConfig> = emptyList(),
)

@Serializable
data class ElementConfig(
    val type: String,
    val id: String = "",
    val text: String? = null,
    val color: String? = null,
    val textColor: String? = null,
    val fontSize: Int? = null,
    val fontWeight: Int? = null,
    val fontFamily: String? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val height: Int? = null,
    val width: Int? = null,
    val src: String? = null,
    val cornerRadius: Int? = null,
    val borderColor: String? = null,
    val borderWidth: Int? = null,
    val paddingH: Int? = null,
    val paddingV: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
    val paddingLeft: Int? = null,
    val paddingRight: Int? = null,
    val placeholder: String? = null,
    val actionName: String? = null,
    val align: String? = null,
    val vAlign: String? = null,
    val children: List<ElementConfig>? = null,
)
