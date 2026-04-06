package com.parallelc.micts.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

data class CascadeMenuItem(
    val text: String,
    val subItems: List<CascadeMenuItem>? = null,
    val onSelected: (() -> Unit)? = null,
)

@Composable
fun CascadingMenuPopup(
    show: Boolean,
    items: List<CascadeMenuItem>,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    onDismissRequest: () -> Unit,
) {
    val subItems = remember { mutableStateOf<List<CascadeMenuItem>?>(null) }

    OverlayListPopup(
        show = show,
        alignment = alignment,
        onDismissRequest = onDismissRequest,
        onDismissFinished = { subItems.value = null },
    ) {
        AnimatedContent(
            targetState = subItems.value,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "cascadeMenu",
        ) { currentSubItems ->
            val page = currentSubItems ?: items
            ListPopupColumn {
                page.forEachIndexed { index, item ->
                    DropdownImpl(
                        text = item.text,
                        optionSize = page.size,
                        isSelected = false,
                        onSelectedIndexChange = {
                            if (item.subItems != null) {
                                subItems.value = item.subItems
                            } else {
                                item.onSelected?.invoke()
                                onDismissRequest()
                            }
                        },
                        index = index,
                    )
                }
            }
        }
    }
}
