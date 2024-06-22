package suhov.vitaly

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

object PropsReader {
	fun getCredentials(): Credentials {
		val inputStream: InputStream = File("props.json").inputStream()
		val inputString = inputStream.bufferedReader().use { it.readText() }
		val credentials: Credentials = Json.decodeFromString(inputString)
		return credentials
	}
}