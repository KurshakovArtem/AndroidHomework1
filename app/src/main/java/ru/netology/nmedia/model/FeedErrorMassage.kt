package ru.netology.nmedia.model

data class ErrorReport(
    val postIdError: Long,
    val feedErrorMassage: FeedErrorMassage
)

enum class FeedErrorMassage {
LIKE_ERROR, DISLIKE_ERROR, REMOVE_ERROR, SAVE_ERROR, SAVE_REFRESH_ERROR
}