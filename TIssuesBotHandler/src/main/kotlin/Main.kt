package suhov.vitaly

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
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
	val bot = TelegramBot(credentials.token)
	bot.handleUpdates()
}

@CommandHandler(["/start"])
suspend fun start(user: User, bot: TelegramBot) {
	message { "Доброго вам дня. Сейчас нужно будет подтвердить, что вы человек, давайте попробуем." }.send(user, bot)
	val enc = Json.encodeToString(user)
	val us = (Json.parseToJsonElement(enc) as Map<*, *>).toMap()
	var userLogMsg = "Пользователь ${user.id } начал сессию -"
	us.forEach { (t, u) ->
		userLogMsg += "\n$t - $u"
	}
	Logger.printResult(userLogMsg)
	firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = true)
}



suspend fun firstQuestionTextRepeat(user: User, bot: TelegramBot, isFirstTime: Boolean = false) {
	if (!isFirstTime){
		message { "Попробуйте ещё раз" }.send(user, bot)
	}
	val randomOne = Random.nextInt(1..10)
	val randomTwo = Random.nextInt(1..10)
	message { "Сколько будет $randomOne + $randomTwo = ?" }.send(user, bot)

	userMapCaptcha[user.id] = randomOne + randomTwo
	bot.inputListener[user] = "captcha"
}

@InputHandler(["captcha"])
suspend fun startConversation(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text.toIntOrNull()
	val num = userMapCaptcha[user.id]
	if (answeredText == num) {
		userMap[user.id] = user.toUserVC()
		message { "Отлично. Спасибо." }.send(user, bot)
		message { "Представьтесь полным фимилией именем и очеством" }.send(user, bot)
		bot.inputListener[user] = "fullName"
	} else {
		firstQuestionTextRepeat(user = user, bot = bot, isFirstTime = false)
	}
}

@InputHandler(["fullName"])
suspend fun fullName(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	userMap[user.id]?.fullName = answeredText
	message { "Введите ваш корпус - " }.send(user, bot)
	bot.inputListener[user] = "building"
}

@InputHandler(["building"])
suspend fun building(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	userMap[user.id]?.building = answeredText
	message { "Введите ваш подъезд - " }.send(user, bot)
	bot.inputListener[user] = "entrance"
}

@InputHandler(["entrance"])
suspend fun entrance(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	userMap[user.id]?.entrance = answeredText
	message { "Введите номер вашей квартиры - " }.send(user, bot)
	bot.inputListener[user] = "flatNumber"
}

@InputHandler(["flatNumber"])
suspend fun flatNumber(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	userMap[user.id]?.flatNumber = answeredText
	message { "Введите ваш номер телефона - " }.send(user, bot)
	bot.inputListener[user] = "phoneNumber"
}

@InputHandler(["phoneNumber"])
suspend fun phoneNumber(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	userMap[user.id]?.phoneNumber = answeredText
	message { "Опишите вашу проблему - " }.send(user, bot)
	bot.inputListener[user] = "issueText"
}

@InputHandler(["issueText"])
suspend fun issueText(update: ProcessedUpdate, user: User, bot: TelegramBot) {
	val answeredText = update.text
	message { "Спасибо. Мы постараемся вам помочь." }.send(user, bot)

	val userVC = userMap[user.id]?.apply {
		issueText = answeredText
	} ?: return
	val enc = Json.encodeToString(userVC)
	val us = (Json.parseToJsonElement(enc) as Map<*, *>).toMap()

	googleSheets.writeNewRow(
		writeArray = mutableListOf(
			mutableListOf<Any>().apply {
				us.values.forEach {
					if (it != null){
						add(it.toString().replace("\"", ""))
						Logger.printResult(it)
					}
				}
			},
		)
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