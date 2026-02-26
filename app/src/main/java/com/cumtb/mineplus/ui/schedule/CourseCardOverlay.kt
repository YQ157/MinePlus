package com.cumtb.mineplus.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.theme.CoursePalettes
import com.cumtb.mineplus.ui.theme.Dimens
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Immutable
internal data class CourseOverlayAnchor(
    val courseId: Int,
    val course: CourseSchedule,
    val boundsInRoot: Rect
)

/**
 * Host an overlay that expands a clicked course card.
 *
 * - Single expanded course at a time.
 * - Rendered as an overlay that can cover other content.
 * - Direction (expand up / down) picked based on remaining space.
 */
@Composable
internal fun CourseOverlayHost(
    expandedAnchor: CourseOverlayAnchor?,
    viewportSize: IntSize?,
    modifier: Modifier = Modifier,
    coursePalette: List<Color>,
    onDismissRequest: () -> Unit
) {
    // Transparent full-screen click layer (no gray scrim), to dismiss when tapping outside.
    AnimatedVisibility(
        visible = expandedAnchor != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (expandedAnchor == null) return@AnimatedVisibility

        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismissRequest() }
            )
        }
    }

    // Render the expanded card in a Popup so it won't be clipped by the scroll container.
    if (expandedAnchor != null) {
        CourseExpandedPopupInWindow(
            anchor = expandedAnchor,
            viewportSize = viewportSize,
            coursePalette = coursePalette
        )
    }
}

@Composable
private fun CourseExpandedPopupInWindow(
    anchor: CourseOverlayAnchor,
    viewportSize: IntSize?,
    coursePalette: List<Color>
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // tokens (smaller)
    val margin: Dp = 8.dp
    val maxPanelWidth: Dp = 280.dp
    val minPanelHeight: Dp = 160.dp
    val maxPanelHeight: Dp = 240.dp

    val marginPx = with(density) { margin.toPx() }
    val maxPanelWidthPx = with(density) { maxPanelWidth.toPx() }
    val minPanelHeightPx = with(density) { minPanelHeight.toPx() }
    val maxPanelHeightPx = with(density) { maxPanelHeight.toPx() }

    val viewportWidthPx = viewportSize?.width?.toFloat() ?: 0f
    val viewportHeightPx = viewportSize?.height?.toFloat() ?: 0f

    val bgColor = coursePalette.getOrElse(anchor.course.colorIndex) { Color.Gray }
    val textColor = if (bgColor.luminance() > 0.6f) Color(0xFF111111) else Color.White

    // Safety: if we don't know viewport yet, show centered popup.
    val hasViewport = viewportWidthPx > 0f && viewportHeightPx > 0f

    val card = anchor.boundsInRoot

    val panelWidthPx = if (hasViewport) {
        min(maxPanelWidthPx, (viewportWidthPx - 2 * marginPx).coerceAtLeast(0f))
    } else {
        maxPanelWidthPx
    }

    // Vertical height cap: never exceed available space.
    val spaceBelow = if (hasViewport) (viewportHeightPx - card.bottom - marginPx).coerceAtLeast(0f) else 0f
    val spaceAbove = if (hasViewport) (card.top - marginPx).coerceAtLeast(0f) else 0f

    // Choose expand direction by which side can fit more.
    val expandDown = if (!hasViewport) {
        true
    } else {
        // Prefer the side that can fit at least min height, otherwise take the larger side.
        when {
            spaceBelow >= minPanelHeightPx -> true
            spaceAbove >= minPanelHeightPx -> false
            else -> spaceBelow >= spaceAbove
        }
    }

    val availableHeight = if (!hasViewport) maxPanelHeightPx else {
        (if (expandDown) spaceBelow else spaceAbove)
            .coerceAtLeast(120f) // absolute minimum to avoid zero-height
            .coerceAtMost(maxPanelHeightPx)
    }
    val panelHeightPx = availableHeight.coerceAtLeast(minPanelHeightPx).coerceAtMost(maxPanelHeightPx)

    // Horizontal anchor.
    val desiredLeft = if (layoutDirection == LayoutDirection.Ltr) card.left else card.right - panelWidthPx
    val left = if (!hasViewport) {
        0f
    } else {
        val maxLeft = (viewportWidthPx - marginPx - panelWidthPx).coerceAtLeast(marginPx)
        desiredLeft.coerceIn(marginPx, maxLeft)
    }

    // Vertical anchor.
    val desiredTop = if (expandDown) card.bottom + marginPx else card.top - marginPx - panelHeightPx
    val top = if (!hasViewport) {
        0f
    } else {
        val maxTop = (viewportHeightPx - marginPx - panelHeightPx).coerceAtLeast(marginPx)
        desiredTop.coerceIn(marginPx, maxTop)
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(left.roundToInt(), top.roundToInt()),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(with(density) { panelWidthPx.toDp() })
                .height(with(density) { panelHeightPx.toDp() })
                .clip(RoundedCornerShape(16.dp)),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            color = bgColor
        ) {
            // Make content scroll if constrained.
            CourseExpandedContent(
                course = anchor.course,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun CourseExpandedContent(course: CourseSchedule, textColor: Color) {
    val scroll = rememberScrollState()
    val secondary = textColor.copy(alpha = 0.85f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = Dimens.large, vertical = Dimens.medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.small)
    ) {
        Text(
            text = course.courseName,
            style = MaterialTheme.typography.titleLarge,
            color = textColor
        )

        Spacer(modifier = Modifier.height(Dimens.tiny))

        InfoRow(
            label = "教师",
            value = course.teacher.ifBlank { "-" },
            labelColor = secondary,
            valueColor = textColor
        )
        InfoRow(
            label = "学分",
            value = course.credit.toString(),
            labelColor = secondary,
            valueColor = textColor
        )
        InfoRow(
            label = "计算加权",
            value = when (course.weightedCalc) {
                true -> "是"
                false -> "否"
                null -> "-"
            },
            labelColor = secondary,
            valueColor = textColor
        )
        InfoRow(
            label = "时间",
            value = "${course.rawStartTime} - ${course.rawEndTime}",
            labelColor = secondary,
            valueColor = textColor
        )
        InfoRow(
            label = "教室",
            value = course.room.ifBlank { "-" },
            labelColor = secondary,
            valueColor = textColor
        )

        val raw = course.rawScheduleText
        if (!raw.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.tiny))
            Text(
                text = "原始信息",
                style = MaterialTheme.typography.titleSmall,
                color = secondary
            )
            Text(
                text = raw,
                style = MaterialTheme.typography.bodyMedium,
                color = secondary
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )
    }
}
