package com.nangmanroad.server

import java.io.File
import java.sql.DriverManager

/** One atomic snapshot keeps the existing sync contract and supports importing the old JSON file. */
internal class SnapshotDatabase(file: File) {
    private val url = "jdbc:sqlite:${file.absolutePath}"

    init {
        file.parentFile?.mkdirs()
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use {
                it.executeUpdate("CREATE TABLE IF NOT EXISTS sync_state (id INTEGER PRIMARY KEY CHECK (id = 1), payload TEXT NOT NULL)")
            }
        }
    }

    fun read(): String? = DriverManager.getConnection(url).use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT payload FROM sync_state WHERE id = 1").use {
                if (it.next()) it.getString(1) else null
            }
        }
    }

    fun write(payload: String) {
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement(
                "INSERT INTO sync_state (id, payload) VALUES (1, ?) ON CONFLICT(id) DO UPDATE SET payload = excluded.payload",
            ).use {
                it.setString(1, payload)
                it.executeUpdate()
            }
        }
    }
}
