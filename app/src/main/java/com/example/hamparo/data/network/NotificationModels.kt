package com.example.hamparo.data.network

data class PushNotification(
    val data: NotificationData,
    val to: String // El token del receptor (Cuidador)
)

data class NotificationData(
    val titulo: String,
    val mensaje: String
)