package suhov.vitaly

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator

object Logger {

	private val logger = LogManager.getRootLogger().apply {
		Configurator.setAllLevels(this.name, Level.ALL)
	}
	fun printText(any: Any, text: Any){
		logger.info("Function - $any - $text")
	}

	fun printResult(text: Any){
		logger.info("$text")
	}
}