package com.nangmanroad.core.map

import com.nangmanroad.core.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class MapConfig(val keyId: String, val clientType: String)

internal fun localMapConfig() = MapConfig(BuildConfig.NAVER_NCP_KEY_ID, BuildConfig.NAVER_CLIENT_TYPE)

/** The map client ID is public configuration; never put API secrets in this response. */
internal suspend fun loadMapConfig(): MapConfig = withContext(Dispatchers.IO) {
    val fallback = localMapConfig()
    try {
        val url = URL(BuildConfig.MAP_CONFIG_URL)
        require(url.protocol == "https")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            require(connection.responseCode == 200)
            val body = connection.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(4097)
                val result = StringBuilder()
                while (result.length <= 4096) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, 4097 - result.length))
                    if (count < 0) break
                    result.append(buffer, 0, count)
                }
                require(result.length <= 4096)
                result.toString()
            }
            val json = JSONObject(body)
            val key = (json.opt("naverKeyId") as? String)?.trim().orEmpty()
            val type = if (json.has("clientType")) {
                json.opt("clientType") as? String ?: error("Invalid client type")
            } else "ncp"
            require(key.isNotEmpty() && type in setOf("ncp", "legacy"))
            MapConfig(key, type)
        } finally {
            connection.disconnect()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        fallback
    }
}
