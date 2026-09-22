package com.factotum8.networking4jandroid.data.local.db

import androidx.room.TypeConverter
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToIso(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun circleToValue(value: Circle?): String? = value?.value

    @TypeConverter
    fun valueToCircle(value: String?): Circle? = value?.let { stored -> Circle.entries.first { it.value == stored } }

    @TypeConverter
    fun contactTypeToValue(value: ContactType?): String? = value?.value

    @TypeConverter
    fun valueToContactType(value: String?): ContactType? =
        value?.let { stored -> ContactType.entries.first { it.value == stored } }

    @TypeConverter
    fun actionTypeToValue(value: ActionType?): String? = value?.value

    @TypeConverter
    fun valueToActionType(value: String?): ActionType? =
        value?.let { stored -> ActionType.entries.first { it.value == stored } }

    @TypeConverter
    fun interactionTypeToValue(value: InteractionType?): String? = value?.value

    @TypeConverter
    fun valueToInteractionType(value: String?): InteractionType? =
        value?.let { stored -> InteractionType.entries.first { it.value == stored } }
}
