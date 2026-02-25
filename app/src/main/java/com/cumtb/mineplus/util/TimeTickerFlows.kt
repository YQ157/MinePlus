package com.cumtb.mineplus.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 以“分钟边界”对齐的 ticker：
 * - 立刻 emit 当前时间
 * - 之后每到下一分钟边界 emit 一次（适合刷新“进行中/即将开始”这类分钟级 UI）
 */
fun minuteAlignedTickerFlow(
    clock: Clock = Clock.systemDefaultZone(),
    interval: Duration = Duration.ofMinutes(1)
): Flow<Instant> = flow {
    require(!interval.isNegative && !interval.isZero) { "interval must be positive" }

    while (true) {
        val now = clock.instant()
        emit(now)

        // Align to next boundary: ceil(now / interval) * interval
        val intervalMs = interval.toMillis()
        val nowMs = now.toEpochMilli()
        val nextMs = ((nowMs / intervalMs) + 1) * intervalMs
        val delayMs = (nextMs - nowMs).coerceAtLeast(0)
        delay(delayMs)
    }
}
