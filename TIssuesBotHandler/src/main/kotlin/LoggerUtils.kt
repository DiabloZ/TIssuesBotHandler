package suhov.vitaly

object LoggerUtils {

	fun startSession(userID: Long, userData: Map<Any?, Any?>){
		var userLogMsg = "Пользователь $userID начал сессию -"
		userData.forEach { (t, u) ->
			userLogMsg += "\n$t - $u"
		}
		Logger.printResult(userLogMsg)
	}

	fun completeIssueText(userID: String){
		Logger.printResult("Пользователь $userID успешно заполнил форму ")
	}

	fun completeCaptcha(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел каптчу"
		)
	}

	fun completeCheckData(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел проверку даты"
		)
	}

	fun completeFullName(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел заполнение фио"
		)
	}

	fun completeBuilding(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел заполнение постройки"
		)
	}

	fun completeEntrance(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел заполнение постройки"
		)
	}

	fun completeFlat(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел заполнение квартиры"
		)
	}

	fun completePhoneNumber(userID: Long) {
		Logger.printResult(
			"Пользователь $userID прошел заполнение телефона"
		)
	}
}