package com.gostream.app.data.moviebox

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object MovieBoxSigner {

    private val GATEWAY_SECRET_BYTES = Base64.decode(
        "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O",
        Base64.DEFAULT
    )

    const val USER_AGENT =
        "com.community.oneroom/50020132 (Linux; U; Android 16; id_ID; Infinix X6870; Build/BP2A.250605.031.A3; Cronet/155.0.8059.16)"

    var deviceId: String = generateDeviceId()
        private set

    fun generateDeviceId(): String {
        val bytes = ByteArray(16)
        Random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun resetDeviceId() {
        deviceId = generateDeviceId()
    }

    fun generateClientToken(): String {
        val ts = System.currentTimeMillis().toString()
        val revTs = ts.reversed()
        val md5Rev = md5(revTs)
        return "$ts,$md5Rev"
    }

    fun getClientInfoJson(): String {
        return JSONObject().apply {
            put("package_name", "com.community.oneroom")
            put("version_name", "4.0.03.1028.03")
            put("version_code", 50020132)
            put("os", "android")
            put("os_version", "15")
            put("device_id", deviceId)
            put("install_store", "gp")
            put("brand", "Infinix")
            put("model", "Infinix X6870")
            put("system_language", "in")
            put("net", "NETWORK_WIFI")
            put("region", "ID")
            put("timezone", "Asia/Jakarta")
            put("sp_code", "51010")
            put("X-Play-Mode", "2")
            put("X-Idle-Data", "1")
            put("X-Family-Mode", "1")
            put("X-PM-Active", "true")
            put("X-PM-Level", "2")
        }.toString()
    }

    fun formatPathQuery(pathWithQuery: String): String {
        if (!pathWithQuery.contains('?')) return pathWithQuery
        val parts = pathWithQuery.split('?', limit = 2)
        val path = parts[0]
        val query = if (parts.size > 1) parts[1] else ""
        if (query.isBlank()) return path

        val uri = Uri.parse("https://dummy$pathWithQuery")
        val paramNames = uri.queryParameterNames.toList().sorted()
        if (paramNames.isEmpty()) return path

        val sortedQuery = paramNames.joinToString("&") { key ->
            val value = uri.getQueryParameter(key) ?: ""
            "$key=$value"
        }
        return "$path?$sortedQuery"
    }

    fun generateSignature(
        method: String,
        pathWithQuery: String,
        body: String = "",
        accept: String = "*/*",
        contentType: String = ""
    ): String {
        val ts = System.currentTimeMillis().toString()
        val cl = if (body.isNotEmpty()) body.toByteArray(Charsets.UTF_8).size.toString() else ""
        val bodyMd5 = if (body.isNotEmpty()) {
            val trunc = if (body.length > 102400) body.substring(0, 102400) else body
            md5(trunc)
        } else ""
        val formattedPath = formatPathQuery(pathWithQuery)

        val stringToSign = listOf(
            method.uppercase(),
            accept,
            contentType,
            cl,
            ts,
            bodyMd5,
            formattedPath
        ).joinToString("\n")

        val mac = Mac.getInstance("HmacMD5")
        val secretKeySpec = SecretKeySpec(GATEWAY_SECRET_BYTES, "HmacMD5")
        mac.init(secretKeySpec)
        val hmacDigest = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
        val sig = Base64.encodeToString(hmacDigest, Base64.NO_WRAP)

        return "$ts|2|$sig"
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
