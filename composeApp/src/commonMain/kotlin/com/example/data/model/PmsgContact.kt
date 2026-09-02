package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PmsgContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val hasPmsgInstalled: Boolean = true,
    val statusDescription: String = "Disponível no Pmsg (Criptografado 24h)",
    val avatarColorHex: Long = 0xFF00FFC2
)
