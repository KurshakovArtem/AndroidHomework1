package ru.netology.nmedia.dto

data class Attachment(
    val url: String,
    val description: String?,
    val type: AttachmentType
)
