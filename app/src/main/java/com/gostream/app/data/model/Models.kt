package com.gostream.app.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: T?
)

data class ProviderItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("content_type") val contentType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("icon_url") val iconUrl: String?
) : Serializable

data class VideoItem(
    @SerializedName("id") val id: String,
    @SerializedName("model_id") val modelId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("episode_count") val episodeCount: Int? = 0
) : Serializable

data class EpisodeItem(
    @SerializedName("id") val id: String,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: String? = null
) : Serializable

data class StreamSource(
    val url: String,
    val isHls: Boolean = false,
    val isDash: Boolean = false,
    val headers: Map<String, String> = emptyMap()
)
