package com.erik_kz.jiyi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.erik_kz.jiyi.models.BoardSize
import com.erik_kz.jiyi.models.MemoryCard
import kotlin.math.min

//val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()

// basically this just helps us
interface CardClickListener {
    fun onCardClickListener(position: Int)
}

class MemoryBoardAdapter(
    private val rvParent: RecyclerView,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: (position: Int, holder: ViewHolder, rvContext: Context) -> Unit,
    ): RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    // someone told me to do this ocntext thing instead of getSystem().displayMetrics
    private val Int.px: Int get() = (this * rvParent.context.resources.displayMetrics.density).toInt()

    companion object {
        private const val CARD_MARGIN_SIZE = 8
        private const val TAG = "MemoryBoardAdapter"
    }


    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cvCard = view.findViewById<CardView>(R.id.cvCard)
        val ibvCard = view.findViewById<ImageButton>(R.id.ibvCardImage)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.memory_card, parent, false)
        val cardLayoutParams = view.findViewById<CardView>(R.id.cvCard).layoutParams as MarginLayoutParams

        // these were previously set inside onCreateViewHolder then you realize.. well we only need these once!!
        val rvPadding = parent.paddingTop * 2
        val cardMarginInPx = CARD_MARGIN_SIZE.px
        val slotWidth = (parent.width - rvPadding) / boardSize.cols
        val slotHeight = (parent.height - rvPadding) / boardSize.rows
        val cardSize = min(slotWidth, slotHeight)

//        Log.d(TAG, "${(rvParent.width - rvPadding) / boardSize.cols} == $slotWidth")
        cardLayoutParams.width = cardSize - cardMarginInPx * 2
        cardLayoutParams.height = cardSize - cardMarginInPx * 2
        cardLayoutParams.setMargins(cardMarginInPx, cardMarginInPx, cardMarginInPx, cardMarginInPx)

        return ViewHolder(view)
    }

    var selectedCardIds: ArrayList<Int> = arrayListOf()
    var isInTimeout = false
//
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // we animate only the cv and the ibv's only purpose is the onclick and displaying the image.
        //
        holder.ibvCard.setOnClickListener {
            cardClickListener(position, holder, rvParent.context)
        }
    }

    override fun getItemCount() = boardSize.numCards
}