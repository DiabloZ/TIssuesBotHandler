package suhov.vitaly

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class GoogleSheetsService(
	private val credentials: GoogleSheetsServiceCredentials
) {
	private fun getCredentials(): Credential {
		val fileInStream = FileInputStream(CRED_PATH)
		val clientSecrets = com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(fileInStream))
		val flow = com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow.Builder(
			GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
			.setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
			.setAccessType(ACCESS_TYPE)
			.build()
		return AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize(credentials.userID)
	}

	private val service: Sheets by lazy {
		Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getCredentials())
			.setApplicationName(credentials.applicationName)
			.build()
	}

	fun readSheet(spreadsheetId: String = credentials.sheetID, range: String = DEFAULT_READ_RANGE): ValueRange {
		return service.spreadsheets().values().get(spreadsheetId, range).execute()
	}

	@Synchronized
	fun writeNewRow(spreadsheetId: String = credentials.sheetID, range: String = DEFAULT_WRITE_RANGE, writeArray: MutableList<MutableList<Any>>) {
		service.spreadsheets().values()
			.append(spreadsheetId, range, ValueRange().setValues(writeArray))
			.setValueInputOption(VALUE_INPUT_OPTION)
			.execute()
	}

	fun updateRow(spreadsheetId: String = credentials.sheetID, range: String = DEFAULT_WRITE_RANGE, writeArray: MutableList<MutableList<Any>>) {
		service.spreadsheets().values()
			.update(spreadsheetId, range, ValueRange().setValues(writeArray))
			.setValueInputOption(VALUE_INPUT_OPTION)
			.execute()
	}

	companion object {
		private const val TOKENS_DIRECTORY_PATH = "tokens"
		private const val DEFAULT_WRITE_RANGE = "Лист1!A:A"
		private const val DEFAULT_READ_RANGE = "Лист1!A:Z"
		private const val CRED_PATH = "./credentials.json"
		private const val VALUE_INPUT_OPTION = "RAW"
		private const val ACCESS_TYPE = "offline"
		private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
		private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE)
	}
}

//fun main() {
//	//example
//	val service = GoogleSheetsService()
//	val response = service.readSheet()
//	val values = response.getValues()
//
//	if (values == null || values.isEmpty()) {
//		Logger.printResult("No data found.")
//	} else {
//		for (row in values) {
//			Logger.printResult(row)
//		}
//		service.writeNewRow(
//			writeArray = mutableListOf(
//				mutableListOf<Any>("1"),
//				mutableListOf<Any>("1", 2 ,"", "Круто!"),
//			)
//		)
//	}
//}
