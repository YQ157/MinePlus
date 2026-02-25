package com.cumtb.mineplus.ui.today.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cumtb.mineplus.data.model.CourseSchedule

@Composable
fun TodayCourseCard(course: CourseSchedule) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
