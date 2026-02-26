package com.cumtb.mineplus.ui.today.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.util.CourseTimeStatus

@Composable
fun TodayCourseCard(
    course: CourseSchedule,
    status: CourseTimeStatus
) {
    val isInProgress = status == CourseTimeStatus.IN_PROGRESS
    val isStartingSoon = status == CourseTimeStatus.STARTING_SOON
    val isEmphasized = isInProgress || isStartingSoon

    val darkTheme = isSystemInDarkTheme()

    // Low-saturation, comfortable status colors (explicit light/dark variants).
    // - STARTING_SOON: warm apricot / soft coral (gentle but attention-grabbing)
    // - IN_PROGRESS: lake blue / muted teal (stable, "in progress")
    val startingSoonContainer = if (darkTheme) Color(0xFF4A342C) else Color(0xFFFFDCCB)
    val startingSoonContent = if (darkTheme) Color(0xFFFFE8DD) else Color(0xFF4C1E11)

    val inProgressContainer = if (darkTheme) Color(0xFF103A42) else Color(0xFFD6F2F5)
    val inProgressContent = if (darkTheme) Color(0xFFBCECEF) else Color(0xFF06323A)

    val baseContainerColor = when {
        isInProgress -> inProgressContainer
        isStartingSoon -> startingSoonContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val contentColor = when {
        isInProgress -> inProgressContent
        isStartingSoon -> startingSoonContent
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Breath only for "starting soon". The animation starts at 0 so the initial color is unchanged.
    val breath: Float = if (!isStartingSoon) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "TodayCourseCardBreathing")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        v
    }

    // Make the container get slightly brighter (same hue family) instead of darker.
    val brightenTarget = contentColor

    val containerColor = if (!isEmphasized) {
        baseContainerColor
    } else {
        val strength = if (isStartingSoon) 0.05f else 0.03f
        lerp(baseContainerColor, brightenTarget, breath * strength)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = if (isEmphasized) 16.dp else 12.dp
            )
        ) {
            Row {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val badgeText = when (status) {
                    CourseTimeStatus.IN_PROGRESS -> "进行中"
                    CourseTimeStatus.STARTING_SOON -> "即将开始"
                    else -> null
                }

                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = course.room.ifBlank { "未填写教室" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buildString {
                        append("第")
                        append(course.startNode)
                        if (course.step > 1) {
                            append("-")
                            append(course.startNode + course.step - 1)
                        }
                        append("节")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "${course.rawStartTime}-${course.rawEndTime}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Debug UI intentionally removed in this tuned version.
            // Keep SHOW_TODAY_COURSE_CARD_DEBUG for quick re-enable later.
        }
    }
}
