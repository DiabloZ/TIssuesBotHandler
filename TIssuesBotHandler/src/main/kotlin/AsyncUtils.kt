package suhov.vitaly

import kotlinx.coroutines.*
import java.net.InetAddress
import kotlin.time.measureTime

object AsyncUtils {
	private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
		runBot()
	}
	val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

	private const val ERR_TRIES = 10
	suspend fun runPing(whenError: suspend () -> Unit) {
		val host = "rambler.ru"
		var errCounter = 0

		var count = 0
		while (true){
			count++
			val time = measureTime {
				val isReachable = kotlin.runCatching {
					InetAddress.getAllByName(host).first().isReachable(0)
				}
				if (isReachable.isFailure){
					errCounter++
				}
			}

			//Logger.printResult("!!! time - $time errCounter - $errCounter!!! counter - $count")
			delay(1000)

			if (errCounter == ERR_TRIES){
				whenError.invoke()
				Logger.printResult("Restart because errors - $errCounter")
				count = 0
				errCounter = 0
			}
			if (count == ERR_TRIES){
				count = 0
				errCounter = 0
			}
		}
	}
}