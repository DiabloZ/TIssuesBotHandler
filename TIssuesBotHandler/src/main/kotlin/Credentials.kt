package suhov.vitaly

import kotlinx.serialization.Serializable

@Serializable
data class Credentials (
	val token: String,
	val googleSheetCredentials: GoogleSheetsServiceCredentials,
)

@Serializable
data class GoogleSheetsServiceCredentials(
	val applicationName: String,
	val sheetID: String,
	val userID: String,
)