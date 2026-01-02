package com.example.hamparo.data.local

import androidx.room.TypeConverter
import com.example.hamparo.data.local.entities.MedicionType
import com.example.hamparo.data.local.entities.UserRole

class Converters {

    // --- Convertidores para UserRole ---
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(value: String): UserRole {
        return UserRole.valueOf(value)
    }

    // --- Convertidores para MedicionType ---
    @TypeConverter
    fun fromMedicionType(type: MedicionType): String {
        return type.name
    }

    @TypeConverter
    fun toMedicionType(value: String): MedicionType {
        return MedicionType.valueOf(value)
    }
}