package suhov.vitaly

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

private val userMap = ConcurrentHashMap<Long, UserVC>()
private val userMapCaptcha = ConcurrentHashMap<Long, Int>()

suspend fun main() = runBlocking {
	val credentials = PropsReader.getCredentials()
	val bot = TelegramBot(credentials.token)
	bot.handleUpdates()
}

@CommandHandler(["/start"])
suspend fun start(user: User, bot: TelegramBot) {
	message { "Доброго вам дня. Сейчас нужно будет подтвердить, что вы человек, давайте попробуем." }.send(user, bot)
	Logger.printResult("Пользователь начал сессию - $user")
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
	userMap[user.id]?.issueText = answeredText
	message { "Спасибо. Мы постараемся вам помочь." }.send(user, bot)
	userMap[user.id]?.let {
		message { "Введенные данные - $it" }.send(user, bot)
	}
}

fun User.toUserVC() = UserVC(
	id = id,
	isBot = isBot,
	firstName = firstName,
	lastName = lastName,
	username = "@$username",
	languageCode = languageCode,
	isPremium = isPremium,
	addedToAttachmentMenu = addedToAttachmentMenu,
	canJoinGroups = canJoinGroups,
	canReadAllGroupMessages = canReadAllGroupMessages,
	supportsInlineQueries = supportsInlineQueries,
	canConnectToBusiness = canConnectToBusiness,
	building = "",
	flatNumber = "",
	issueText = "",
	phoneNumber = ""
)

@Serializable
data class UserVC(
	val id: Long,
	val isBot: Boolean,
	val firstName: String,
	val lastName: String? = null,
	val username: String? = null,
	val languageCode: String? = null,
	val isPremium: Boolean? = null,
	val addedToAttachmentMenu: Boolean? = null,
	val canJoinGroups: Boolean? = null,
	val canReadAllGroupMessages: Boolean? = null,
	val supportsInlineQueries: Boolean? = null,
	val canConnectToBusiness: Boolean? = null,

	var fullName: String = "", //ФИО
	var building: String = "", //корпус
	var flatNumber: String = "", //номер квартиры
	var issueText: String = "", //проблема
	var phoneNumber: String = "", //телефон
	var entrance: String = "", //подъезд

)