package com.nangmanroad.server

import com.nangmanroad.core.data.VerifiedPlaces
import com.nangmanroad.core.sync.Api
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/** The provider ID, also used by the official place page, ties each photo to its exact campsite. */
internal object GoCampingPhotos {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()

    fun load(serviceKey: String): Map<String, List<String>> {
        val key = URLEncoder.encode(serviceKey.trim(), StandardCharsets.UTF_8)
        return VerifiedPlaces.all.associate { place ->
            val campId = place.id.removePrefix("gocamping-")
            val uri = URI("https://apis.data.go.kr/B551011/GoCamping/imageList" +
                "?serviceKey=$key&MobileOS=ETC&MobileApp=ParkIt&_type=json&numOfRows=12&pageNo=1&contentId=$campId")
            val request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).GET().build()
            val photos = runCatching {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) readPhotos(response.body(), campId) else emptyList()
            }.getOrDefault(emptyList())
            place.id to photos
        }
    }

    internal fun readPhotos(json: String, campId: String): List<String> {
        val result = Api.json.decodeFromString(ImageResponse.serializer(), json).response
        if (result.header.resultCode !in setOf("0000", "00")) return emptyList()
        return result.body?.items?.item.orEmpty().asSequence()
            .filter { it.contentId == campId }
            .mapNotNull { item ->
                val url = runCatching { URI(item.imageUrl) }.getOrNull()
                val host = url?.host?.lowercase()
                if (host !in setOf("gocamping.or.kr", "www.gocamping.or.kr") ||
                    url == null || url.userInfo != null || url.port != -1 ||
                    !url.path.orEmpty().startsWith("/upload/camp/$campId/") ||
                    url.scheme !in setOf("http", "https")
                ) null else item.imageUrl.replaceFirst("http://", "https://")
            }
            .distinct().take(6).toList()
    }

    @Serializable
    private data class ImageResponse(val response: Response)
    @Serializable
    private data class Response(val header: Header, val body: Body? = null)
    @Serializable
    private data class Header(val resultCode: String)
    @Serializable
    private data class Body(val items: Items? = null)
    @Serializable
    private data class Items(val item: List<Photo> = emptyList())
    @Serializable
    private data class Photo(val contentId: String, val imageUrl: String)
}
