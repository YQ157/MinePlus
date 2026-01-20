package com.cumtb.mineplus.data.model

data class DatumRequest(
    val lessonIds: List<Long>,  // 从 get-data 接口提取出来的 ID 列表
    val studentId: Long? = null, // 通常传 null
    val stdPersonId: Long?,      // 关键参数！需要从 HTML 解析
    val weekIndex: Int? = null   // 通常传 null，获取所有周
)