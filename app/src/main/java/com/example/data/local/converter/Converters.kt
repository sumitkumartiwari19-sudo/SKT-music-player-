package com.example.data.local.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) return ""
        return list.joinToString(",")
    }
}
