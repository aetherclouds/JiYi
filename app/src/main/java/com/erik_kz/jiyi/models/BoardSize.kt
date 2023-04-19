package com.erik_kz.jiyi.models

enum class BoardSize() {
    EASY,
    MEDIUM,
    HARD;

    val cols get(): Int{
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    val rows get(): Int{
        return when (this) {
            EASY -> 4
            MEDIUM -> 6
            HARD -> 7
        }
    }

    val numCards get(): Int{
        return rows*cols
    }

    val numPairs get(): Int{
        return rows*cols/2
    }
}