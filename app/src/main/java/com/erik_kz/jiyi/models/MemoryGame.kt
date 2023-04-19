package com.erik_kz.jiyi.models

import com.erik_kz.jiyi.utils.DEFAULT_ICONS
import kotlin.random.Random

class MemoryGame(boardSize: BoardSize) {
    val cards: List<MemoryCard>
    var numPairsFound = 0
    var numMoves = 0
    var alreadySelectedCardPosition: Int? = null
    var isGameWon = false

    init {

        // ironically, card ids aren't unique
        val cardIds: IntArray = (IntArray(boardSize.numPairs) {it}).let {(it+it)}//.also { it.shuffle() }
        cardIds.shuffle(Random(System.currentTimeMillis()))

        val cardImages = DEFAULT_ICONS.shuffled()

        cards = cardIds.mapIndexed { index, it -> MemoryCard(it, cardImages[it], index) }
    }


}