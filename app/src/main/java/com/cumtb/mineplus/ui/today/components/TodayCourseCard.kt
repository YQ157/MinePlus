package com.cumtb.mineplus.ui.today.components

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
import androidx.compose.ui.Modifier
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

    // All cards have a background color; emphasized cards use a stronger tone.
    val containerColor = when {
        isInProgress -> MaterialTheme.colorScheme.primaryContainer
        isStartingSoon -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val contentColor = when {
        isInProgress -> MaterialTheme.colorScheme.onPrimaryContainer
        isStartingSoon -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
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
        }
    }
}