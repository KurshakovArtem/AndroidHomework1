package ru.netology.nmedia.supportingFunctions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


object DateSeparator {
    private val zone = ZoneId.of("Europe/Moscow")
    fun getPeriod(time: Long): String{
        val published = Instant
            .ofEpochSecond(time)
            .atZone(zone)
            .toLocalDate()

        val today = LocalDate.now(zone)
        return when(published){
            today -> "Сегодня"
            today.minusDays(1) -> "Вчера"
            else -> "На прошлой неделе"
        }
    }
}