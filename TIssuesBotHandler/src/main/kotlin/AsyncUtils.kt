package suhov.vitaly

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object AsyncUtils {
	private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
		runBot()
	}
	val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

	private const val ERR_TRIES = 10
	private const val ERR_LOSS_TEXT = "100% packet loss"
	suspend fun runPing(whenError: suspend () -> Unit) {
		val ipAddress = "8.8.8.8"
		var errCounter = 0
		var isCalled = false
		val errText = "Host Unreach"
		val command = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
			listOf("ping", "-n", ERR_TRIES.toString(), ipAddress) // For Windows
		} else {
			listOf("ping", "-c", ERR_TRIES.toString(), ipAddress) // For Unix-based systems (Linux, macOS)
		}

		withContext(Dispatchers.IO){
			try {
				val process = ProcessBuilder(command)
					.redirectErrorStream(true)
					.start()

				val reader = BufferedReader(InputStreamReader(process.inputStream))
				var line: String?
				while (reader.readLine().also { line = it } != null && !isCalled) {
					Logger.printResult(line+errCounter)
					if (line?.contains(errText) == true) {
						errCounter++
					}
					if (errCounter == ERR_TRIES) {
						isCalled = true
						whenError.invoke()
						Logger.printResult("Restart because errors - $errCounter")
					}
					if (line?.contains(ERR_LOSS_TEXT) == true){
						isCalled = true
						whenError.invoke()
						Logger.printResult("Restart because $ERR_LOSS_TEXT")
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}