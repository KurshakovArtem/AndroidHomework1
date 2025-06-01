package ru.netology.nmedia.supportingFunctions


fun converterNumToString(value: Int): String {
    val thousands = value / 1000
    val hundreds = (value % 1000) / 100
    val hundredsOfThousands = (value % 1_000_000) / 100_000
    val millions = value / 1_000_000
    return when (value) {
        in 0..999 -> value.toString()
        in 1_000..9_999 -> if (hundreds == 0) {
            "${thousands}K"
        } else {
            "${thousands}.${hundreds}K"
        }
        in 10_000..999_999 -> "${thousands}K"
        in 1_000_000..999_000_000 -> if (hundredsOfThousands == 0) {
            "${millions}M"
        } else {
            "${millions}.${hundredsOfThousands}M"
        }

        else -> "Недопустимое значение"   // позже переделать в исключение
    }
}