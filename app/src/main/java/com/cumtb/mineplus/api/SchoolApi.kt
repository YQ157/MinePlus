package com.cumtb.mineplus.data.api

import com.cumtb.mineplus.data.model.CourseResponse
import com.cumtb.mineplus.data.model.DatumRequest
import com.cumtb.mineplus.data.model.ScheduleResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SchoolApi {

    // 1. 获取课表主页 HTML (用于解析 semesterId)
    // 假设教务系统课表主页是这个，如果不是请替换
    @GET("student/for-std/course-table")
    @Headers(
        "User-Agent: Mozilla/5.0 (Android) MinePlus/1.0" // 伪装成浏览器
    )
    suspend fun getCoursePageHtml(): ResponseBody

    // 2. 获取课表数据 JSON
    // URL: .../get-data?bizTypeId=2&semesterId=281
    @GET("student/for-std/course-table/get-data")
    suspend fun getScheduleData(
        @Query("semesterId") semesterId: Int,
        @Query("bizTypeId") bizTypeId: Int = 2
    ): CourseResponse

    // 获取课表日程
    @POST("student/ws/schedule-table/datum")
    suspend fun getScheduleDatum(@Body request: DatumRequest): ScheduleResponse
}
