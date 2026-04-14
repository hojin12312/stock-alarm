package com.example.playground.data.local

import androidx.room.TypeConverter
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market

class Converters {
    @TypeConverter
    fun maStatusToString(value: MaStatus?): String? = value?.name

    @TypeConverter
    fun stringToMaStatus(value: String?): MaStatus? = value?.let { MaStatus.valueOf(it) }

    @TypeConverter
    fun marketToString(value: Market): String = value.name

    @TypeConverter
    fun stringToMarket(value: String): Market = Market.valueOf(value)
}
