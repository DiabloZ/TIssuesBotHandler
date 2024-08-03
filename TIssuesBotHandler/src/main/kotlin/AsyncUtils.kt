package suhov.vitaly

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import kotlin.time.measureTime

object AsyncUtils {
	private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
		runBot()
	}
	val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

	private const val ERR_TRIES = 5
	suspend fun runPing(
		whenError: suspend () -> Unit,
		doAfterOn: suspend () -> Unit
	) {
		val host = URI.create("https://www.timeserver.ru").toURL()
		var errCounter = 0
		var wasError = false
		var count = 0
		while (true){
			count++
			val time = measureTime {
				val isReachable = kotlin.runCatching {
					val result = host.isReachable()
					if (!result) {
						error("host is not reachable")
					}
				}
				if (isReachable.isFailure){
					errCounter++
				}
			}

			//Logger.printResult("!!! time - $time errCounter - $errCounter!!! counter - $count")
			delay(1000)

			if (errCounter == ERR_TRIES){
				whenError.invoke()
				count = 0
				errCounter = 0
				if (!wasError){
					wasError = true
					Logger.printResult("Stopped because was errors")
				}
			}
			if (count == ERR_TRIES){
				count = 0
				errCounter = 0
				if (wasError) {
					wasError = false
					doAfterOn.invoke()
					Logger.printResult("Restart because was errors")
				}
			}
		}
	}

	private fun URL.isReachable(timeout: Int = 100): Boolean {
		return try {
			val connection = this.openConnection() as HttpURLConnection
			connection.connectTimeout = timeout
			connection.readTimeout = timeout
			connection.requestMethod = "HEAD"
			val responseCode = connection.responseCode
			responseCode in 200..299
		} catch (e: Exception) {
			false
		}
	}
}