package com.nangmanroad.server

import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.ApiError
import com.nangmanroad.core.sync.CarCampingReport
import com.nangmanroad.core.sync.ClaimRequest
import com.nangmanroad.core.sync.NewComment
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.SignInRequest
import com.nangmanroad.core.sync.ToggleRequest
import com.nangmanroad.core.sync.UploadedPhoto
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

private const val MAX_PHOTO_BYTES = 15L * 1024 * 1024

fun Application.syncModule(store: SyncStore) {
    install(ContentNegotiation) { json(Api.json) }
    install(CallLogging) {
        format { call -> "${call.response.status()}: ${call.request.httpMethod.value} ${call.request.uri} (${call.request.contentType()})" }
    }
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause -> call.respond(HttpStatusCode.Unauthorized, ApiError(cause.message.orEmpty())) }
        exception<IllegalArgumentException> { call, cause -> call.respond(HttpStatusCode.BadRequest, ApiError(cause.message.orEmpty())) }
        exception<NoSuchElementException> { call, cause -> call.respond(HttpStatusCode.NotFound, ApiError(cause.message.orEmpty())) }
        exception<TooManyAttemptsException> { call, cause -> call.respond(HttpStatusCode.TooManyRequests, ApiError(cause.message.orEmpty())) }
    }

    routing {
        get("/health") { call.respondText("ok") }

        get("/photos/{name}") {
            val name = call.parameters["name"].orEmpty()
            val file = store.photoDir?.let { File(it, name) }
            // Only plain file names; anything with a path separator is rejected.
            if (file == null || name.contains('/') || name.contains("..") || !file.isFile) {
                call.respond(HttpStatusCode.NotFound, ApiError("사진이 없어요"))
            } else {
                call.respondFile(file)
            }
        }

        route("/api") {
            post("/session") { call.respond(store.signIn(call.receive<SignInRequest>().nickname)) }
            delete("/session") {
                call.bearerToken()?.let(store::signOut)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/state") { call.respond(store.state(call.userId(store))) }
            post("/state/toggle") {
                val request = call.receive<ToggleRequest>()
                call.respond(store.toggle(call.userId(store), request.list, request.id, request.on))
            }

            post("/car-queue") { call.respond(store.sendToCar(call.userId(store), call.receive<SendRequest>())) }
            delete("/car-queue/{id}") { call.respond(store.removeFromCar(call.userId(store), call.parameters["id"].orEmpty())) }
            post("/phone-inbox") { call.respond(store.sendToPhone(call.userId(store), call.receive<SendRequest>())) }
            delete("/phone-inbox/{id}") { call.respond(store.removeFromPhone(call.userId(store), call.parameters["id"].orEmpty())) }

            get("/spots") { call.respond(store.spots(call.userId(store))) }
            post("/spots") { call.respond(store.createPost(call.userId(store), call.receive<NewPost>())) }
            post("/spots/{id}/comments") {
                call.respond(store.addComment(call.userId(store), call.parameters["id"].orEmpty(), call.receive<NewComment>()))
            }
            post("/spots/{id}/tips") {
                call.respond(store.addTip(call.userId(store), call.parameters["id"].orEmpty(), call.receive<NewTip>()))
            }
            post("/spots/{id}/car-camping") {
                call.respond(store.reportCarCamping(call.userId(store), call.parameters["id"].orEmpty(), call.receive<CarCampingReport>()))
            }

            post("/photos") {
                call.userId(store)
                require(call.request.contentType().match(ContentType.Image.Any)) { "이미지 파일만 올릴 수 있어요" }
                val length = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                require(length == null || length <= MAX_PHOTO_BYTES) { "사진은 15MB까지 올릴 수 있어요" }
                // Spool to a temporary file with a hard cap, then trust the bytes, not the header.
                val temp = store.newPhotoFile("tmp")
                try {
                    val copied = temp.outputStream().use { out -> call.receiveChannel().copyTo(out, limit = MAX_PHOTO_BYTES + 1) }
                    require(copied <= MAX_PHOTO_BYTES) { "사진은 15MB까지 올릴 수 있어요" }
                    val extension = imageExtension(temp) ?: throw IllegalArgumentException("JPEG나 PNG 사진만 올릴 수 있어요")
                    val file = store.newPhotoFile(extension)
                    require(temp.renameTo(file)) { "사진을 저장하지 못했어요" }
                    call.respond(UploadedPhoto("/photos/${file.name}"))
                } finally {
                    temp.delete()
                }
            }

            post("/pairings") { call.respond(store.startPairing()) }
            get("/pairings/{id}") { call.respond(store.pairingStatus(call.parameters["id"].orEmpty())) }
            post("/pairings/claim") { call.respond(store.claimPairing(call.userId(store), call.receive<ClaimRequest>().code)) }
        }
    }
}

/** "jpg" or "png" from the file's first bytes, or null when it is neither. */
private fun imageExtension(file: File): String? {
    val head = file.inputStream().use { input -> ByteArray(8).also { input.read(it) } }
    val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    return when {
        head.contentEquals(png) -> "png"
        head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() && head[2] == 0xFF.toByte() -> "jpg"
        else -> null
    }
}

private fun ApplicationCall.bearerToken(): String? =
    request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotEmpty() }

private fun ApplicationCall.userId(store: SyncStore): String = store.userFor(bearerToken())
