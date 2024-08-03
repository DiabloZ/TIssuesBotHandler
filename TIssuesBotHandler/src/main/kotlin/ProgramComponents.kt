package suhov.vitaly

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import suhov.vitaly.TextConstants.Texts.BUILDING_RIGHT_ANSWER_1
import suhov.vitaly.TextConstants.Texts.BUILDING_RIGHT_ANSWER_2
import suhov.vitaly.TextConstants.Texts.CHECK_ERR
import suhov.vitaly.TextConstants.Texts.CHECK_VARIANT1
import suhov.vitaly.TextConstants.Texts.CHECK_VARIANT2
import suhov.vitaly.TextConstants.Commands.BUILDING
import suhov.vitaly.TextConstants.Commands.CAPTCHA
import suhov.vitaly.TextConstants.Commands.CHECK_DATA_STEP
import suhov.vitaly.TextConstants.Commands.END
import suhov.vitaly.TextConstants.Commands.ENTRANCE
import suhov.vitaly.TextConstants.Commands.FLAT_NUMBER
import suhov.vitaly.TextConstants.Commands.FULL_NAME
import suhov.vitaly.TextConstants.Commands.ISSUE_TEXT
import suhov.vitaly.TextConstants.Commands.START
import suhov.vitaly.TextConstants.Texts.ENTRANCE_RIGHT_ANSWER_1
import suhov.vitaly.TextConstants.Texts.ENTRANCE_RIGHT_ANSWER_2
import suhov.vitaly.TextConstants.Texts.ENTRANCE_RIGHT_ANSWER_3
import suhov.vitaly.TextConstants.Texts.ENTRANCE_RIGHT_ANSWER_4
import suhov.vitaly.TextConstants.Texts.FULLNAME_TEXT
import suhov.vitaly.TextConstants.Texts.START_MESSAGE
import suhov.vitaly.TextConstants.Texts.CAPTCHA_MESSAGE_1
import suhov.vitaly.TextConstants.Texts.CAPTCHA_MESSAGE_2
import suhov.vitaly.TextConstants.Texts.CAPTCHA_MESSAGE_INPUT
import suhov.vitaly.TextConstants.Texts.CAPTCHA_OPTION_1
import suhov.vitaly.TextConstants.Texts.CAPTCHA_OPTION_2
import suhov.vitaly.TextConstants.Texts.CAPTCHA_SUCCESS
import suhov.vitaly.TextConstants.Commands.PHONE_NUMBER
import suhov.vitaly.TextConstants.Texts.BLOCK_USER_MESSAGE
import suhov.vitaly.TextConstants.Texts.CHOOSE_BUILDING_MESSAGE
import suhov.vitaly.TextConstants.Texts.CHOOSE_ENTRANCE_MESSAGE
import suhov.vitaly.TextConstants.Texts.ENTER_PROBLEM_LOGNER
import suhov.vitaly.TextConstants.Texts.ENTER_PROBLEM_MESSAGE
import suhov.vitaly.TextConstants.Texts.ENTER_PROBLEM_SHORTER
import suhov.vitaly.TextConstants.Texts.FLAT_NUMBER_MESSAGE
import suhov.vitaly.TextConstants.Texts.ERROR_FLAT_NUMBER_MESSAGE
import suhov.vitaly.TextConstants.Texts.PHONE_NUMBER_MESSAGE
import suhov.vitaly.TextConstants.Texts.ERROR_PHONE_NUMBER_MESSAGE
import suhov.vitaly.TextConstants.Texts.INPUT_PHONE_NUMBER
import suhov.vitaly.TextConstants.Texts.ISSUE_MESSAGE
import suhov.vitaly.TextConstants.Texts.PHONE_PREFIX_1
import suhov.vitaly.TextConstants.Texts.PHONE_PREFIX_2
import suhov.vitaly.TextConstants.Texts.PHONE_PREFIX_3
import suhov.vitaly.TextConstants.Texts.PRESS_BUTTON_MESSAGE
import suhov.vitaly.TextConstants.Texts.TRY_AGAIN_MESSAGE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

private val userMap = ConcurrentHashMap<Long, UserVC>()
private val userMapCaptcha = ConcurrentHashMap<Long, Int>()
private val credentials = PropsReader.getCredentials()
private val googleSheets by lazy { GoogleSheetsService(credentials.googleSheetCredentials) }

suspend fun setupBot() {
	LoggerUtils.startBot()
	val bot = TelegramBot(credentials.token)
	bot.handleUpdates {
		onCommand(START) {
			counterMap[user.id] = 0
			message { START_MESSAGE }.send(user, bot)
			val enc = Json.encodeToString(user)
			val us = (Json.parseToJsonElement(enc) as Map<*, *>).toMap()
			LoggerUtils.startSession(user.id, us)
			firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = true)
		}

		inputChain(CAPTCHA){
			val user = user ?: return@inputChain
			val answeredText = update.text.toIntOrNull()
			val num = userMapCaptcha[user.id]
			if (answeredText == num) {
				counterMap[user.id] = 0
				message { CAPTCHA_SUCCESS }.send(user, bot)
				if (userMap.containsKey(user.id)){
					message(CAPTCHA_MESSAGE_INPUT).replyKeyboardMarkup {
						options {
							+ CAPTCHA_OPTION_1
							+ CAPTCHA_OPTION_2
						}
					}.send(user, bot)
					bot.inputListener[user] = CHECK_DATA_STEP
				} else {
					userMap[user.id] = user.toUserVC()
					message { CAPTCHA_MESSAGE_1 }.replyKeyboardRemove().send(user, bot)
					bot.inputListener[user] = FULL_NAME
				}
				LoggerUtils.completeCaptcha(user.id)
			} else {
				firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = false)
			}
		}

		inputChain(CHECK_DATA_STEP){
			val user = user ?: return@inputChain
			val text = update.text
			when (text) {
				CHECK_VARIANT1 -> {
					message { CAPTCHA_MESSAGE_1 }.replyKeyboardRemove().send(user, bot)
					bot.inputListener[user] = FULL_NAME
				}
				CHECK_VARIANT2 -> {
					message { CAPTCHA_MESSAGE_2 }.replyKeyboardRemove().send(user, bot)
					bot.inputListener[user] = ISSUE_TEXT
				}
			}
			LoggerUtils.completeCheckData(user.id)
		}.breakIf({update.text != CHECK_VARIANT1 && update.text != CHECK_VARIANT2}) {
			val user = user ?: return@breakIf
			message { CHECK_ERR }.send(user, bot)
			bot.inputListener[user] = CAPTCHA
		}

		inputChain(FULL_NAME) {
			val user = user ?: return@inputChain
			val answeredText = update.text

			if (update.text.length < 5){
				message { FULLNAME_TEXT }.send(user, bot)
				bot.inputListener[user] = FULL_NAME
				return@inputChain
			}

			userMap[user.id]?.fullName = answeredText
			chooseBuilding(user = user, bot = bot, isFirstTime = true)
			LoggerUtils.completeFullName(user.id)
		}

		inputChain(BUILDING) {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (answeredText != BUILDING_RIGHT_ANSWER_1 && answeredText != BUILDING_RIGHT_ANSWER_2){
				chooseBuilding(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.building = answeredText
			chooseEntrance(user = user, bot = bot, isFirstTime = true)
			LoggerUtils.completeBuilding(user.id)
		}

		inputChain(ENTRANCE) {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (
				answeredText != ENTRANCE_RIGHT_ANSWER_1 &&
				answeredText != ENTRANCE_RIGHT_ANSWER_2 &&
				answeredText != ENTRANCE_RIGHT_ANSWER_3 &&
				answeredText != ENTRANCE_RIGHT_ANSWER_4
			){
				chooseEntrance(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.entrance = answeredText
			enterFlatNumber(user = user, bot = bot, isFirstTime = true)
			LoggerUtils.completeEntrance(user.id)
		}

		inputChain(FLAT_NUMBER) {
			val user = user ?: return@inputChain
			val answeredText = update.text
			val answeredTextString = answeredText.toIntOrNull()
			if (answeredTextString == null || answeredTextString <= 0 || answeredTextString >= 599){
				enterFlatNumber(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.flatNumber = answeredText
			message { INPUT_PHONE_NUMBER }.replyKeyboardRemove().send(user, bot)
			bot.inputListener[user] = PHONE_NUMBER
			LoggerUtils.completeFlat(user.id)
		}

		inputChain(PHONE_NUMBER) {
			val user = user ?: return@inputChain
			val answeredText = update.text.clearPhoneNumber()
			val isNotPhone = !answeredText.startsWith(PHONE_PREFIX_1) &&
				!answeredText.startsWith(PHONE_PREFIX_2) &&
				!answeredText.startsWith(PHONE_PREFIX_3)
			if (answeredText.length !in 10..11 || isNotPhone) {
				enterPhoneNumber(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}
			userMap[user.id]?.phoneNumber = answeredText
			enterProblem(user = user, bot = bot, isFirstTime = true)
			LoggerUtils.completePhoneNumber(user.id)
		}

		inputChain(ISSUE_TEXT) {
			val user = user ?: return@inputChain
			val answeredText = update.text
			if (answeredText.length > 200 || answeredText.length < 10) {
				enterProblem(user = user, bot = bot, isFirstTime = false)
				return@inputChain
			}

			message { ISSUE_MESSAGE }.send(user, bot)

			val userVC = userMap[user.id]?.apply {
				issueText = answeredText
				data = formatter.format(Date())
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
			LoggerUtils.completeIssueText(userVC.id)
			bot.inputListener[user] = END
		}
	}
}

val counterMap = ConcurrentHashMap<Long, Int>()

suspend fun firstQuestionTextRepeat(user: User, bot: TelegramBot, isFirstTime: Boolean = false) {
	val counter = counterMap[user.id] ?: 0

	if (counter == 3) {
		counterMap[user.id] = 0
		message { BLOCK_USER_MESSAGE }.replyKeyboardRemove().send(user, bot)
		return
	}

	if (!isFirstTime){
		message { TRY_AGAIN_MESSAGE }.send(user, bot)
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

	message(TextUtils.firstQuestionRepeatText(randomOne, randomTwo)).replyKeyboardMarkup {
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
	bot.inputListener[user] = CAPTCHA
}

suspend fun chooseBuilding(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { PRESS_BUTTON_MESSAGE }.send(user, bot)
	}
	message(CHOOSE_BUILDING_MESSAGE).replyKeyboardMarkup {
		options {
			+ BUILDING_RIGHT_ANSWER_1
			+ BUILDING_RIGHT_ANSWER_2
		}
	}.send(user, bot)
	bot.inputListener[user] = BUILDING
}

suspend fun chooseEntrance(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { PRESS_BUTTON_MESSAGE }.send(user, bot)
	}
	message(CHOOSE_ENTRANCE_MESSAGE).replyKeyboardMarkup {
		options {
			+ ENTRANCE_RIGHT_ANSWER_1
			+ ENTRANCE_RIGHT_ANSWER_2
			br()
			+ ENTRANCE_RIGHT_ANSWER_3
			+ ENTRANCE_RIGHT_ANSWER_4
		}
	}.send(user, bot)
	bot.inputListener[user] = ENTRANCE
}

suspend fun enterFlatNumber(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { FLAT_NUMBER_MESSAGE }.send(user, bot)
	} else {
		message(ERROR_FLAT_NUMBER_MESSAGE).replyKeyboardRemove().send(user, bot)
	}

	bot.inputListener[user] = FLAT_NUMBER
}

suspend fun enterPhoneNumber(user: User, bot: TelegramBot, isFirstTime: Boolean = false){
	if (!isFirstTime){
		message { ERROR_PHONE_NUMBER_MESSAGE }.send(user, bot)
	} else {
		message(PHONE_NUMBER_MESSAGE).send(user, bot)
	}

	bot.inputListener[user] = PHONE_NUMBER
}

suspend fun enterProblem(user: User, bot: TelegramBot, length: Int = 0, isFirstTime: Boolean = false){
	when{
		!isFirstTime && length > 200 -> message { ENTER_PROBLEM_SHORTER }.send(user, bot)
		!isFirstTime && length < 10 -> message { ENTER_PROBLEM_LOGNER }.send(user, bot)
		else -> {
			message { ENTER_PROBLEM_MESSAGE }.send(user, bot)}
	}

	bot.inputListener[user] = ISSUE_TEXT
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
	@EncodeDefault var data: String = formatter.format(Date()),
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