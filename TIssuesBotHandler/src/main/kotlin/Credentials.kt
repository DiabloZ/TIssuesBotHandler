package suhov.vitaly

import kotlinx.serialization.Serializable

@Serializable
data class Credentials (
	val token: String
)