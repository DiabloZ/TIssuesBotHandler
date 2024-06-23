package suhov.vitaly

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

private val userMap = ConcurrentHashMap<Long, UserVC>()
private val userMapCaptcha = ConcurrentHashMap<Long, Int>()
private val credentials = PropsReader.getCredentials()
private val googleSheets by lazy { GoogleSheetsService(credentials.googleSheetCredentials) }

fun main() = runBlocking {
	restoreSession()
	setupBot()
}

suspend fun setupBot() {
	val bot = TelegramBot(credentials.token)
	bot.handleUpdates {
		onCommand("/start") {
			counterMap[user.id] = 0
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
				counterMap[user.id] = 0
				message { "Отлично. Спасибо." }.send(user, bot)
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
					message { "Представьтесь полным фимилией именем и очеством" }.replyKeyboardRemove().send(user, bot)
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

			if (update.text.length < 5){
				message { "Укажите ФИО полностью, пожалуйста." }.send(user, bot)
				bot.inputListener[user] = "fullName"
				return@inputChain
			}

			userMap[user.id]?.fullName = answeredText
			chooseBuilding(user = user, bot = bot, isFirstTime = true)
		}

		inputChain("building") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (answeredText != "1.1" && answeredText != "1.2"){
				chooseBuilding(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.building = answeredText
			chooseEntrance(user = user, bot = bot, isFirstTime = true)
		}

		inputChain("entrance") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (answeredText != "1" && answeredText != "2" && answeredText != "3" && answeredText != "4"){
				chooseEntrance(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.entrance = answeredText
			enterFlatNumber(user = user, bot = bot, isFirstTime = true)
		}

		inputChain("flatNumber") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			val answeredTextString = answeredText.toIntOrNull()
			if (answeredTextString == null || answeredTextString <= 0 || answeredTextString >= 599){
				enterFlatNumber(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.flatNumber = answeredText
			message { "Введите ваш номер телефона - " }.replyKeyboardRemove().send(user, bot)
			bot.inputListener[user] = "phoneNumber"
		}

		inputChain("phoneNumber") {
			val user = user ?: return@inputChain
			val answeredText = update.text.clearPhoneNumber()
			val isNotPhone = !answeredText.startsWith("7") && !answeredText.startsWith("8") && !answeredText.startsWith("9")
			if (answeredText.length !in 10..11 || isNotPhone) {
				enterPhoneNumber(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.phoneNumber = answeredText
			enterProblem(user = user, bot = bot, isFirstTime = true)
		}

		inputChain("issueText") {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (answeredText.length > 200 || answeredText.length < 10) {
				enterProblem(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}

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
			bot.inputListener[user] = "!!!"
		}
	}
}

val counterMap = ConcurrentHashMap<Long, Int>()

suspend fun firstQuestionTextRepeat(user: User, bot: TelegramBot, isFirstTime: Boolean = false) {
	val counter = counterMap[user.id] ?: 0

	if (counter == 3) {
		counterMap[user.id] = 0
		message { "В следующий раз повезёт. Досвидания." }.replyKeyboardRemove().send(user, bot)
		return
	}

	if (!isFirstTime){
		message { "Попробуйте ещё раз" }.send(user, bot)
	}
	val randomOne = Random.nextInt(1..10)
	val randomTwo = Random.nextInt(1..10)
	val rightAnswerNumber = Random.nextInt(0..5)
	val rightAnswer = randomOne + randomTwo
	val setAnswers = mutableSetOf<Int>()
	while (setAnswers.size != 10){
		setAnswers.add(rightAnswerNumber + Random.nextInt(0..10))
	}
	val setList = setAnswers.toList()
	message("Сколько будет $randomOne + $randomTwo = ?").replyKeyboardMarkup {
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

	counterMap[user.id] = counter + 1
	userMapCaptcha[user.id] = rightAnswer
	bot.inputListener[user] = "captcha"
}

suspend fun chooseBuilding(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { "Нажмите на кнопку" }.send(user, bot)
	}
	message("Выберите корпус ").replyKeyboardMarkup {
		options {
			+ "1.1"
			+ "1.2"
		}
	}.send(user, bot)
	bot.inputListener[user] = "building"
}

suspend fun chooseEntrance(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { "Нажмите на кнопку" }.send(user, bot)
	}
	message("Выберите подъезд ").replyKeyboardMarkup {
		options {
			+ "1"
			+ "2"
			br()
			+ "3"
			+ "4"
		}
	}.send(user, bot)
	bot.inputListener[user] = "entrance"
}

suspend fun enterFlatNumber(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { "Введите номер квартиры корректно и числами" }.send(user, bot)
	} else {
		message("Введите номер вашей квартиры (числом) - ").replyKeyboardRemove().send(user, bot)
	}

	bot.inputListener[user] = "flatNumber"
}

suspend fun enterPhoneNumber(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { "Введите телефон корректно и полностью" }.send(user, bot)
	} else {
		message("Введите ваш номер телефона - ").send(user, bot)
	}

	bot.inputListener[user] = "phoneNumber"
}

suspend fun enterProblem(user: User, bot: TelegramBot, length: Int = 0, isFirstTime: Boolean = false){
	when{
		!isFirstTime && length > 200 -> message { "Опишите проблему более лакончино" }.send(user, bot)
		!isFirstTime && length < 10 -> message { "Опишите проблему более подробно" }.send(user, bot)
		else -> {message { "Опишите вашу проблему как можно более емко (до 200 символов)- " }.send(user, bot)}
	}

	bot.inputListener[user] = "issueText"
}

private val regexDigital = Regex("(\\d+(?:\\.\\d+)?)")
fun String.clearPhoneNumber(): String {
	return regexDigital.findAll(this)
		.toList()
		.mapNotNull { it.groupValues.firstOrNull() }.joinToString(
			separator = "",
			prefix = "",
			postfix = "",
			truncated = "",
		)
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

val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm")

@Serializable
data class UserVC @OptIn(ExperimentalSerializationApi::class) constructor(
	val id: String,
	val isBot: String,
	val lastName: String,
	val firstName: String,
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
	@EncodeDefault val data: String = formatter.format(Date()),
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
	lastName =      get(2).toString(),
	firstName =     get(3).toString(),
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