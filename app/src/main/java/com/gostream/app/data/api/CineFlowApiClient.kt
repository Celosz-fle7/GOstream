package com.gostream.app.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gostream.app.GOStreamApplication
import com.gostream.app.data.model.ApiResponse
import com.gostream.app.data.model.EpisodeItem
import com.gostream.app.data.model.ProviderItem
import com.gostream.app.data.model.StreamSource
import com.gostream.app.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object CineFlowApiClient {

    private const val BASE_URL = "https://ngintipya2.cineflow.my.id/"
    private const val USER_AGENT = "CineFlow/0.2.9 (com.cineflow.app; Android 14; SDK 34)"
    private const val APP_PKG = "com.cineflow.app"

    private val gson = Gson()
    private val prefs by lazy {
        GOStreamApplication.instance.getSharedPreferences("gostream_auth_prefs", Context.MODE_PRIVATE)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var accessToken: String?
        get() = prefs.getString("access_token", "cf_at_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjaW5lZmxvdyIsImF1ZCI6ImNpbmVmbG93LWFwaSIsInN1YiI6InVzcl8zMDgyYmI0YTQ2ZTE1ODM1OWNhMyIsInVzZXJfaWQiOiJ1c3JfMzA4MmJiNGE0NmUxNTgzNTljYTMiLCJhcHBfaW5zdGFuY2VfaWQiOiI1MmZjY2Y2NS05NjMyLTNhODEtOTQyNS0wMDFhODY3OGZlZmMiLCJkZXZpY2VfdHlwZSI6ImFuZHJvaWRfbW9iaWxlIiwidG9rZW5fdHlwZSI6ImFjY2VzcyIsImlhdCI6MTc5NDI5MjIzMywiZXhwIjoxNzk0Mjk0MDMzLCJqdGkiOiJhdF80NzU2ZjhhZWJmM2YxYjM0OWEyMjYwNTgwMzYyN2FiNiJ9.8GcL2yN5FZW_u6VpihWT0bhQLRcjn_BotCdvbNxkFbw")
        set(value) = prefs.edit().putString("access_token", value).apply()

    private var refreshToken: String?
        get() = prefs.getString("refresh_token", "cf_rt_0Z-_dpqGUB7bepw9k3eu5CyaWiMTyZdgaW9g3BfTsibRhvw8hlBukA")
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    private var appInstanceId: String
        get() {
            var id = prefs.getString("app_instance_id", null)
            if (id == null) {
                id = "52fccf65-9632-3a81-9425-001a8678fefc"
                prefs.edit().putString("app_instance_id", id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString("app_instance_id", value).apply()

    private fun buildRequest(endpoint: String, method: String = "GET", bodyJson: String? = null): Request {
        val url = if (endpoint.startsWith("http")) endpoint else "$BASE_URL$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("X-Requested-With", APP_PKG)
            .addHeader("Accept", "application/json")

        accessToken?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }

        if (method == "POST") {
            val body = (bodyJson ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
            builder.post(body)
        } else {
            builder.get()
        }

        return builder.build()
    }

    suspend fun getProviders(): List<ProviderItem> = withContext(Dispatchers.IO) {
        val request = buildRequest("api/modelles/models")
        try {
            val response = client.newCall(request).execute()
            if (response.code == 401) {
                refreshAuthToken()
                return@withContext getProviders()
            }
            val body = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<ApiResponse<List<ProviderItem>>>() {}.type
            val apiRes: ApiResponse<List<ProviderItem>> = gson.fromJson(body, type)
            apiRes.data?.filter { it.status == "active" } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getVideos(modelId: String, page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        val request = buildRequest("api/modelles/videos?model_id=$modelId&page=$page")
        try {
            val response = client.newCall(request).execute()
            if (response.code == 401) {
                refreshAuthToken()
                return@withContext getVideos(modelId, page)
            }
            val body = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<ApiResponse<List<VideoItem>>>() {}.type
            val apiRes: ApiResponse<List<VideoItem>> = gson.fromJson(body, type)
            apiRes.data ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStreamSource(modelId: String, episodeId: String, videoId: String): StreamSource? = withContext(Dispatchers.IO) {
        val request = buildRequest("api/modelles/source?model_id=$modelId&episode_id=$episodeId&id=$videoId")
        try {
            val response = client.newCall(request).execute()
            if (response.code == 401) {
                refreshAuthToken()
                return@withContext getStreamSource(modelId, episodeId, videoId)
            }
            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: return@withContext null
            val streamUrl = dataObj.optString("url")
            if (streamUrl.isBlank()) return@withContext null

            val isHls = streamUrl.contains(".m3u8")
            val isDash = streamUrl.contains(".mpd")

            StreamSource(
                url = streamUrl,
                isHls = isHls,
                isDash = isDash,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to BASE_URL
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun refreshAuthToken(): Boolean {
        val rToken = refreshToken ?: return false
        val payload = JSONObject().apply {
            put("refresh_token", rToken)
            put("app_instance_id", appInstanceId)
        }.toString()

        val request = Request.Builder()
            .url("${BASE_URL}api/app/auth/refresh-token")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("X-Requested-With", APP_PKG)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return false
                val root = JSONObject(body)
                val tokenInfo = root.optJSONObject("data")?.optJSONObject("token_info")
                val newAt = tokenInfo?.optString("access_token")
                val newRt = tokenInfo?.optString("refresh_token")
                if (!newAt.isNullOrBlank()) {
                    accessToken = newAt
                    if (!newRt.isNullOrBlank()) refreshToken = newRt
                    true
                } else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
