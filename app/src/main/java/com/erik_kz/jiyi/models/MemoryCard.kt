package com.erik_kz.jiyi.models

data class MemoryCard(
    val id: Int,
    val imageRef: Int,

    val position: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false,
    var isWaitingFlipBack: Boolean = false,
)