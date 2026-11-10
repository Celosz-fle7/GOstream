package com.gostream.app.data.moviebox

import android.content.Context
import com.gostream.app.GOStreamApplication
import com.gostream.app.data.model.EpisodeItem
import com.gostream.app.data.model.StreamSource
import com.gostream.app.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MovieBoxCategory(
    val id: String,
    val name: String,
    val tabId: Int,
    val isFilm: Boolean
)

object MovieBoxClient {

    private const val BASE_URL = "https://api4sg.aoneroom.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val prefs by lazy {
        GOStreamApplication.instance.getSharedPreferences("moviebox_session_prefs", Context.MODE_PRIVATE)
    }

    private var authToken: String?
        get() = prefs.getString("mb_auth_token", null)
        set(value) = prefs.edit().putString("mb_auth_token", value).apply()

    suspend fun getCategories(platform: String): List<MovieBoxCategory> {
        return if (platform.equals("FILM", ignoreCase = true)) {
            listOf(
                MovieBoxCategory("film_trending", "🔥 Trending Film", 1, isFilm = true),
                MovieBoxCategory("film_box_office", "🎬 Film Layar Lebar", 2, isFilm = true),
                MovieBoxCategory("film_indo", "🇮🇩 Film Indonesia", 29, isFilm = true)
            )
        } else {
            listOf(
                MovieBoxCategory("drama_short", "⚡ Short TV (Micro-Drama)", 13, isFilm = false),
                MovieBoxCategory("drama_asian", "🌏 Asian Drama & Drakor", 18, isFilm = false),
                MovieBoxCategory("drama_serial", "📺 Serial TV Pilihan", 5, isFilm = false),
                MovieBoxCategory("drama_anime", "⛩️ Anime Series", 8, isFilm = false)
            )
        }
    }

    suspend fun fetchFeed(tabId: Int, page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        ensureHandshake()

        val path = if (tabId == 1 || tabId == 8 || tabId == 18) {
            "/wefeed-mobile-bff/tab/ranking-list?tabId=$tabId"
        } else {
            "/wefeed-mobile-bff/tab-operating?tabId=$tabId&page=$page"
        }

        try {
            val response = executeRequest("GET", path)
            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: return@withContext emptyList()

            val subjectsList = mutableListOf<VideoItem>()

            // 1. Parsing format ranking-list (data.categoryList[].subjects[])
            val categoryList = dataObj.optJSONArray("categoryList")
            if (categoryList != null) {
                for (i in 0 until categoryList.length()) {
                    val cat = categoryList.optJSONObject(i) ?: continue
                    val subArray = cat.optJSONArray("subjects") ?: continue
                    for (j in 0 until subArray.length()) {
                        val sub = subArray.optJSONObject(j) ?: continue
                        mapSubject(sub, tabId)?.let { subjectsList.add(it) }
                    }
                }
            }

            // 2. Parsing format tab-operating (data.operatingCards[] or data.sections[])
            val cards = dataObj.optJSONArray("operatingCards") ?: dataObj.optJSONArray("items")
            if (cards != null) {
                for (i in 0 until cards.length()) {
                    val card = cards.optJSONObject(i) ?: continue
                    val subArray = card.optJSONArray("subjects") ?: card.optJSONArray("items") ?: continue
                    for (j in 0 until subArray.length()) {
                        val sub = subArray.optJSONObject(j) ?: continue
                        mapSubject(sub, tabId)?.let { subjectsList.add(it) }
                    }
                }
            }

            subjectsList.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun search(query: String, page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        ensureHandshake()

        val path = "/wefeed-mobile-bff/subject-api/search"
        val payload = JSONObject().apply {
            put("keyword", query)
            put("page", page)
            put("perPage", 20)
        }.toString()

        try {
            val response = executeRequest("POST", path, payload)
            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: return@withContext emptyList()
            val items = dataObj.optJSONArray("items") ?: return@withContext emptyList()

            val results = mutableListOf<VideoItem>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                mapSubject(item, 0)?.let { results.add(it) }
            }
            results.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchEpisodes(subjectId: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        ensureHandshake()

        // 1. Coba jalur ShortTV Mini-List
        val shortPath = "/wefeed-mobile-bff/shorts/mini-list?endPosition=50&pagerMode=1&startPosition=1&subjectId=$subjectId"
        try {
            val response = executeRequest("GET", shortPath)
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val root = JSONObject(body)
                val items = root.optJSONObject("data")?.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val epList = mutableListOf<EpisodeItem>()
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val epNum = item.optInt("episodeNumber", i + 1)
                        val title = item.optString("title", "Episode $epNum")
                        epList.add(EpisodeItem(id = "$epNum", episodeNumber = epNum, title = title))
                    }
                    return@withContext epList
                }
            }
        } catch (_: Exception) {}

        // 2. Jalur Film / Serial TV Standar
        (1..12).map {
            EpisodeItem(id = "$it", episodeNumber = it, title = "Episode $it")
        }
    }

    suspend fun resolveStreamUrl(subjectId: String, episodeNumber: Int = 1): StreamSource? = withContext(Dispatchers.IO) {
        ensureHandshake()

        // A. Coba jalur Shorts Mini-List (Direct CloudFront Signed MP4 720p)
        val shortPath = "/wefeed-mobile-bff/shorts/mini-list?endPosition=$episodeNumber&pagerMode=1&startPosition=$episodeNumber&subjectId=$subjectId"
        try {
            val response = executeRequest("GET", shortPath)
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val root = JSONObject(body)
                val items = root.optJSONObject("data")?.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val epItem = items.optJSONObject(0)
                    val mediaInfo = epItem?.optJSONObject("mediaInfo")
                    val resources = mediaInfo?.optJSONArray("videoResourceList")
                    if (resources != null && resources.length() > 0) {
                        val res = resources.optJSONObject(0)
                        val playUrl = res?.optString("videoPlayUrl")
                        if (!playUrl.isNullOrBlank()) {
                            return@withContext StreamSource(
                                url = playUrl,
                                isHls = playUrl.contains(".m3u8"),
                                headers = mapOf("User-Agent" to MovieBoxSigner.USER_AGENT)
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // B. Coba jalur play-info (Movie / DASH Manifest / CloudFront)
        val playPath = "/wefeed-mobile-bff/subject-api/play-info?subjectId=$subjectId&se=1&ep=$episodeNumber"
        try {
            val response = executeRequest("GET", playPath)
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val root = JSONObject(body)
                val streams = root.optJSONObject("data")?.optJSONArray("streams")
                if (streams != null && streams.length() > 0) {
                    val st = streams.optJSONObject(0)
                    val streamUrl = st?.optString("url") ?: ""
                    val signCookie = st?.optString("signCookie") ?: ""

                    if (streamUrl.isNotBlank()) {
                        val headers = mutableMapOf("User-Agent" to MovieBoxSigner.USER_AGENT)
                        if (signCookie.isNotBlank()) {
                            headers["Cookie"] = signCookie
                        }
                        return@withContext StreamSource(
                            url = streamUrl,
                            isDash = streamUrl.contains(".mpd"),
                            isHls = streamUrl.contains(".m3u8"),
                            headers = headers
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun mapSubject(json: JSONObject, fallbackTabId: Int): VideoItem? {
        val id = json.optString("subjectId", json.optString("id", ""))
        if (id.isBlank()) return null

        val title = json.optString("title", json.optString("name", "Untitled"))
        val desc = json.optString("description", json.optString("desc", ""))

        var coverUrl = ""
        val coverObj = json.optJSONObject("cover")
        if (coverObj != null) {
            coverUrl = coverObj.optString("url", "")
        } else {
            coverUrl = json.optString("cover", "")
        }

        val epCount = json.optInt("totalEpisode", json.optInt("episodes", 1))
        val score = json.optString("score", "8.5")

        return VideoItem(
            id = id,
            modelId = "$fallbackTabId",
            title = title,
            posterUrl = coverUrl,
            overview = desc,
            status = "Skor: $score ★",
            episodeCount = epCount
        )
    }

    private fun ensureHandshake() {
        if (authToken != null) return

        try {
            val path = "/wefeed-mobile-bff/app/config"
            val sig = MovieBoxSigner.generateSignature("GET", path)
            val clientToken = MovieBoxSigner.generateClientToken()

            val request = Request.Builder()
                .url("$BASE_URL$path")
                .addHeader("x-tr-signature", sig)
                .addHeader("X-Client-Token", clientToken)
                .addHeader("X-Client-Info", MovieBoxSigner.getClientInfoJson())
                .addHeader("X-Client-Status", "1")
                .addHeader("user-agent", MovieBoxSigner.USER_AGENT)
                .addHeader("accept", "*/*")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val xUser = response.header("x-user")
            if (!xUser.isNullOrBlank()) {
                val uJson = JSONObject(xUser)
                val token = uJson.optString("token")
                if (token.isNotBlank()) {
                    authToken = token
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeRequest(method: String, pathWithQuery: String, bodyJson: String? = null): okhttp3.Response {
        val contentType = if (bodyJson != null) "application/json" else ""
        val sig = MovieBoxSigner.generateSignature(method, pathWithQuery, bodyJson ?: "", "*/*", contentType)

        val builder = Request.Builder()
            .url("$BASE_URL$pathWithQuery")
            .addHeader("x-tr-signature", sig)
            .addHeader("X-Client-Info", MovieBoxSigner.getClientInfoJson())
            .addHeader("X-Client-Status", "1")
            .addHeader("user-agent", MovieBoxSigner.USER_AGENT)
            .addHeader("accept", "*/*")

        authToken?.let {
            builder.addHeader("authorization", "Bearer $it")
        }

        if (method == "POST") {
            val body = (bodyJson ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
            builder.post(body)
        } else {
            builder.get()
        }

        val response = httpClient.newCall(builder.build()).execute()
        if (response.code == 401 || response.code == 441) {
            authToken = null
            MovieBoxSigner.resetDeviceId()
            ensureHandshake()
        }
        return response
    }
}
