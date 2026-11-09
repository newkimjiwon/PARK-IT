package com.nangmanroad.core.sync

import com.nangmanroad.core.data.Spot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A failed sync call, with a message that can be shown to the user as is. */
class SyncException(val status: Int?, message: String) : Exception(message)

/** Thin HTTP client for the sync server; see [Api] for the endpoints. */
class SyncClient(
    private val baseUrl: String,
    private val token: () -> String?,
    engine: HttpClientEngine = OkHttp.create {
        config {
            connectTimeout(5, TimeUnit.SECONDS)
            readTimeout(20, TimeUnit.SECONDS)
        }
    },
) {

    private val http = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(Api.json) }
    }

    suspend fun signIn(nickname: String, password: String): Session =
        call(HttpMethod.Post, "/api/session", authorized = false) { jsonBody(SignInRequest(nickname, password)) }.body()

    suspend fun signOut() {
        call(HttpMethod.Delete, "/api/session")
    }

    suspend fun state(): UserState = call(HttpMethod.Get, "/api/state").body()

    suspend fun toggle(list: UserList, id: String, on: Boolean): UserState =
        call(HttpMethod.Post, "/api/state/toggle") { jsonBody(ToggleRequest(list, id, on)) }.body()

    suspend fun sendToCar(request: SendRequest): UserState = call(HttpMethod.Post, "/api/car-queue") { jsonBody(request) }.body()

    suspend fun removeFromCar(id: String): UserState = call(HttpMethod.Delete, "/api/car-queue/$id").body()

    suspend fun sendToPhone(request: SendRequest): UserState = call(HttpMethod.Post, "/api/phone-inbox") { jsonBody(request) }.body()

    suspend fun removeFromPhone(id: String): UserState = call(HttpMethod.Delete, "/api/phone-inbox/$id").body()

    suspend fun spots(): List<Spot> = call(HttpMethod.Get, "/api/spots").body<List<Spot>>().map(::resolvePhotos)

    suspend fun createPost(post: NewPost): Spot = resolvePhotos(call(HttpMethod.Post, "/api/spots") { jsonBody(post) }.body())

    suspend fun addComment(spotId: String, comment: NewComment): Spot =
        resolvePhotos(call(HttpMethod.Post, "/api/spots/$spotId/comments") { jsonBody(comment) }.body())

    suspend fun addTip(spotId: String, tip: NewTip): Spot =
        resolvePhotos(call(HttpMethod.Post, "/api/spots/$spotId/tips") { jsonBody(tip) }.body())

    suspend fun reportCarCamping(spotId: String, report: CarCampingReport): Spot =
        resolvePhotos(call(HttpMethod.Post, "/api/spots/$spotId/car-camping") { jsonBody(report) }.body())

    /** Uploads a photo and returns its server path, e.g. "/photos/3f2a.jpg". */
    suspend fun uploadPhoto(file: File): String {
        val type = if (file.extension.equals("png", ignoreCase = true)) ContentType.Image.PNG else ContentType.Image.JPEG
        return call(HttpMethod.Post, "/api/photos") {
            contentType(type)
            setBody(file.readBytes())
        }.body<UploadedPhoto>().path
    }

    suspend fun startPairing(): PairingCode = call(HttpMethod.Post, "/api/pairings", authorized = false).body()

    suspend fun pairingStatus(pairingId: String): PairingStatus =
        call(HttpMethod.Get, "/api/pairings/$pairingId", authorized = false).body()

    suspend fun claimPairing(code: String): UserState = call(HttpMethod.Post, "/api/pairings/claim") { jsonBody(ClaimRequest(code)) }.body()

    /** Server photo paths become absolute URLs; sample photos already are. */
    fun resolve(path: String): String = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path

    private fun resolvePhotos(spot: Spot): Spot = spot.copy(
        photos = spot.photos.map(::resolve),
        products = spot.products.map { it.copy(imageUrl = resolve(it.imageUrl)) },
    )

    /** Sends a request and turns network errors and non-2xx answers into [SyncException]. */
    private suspend fun call(
        method: HttpMethod,
        path: String,
        authorized: Boolean = true,
        // Not "build": inside the request block that would resolve to HttpRequestBuilder.build().
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val response = try {
            http.request(baseUrl.trimEnd('/') + path) {
                this.method = method
                if (authorized) token()?.let { bearerAuth(it) }
                configure(this)
            }
        } catch (e: IOException) {
            throw SyncException(null, "서버에 연결할 수 없어요")
        }
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiError>().message }.getOrNull()?.takeIf { it.isNotBlank() }
            throw SyncException(response.status.value, message ?: "서버 오류가 났어요 (${response.status.value})")
        }
        return response
    }

    private inline fun <reified B> HttpRequestBuilder.jsonBody(value: B) {
        contentType(ContentType.Application.Json)
        setBody(value)
    }
}
