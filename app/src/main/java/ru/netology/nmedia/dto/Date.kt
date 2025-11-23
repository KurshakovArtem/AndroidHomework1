package ru.netology.nmedia.dto

data class Date(
    override val id: Long,
    val title: String
): FeedItem()
