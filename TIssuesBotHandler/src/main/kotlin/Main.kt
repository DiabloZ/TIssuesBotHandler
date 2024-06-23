package suhov.vitaly

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

private val userMap = ConcurrentHashMap<Long, UserVC>()
private val userMapCaptcha = ConcurrentHashMap<Long, Int>()
private val credentials = PropsReader.getCredentials()
private val googleSheets by lazy { GoogleSheetsService(credentials.googleSheetCredentials) }

suspend fun main() {
	restoreSession()
	setupBot()
	// val bot = TelegramBot(credentials.token)
	// bot.handleUpdates {
	// 	onCommand("/start") {
	// 		message { "Hello, what's your name?" }.send(user, bot)
	// 		bot.inputListener[user] = "conversation"
	// 	}
	// 	inputChain("conversation"){
	// 		message { "Nice to meet you, ${update.text}" }.send(update.getUser(), bot)
	// 		message { "What is your favorite food?" }.send(update.getUser(), bot)
	// 	}.breakIf({ update.text == "peanut butter" }) { // chain break condition
	// 		message { "Oh, too bad, I'm allergic to it." }.send(user!!, bot)
	// 		bot.inputListener[user!!] = "conversation"
	// 		// action that will be applied when match
	// 	}.andThen {
	// 		message { "Good" }.send(user!!, bot)
	// 		bot.inputListener[user!!] = "None"
	// 		// next input point if break condition doesn't match
	//
	// 	}
	// }

	//bot.handleUpdates()
}

suspend fun setupBot() {
	val bot = TelegramBot(credentials.token)
	bot.handleUpdates {
		onCommand("/start") {
			message { "Доброго вам дня. Сейчас нужно будет подтвердить, что вы человек, давайте попробуем." }.send(user, bot)
			val enc = Json.encodeToString(user)
			val us = (Json.parseToJsonElement(enc) as Map<*, *>).toMap()
			var userLogMsg = "Пользователь ${user.id } начал сессию -"
			us.forEach { (t, u) ->
				userLogMsg += "\n$t - $u"
			}
			firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = true)
		}

		inputChain("captcha"){
			val user = user ?: return@inputChain
			val answeredText = update.text.toIntOrNull()
			val num = userMapCaptcha[user.id]
			if (answeredText == num) {
				message { "Отлично. Спасибо." }.replyKeyboardRemove().send(user, bot)
				if (userMap.containsKey(user.id)){
					message("Вы уже заполняли данные ранее ").replyKeyboardMarkup {
						options {
							+ "Хочу заполнить по другому"
							+ "Перейти к описанию проблемы"
						}
					}.send(user, bot)
					bot.inputListener[user] = "checkDataStep"
				} else {
					userMap[user.id] = user.toUserVC()
					message { "Представьтесь полным фимилией именем и очеством" }.send(user, bot)
					bot.inputListener[user] = "fullName"
				}

			} else {
				firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = false)
			}
		}

		inputChain("checkDataStep"){
			val user = user ?: return@inputChain
			val text = update.text
			when (text) {
				"Хочу заполнить по другому" -> {
					message { "Представьтесь полным фимилией именем и очеством" }.replyKeyboardRemove().send(user, bot)
					bot.inputListener[user] = "fullName"
				}
				"Перейти к описанию проблемы" -> {
					message { "Опишите вашу проблему - " }.replyKeyboardRemove().send(user, bot)
					bot.inputListener[user] = "issueText"
				}
			}

		}.breakIf({update.text != "Хочу заполнить по другому" && update.text != "Перейти к описанию проблемы"}) {
			val user = user ?: return@breakIf
			message { "Нажмите на кнопки" }.send(user, bot)
			bot.inputListener[user] = "captcha"
		}

		inputChain("fullName") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			userMap[user.id]?.fullName = answeredText
			message { "Введите ваш корпус - " }.send(user, bot)
			bot.inputListener[user] = "building"
		}

		inputChain("building") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			userMap[user.id]?.building = answeredText
			message { "Введите ваш подъезд - " }.send(user, bot)
			bot.inputListener[user] = "entrance"
		}
		inputChain("entrance") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			userMap[user.id]?.entrance = answeredText
			message { "Введите номер вашей квартиры - " }.send(user, bot)
			bot.inputListener[user] = "flatNumber"
		}
		inputChain("flatNumber") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			userMap[user.id]?.flatNumber = answeredText
			message { "Введите ваш номер телефона - " }.send(user, bot)
			bot.inputListener[user] = "phoneNumber"
		}
		inputChain("phoneNumber") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			userMap[user.id]?.phoneNumber = answeredText
			message { "Опишите вашу проблему - " }.send(user, bot)
			bot.inputListener[user] = "issueText"
		}
		inputChain("issueText") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			message { "Спасибо. Мы постараемся вам помочь." }.send(user, bot)

			val userVC = userMap[user.id]?.apply {
				issueText = answeredText
			} ?: return@inputChain
			val enc = Json.encodeToString(userVC)
			val us = (Json.parseToJsonElement(enc) as Map<*, *>).toMap()

			googleSheets.writeNewRow(
				writeArray = mutableListOf(
					mutableListOf<Any>().apply {
						us.values.forEach {
							if (it != null){
								add(it.toString().replace("\"", ""))
							}
						}
					},
				)
			)
			Logger.printResult("Пользователь ${userVC.id} успешно заполнил форму ")
		}
	}
}

//3. Фильтры и валидации

suspend fun firstQuestionTextRepeat(user: User, bot: TelegramBot, isFirstTime: Boolean = false) {
	if (!isFirstTime){
		message { "Попробуйте ещё раз" }.send(user, bot)
	}
	val randomOne = Random.nextInt(1..10)
	val randomTwo = Random.nextInt(1..10)
	message { "Сколько будет $randomOne + $randomTwo = ?" }.send(user, bot)
	val rightAnswerNumber = Random.nextInt(0..5)
	val rightAnswer = randomOne + randomTwo
	val setAnswers = mutableSetOf<Int>()
	while (setAnswers.size != 10){
		setAnswers.add(rightAnswerNumber + Random.nextInt(0..10))
	}
	val setList = setAnswers.toList()
	message("Вы уже заполняли данные ранее ").replyKeyboardMarkup {
		options {
			for (i in 0 until 6) {
				if (i % 2 == 0){
					br()
				}
				if (i == rightAnswerNumber) {
					+ (rightAnswer).toString()
				} else {
					+ (setList.getOrNull(i) ?: Random.nextInt(0..10)).toString()
				}
			}
		}
	}.send(user, bot)

	userMapCaptcha[user.id] = rightAnswer
	bot.inputListener[user] = "captcha"
}

fun User.toUserVC() = UserVC(
	id = id.toString(),
	isBot = isBot.getStringPresentation(),
	firstName = firstName,
	lastName = lastName ?: "",
	username = "@$username",
	//languageCode = languageCode ?: "",
	isPremium = isPremium.getStringPresentation(),
	//addedToAttachmentMenu = addedToAttachmentMenu.getStringPresentation(),
	canJoinGroups = canJoinGroups.getStringPresentation(),
	//canReadAllGroupMessages = canReadAllGroupMessages.getStringPresentation(),
	//supportsInlineQueries = supportsInlineQueries.getStringPresentation(),
	//canConnectToBusiness = canConnectToBusiness.getStringPresentation(),
)

fun Boolean?.getStringPresentation() = if (this == true) "Да" else "Нет"


@Serializable
data class UserVC(
	val id: String,
	val isBot: String,
	val firstName: String,
	val lastName: String,
	val username: String,
	//val languageCode: String,
	val isPremium: String,
	//val addedToAttachmentMenu: String,
	val canJoinGroups: String,
	//val canReadAllGroupMessages: String,
	//val supportsInlineQueries: String,
	//val canConnectToBusiness: String,
	var fullName: String = "", //ФИО
	var building: String = "", //корпус
	var flatNumber: String = "", //номер квартиры
	var issueText: String = "", //проблема
	var phoneNumber: String = "", //телефон
	var entrance: String = "", //подъезд

)

fun restoreSession() {
	val restoredData = googleSheets.readSheet().getValues()
	val firstElement = restoredData.firstOrNull()
	restoredData.mapNotNull {
		if (firstElement == it){
			null
		} else {
			it.toUserVc()
		}
	}.reversed()
		.distinctBy { it.id }
		.forEach { userMap[it.id.toLongOrNull() ?: Long.MIN_VALUE] = it }
}

private fun <E> MutableList<E>.toUserVc(): UserVC = UserVC(
	id =            get(0).toString(),
	isBot =         get(1).toString(),
	firstName =     get(2).toString(),
	lastName =      get(3).toString(),
	username =      get(4).toString(),
	isPremium =     get(5).toString(),
	canJoinGroups = get(6).toString(),
	fullName =      get(7).toString(),
	building =      get(8).toString(),
	flatNumber =    get(9).toString(),
	issueText =     get(10).toString(),
	phoneNumber =   get(11).toString(),
	entrance =      get(12).toString()

)