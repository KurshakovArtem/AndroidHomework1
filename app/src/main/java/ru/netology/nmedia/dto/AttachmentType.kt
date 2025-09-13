package ru.netology.nmedia.dto

enum class AttachmentType {
    IMAGE, VIDEO, EMPTY,
}

fun String.toAttachmentType(): AttachmentType{
    return when(this){
        "IMAGE" -> AttachmentType.IMAGE
        "VIDEO" -> AttachmentType.VIDEO
        else ->  AttachmentType.EMPTY
    }
}