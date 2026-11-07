package com.nangmanroad.server

import com.nangmanroad.core.sync.Api
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.io.File

/**
 * Development sync server. `sh gradlew :server:run` from the repository root; emulators reach it at
 * http://10.0.2.2:8080. PORT and NANGMANROAD_DATA override the port and the data folder.
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: Api.DEFAULT_PORT
    val dataDir = File(System.getenv("NANGMANROAD_DATA") ?: "data").absoluteFile
    val store = SyncStore(dataDir)
    println("낭만로드 sync server: http://localhost:$port (data: $dataDir)")
    embeddedServer(CIO, port = port, host = "0.0.0.0") { syncModule(store) }.start(wait = true)
}
