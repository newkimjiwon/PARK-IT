package com.nangmanroad.core

import com.nangmanroad.core.data.Author
import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.DeviceKind
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SyncClient
import com.nangmanroad.core.sync.SyncException
import com.nangmanroad.core.sync.UserList
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val session = Session("t0k3n", Author("u-yunseul", "윤슬을따라", ""), DeviceKind.PHONE)

    private fun HttpRequestData.bodyText(): String = String((body as OutgoingContent.ByteArrayContent).bytes())

    @Test
    fun `sign-in sends a JSON body with its content type`() = runBlocking {
        var seen: HttpRequestData? = null
        val engine = MockEngine { request ->
            seen = request
            respond(Api.json.encodeToString(Session.serializer(), session), HttpStatusCode.OK, jsonHeaders)
        }
        val result = SyncClient("http://server", { null }, engine).signIn("윤슬을따라", "parkit-demo")

        val request = seen!!
        assertEquals("http://server/api/session", request.url.toString())
        assertEquals("application/json", request.body.contentType?.withoutParameters().toString())
        assertTrue(request.bodyText().contains("윤슬을따라"))
        assertEquals(null, request.headers[HttpHeaders.Authorization])
        assertEquals("u-yunseul", result.user.id)
    }

    @Test
    fun `authorized calls carry the bearer token`() = runBlocking {
        var seen: HttpRequestData? = null
        val engine = MockEngine { request ->
            seen = request
            respond("""{"user":{"id":"u-yunseul","name":"윤슬을따라","bio":""}}""", HttpStatusCode.OK, jsonHeaders)
        }
        SyncClient("http://server/", { "t0k3n" }, engine).toggle(UserList.SAVED, "s-taean", on = true)

        assertEquals("Bearer t0k3n", seen!!.headers[HttpHeaders.Authorization])
        assertTrue(seen!!.bodyText().contains("s-taean"))
    }

    @Test
    fun `server errors surface their message`() = runBlocking {
        val engine = MockEngine { respond("""{"message":"코드가 맞지 않아요"}""", HttpStatusCode.NotFound, jsonHeaders) }
        val error = runCatching { SyncClient("http://server", { "t" }, engine).claimPairing("000000") }.exceptionOrNull()

        assertTrue(error is SyncException)
        assertEquals(404, (error as SyncException).status)
        assertEquals("코드가 맞지 않아요", error.message)
    }

    @Test
    fun `server photo paths become absolute`() {
        val client = SyncClient("http://10.0.2.2:8080", { null }, MockEngine { respond("") })
        assertEquals("http://10.0.2.2:8080/photos/a.jpg", client.resolve("/photos/a.jpg"))
        assertEquals("https://images.unsplash.com/x", client.resolve("https://images.unsplash.com/x"))
    }
}
