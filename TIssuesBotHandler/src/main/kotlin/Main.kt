package suhov.vitaly

import kotlinx.coroutines.*
import suhov.vitaly.AsyncUtils.runPing
import suhov.vitaly.AsyncUtils.scope

fun main() = runBlocking {
	runBot()
	while (true){
		runPing(
			whenError = {
				runJob?.cancel()
			},
			doAfterOn = {
				runBot()
			}
		)
	}
}
var runJob: Job? = null

fun runBot(){
	runJob?.cancel()
	runJob = scope.launch {
		restoreSession()
		setupBot()
	}
}
