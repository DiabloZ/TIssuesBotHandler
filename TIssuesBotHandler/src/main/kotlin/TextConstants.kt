package suhov.vitaly

object TextConstants {
	object Commands {
		const val START = "/start"
		const val CAPTCHA = "captcha"
		const val CHECK_DATA_STEP = "checkDataStep"
		const val FULL_NAME = "fullName"
		const val ISSUE_TEXT = "issueText"
		const val BUILDING = "building"
		const val ENTRANCE = "entrance"
		const val FLAT_NUMBER = "flatNumber"
		const val PHONE_NUMBER = "phoneNumber"
		const val END = "!!!"
	}
	object Texts {
		const val START_MESSAGE = "Доброго Вам дня. Сейчас нужно будет подтвердить, что Вы человек, давайте попробуем."
		const val CAPTCHA_MESSAGE_1 = "Представьтесь полными фамилией, именем и отчеством."
		const val CAPTCHA_MESSAGE_2 = "Опишите вашу проблему - "
		const val CAPTCHA_MESSAGE_INPUT = "Вы уже заполняли данные ранее "
		const val CAPTCHA_OPTION_1 = "Хочу заполнить по другому"
		const val CAPTCHA_OPTION_2 = "Перейти к описанию проблемы"
		const val CAPTCHA_SUCCESS = "Отлично. Спасибо."
		const val CHECK_VARIANT1 = "Хочу заполнить по другому"
		const val CHECK_VARIANT2 = "Перейти к описанию проблемы"
		const val CHECK_ERR = "Нажмите на кнопки"
		const val FULLNAME_TEXT = "Укажите ФИО полностью, пожалуйста"
		const val BUILDING_RIGHT_ANSWER_1 = "1.1"
		const val BUILDING_RIGHT_ANSWER_2 = "1.2"
		const val ENTRANCE_RIGHT_ANSWER_1 = "1"
		const val ENTRANCE_RIGHT_ANSWER_2 = "2"
		const val ENTRANCE_RIGHT_ANSWER_3 = "3"
		const val ENTRANCE_RIGHT_ANSWER_4 = "4"
		const val INPUT_PHONE_NUMBER = "Введите ваш номер телефона - "
		const val PHONE_PREFIX_1 = "7"
		const val PHONE_PREFIX_2 = "8"
		const val PHONE_PREFIX_3 = "9"
		const val ISSUE_MESSAGE = "Спасибо. Мы постараемся Вам помочь."
		const val BLOCK_USER_MESSAGE = "В следующий раз повезёт. Досвидания."
		const val TRY_AGAIN_MESSAGE = "Попробуйте ещё раз"
		const val PRESS_BUTTON_MESSAGE = "Нажмите на кнопку"
		const val CHOOSE_BUILDING_MESSAGE = "Выберите корпус "
		const val CHOOSE_ENTRANCE_MESSAGE = "Выберите подъезд "
		const val FLAT_NUMBER_MESSAGE = "Введите номер квартиры корректно, числами"
		const val ERROR_FLAT_NUMBER_MESSAGE = "Введите номер вашей квартиры (числом) - "
		const val PHONE_NUMBER_MESSAGE = "Введите ваш номер телефона - "
		const val ERROR_PHONE_NUMBER_MESSAGE = "Введите телефон корректно и полностью"
		const val ENTER_PROBLEM_SHORTER = "Опишите проблему более лакончино"
		const val ENTER_PROBLEM_LOGNER = "Опишите проблему более лакончино"
		const val ENTER_PROBLEM_MESSAGE = "Опишите вашу проблему как можно более емко (до 200 символов) - "
	}
}

