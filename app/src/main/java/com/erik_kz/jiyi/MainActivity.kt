package com.erik_kz.jiyi

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erik_kz.jiyi.models.BoardSize
import com.erik_kz.jiyi.models.MemoryCard
import com.erik_kz.jiyi.models.MemoryGame
import com.erik_kz.jiyi.models.UserImageList
import com.erik_kz.jiyi.utils.EXTRA_BOARD_SIZE
import com.erik_kz.jiyi.utils.doDaRainbow
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


@RequiresApi(Build.VERSION_CODES.P)
class MainActivity : AppCompatActivity() {
    companion object {
        private const val CREATE_REQUEST_CODE = 123
    }
    private val db = Firebase.firestore
    private var gameName: String? = null

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var textNumMoves: String
    private lateinit var textNumPairs: String
    private lateinit var tbTitle: androidx.appcompat.widget.Toolbar


    private var boardSize = BoardSize.EASY
    private var memoryGame = MemoryGame(boardSize)

    private var toolbarHue = 0F

    private lateinit var memoryAdapter: MemoryBoardAdapter

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        /* attempt at programmatically making array of items in resources folder. gave up and mapped
        each individually */
//        val lines = object {}.javaClass.getResourceAsStream("/yep.xml")?.bufferedReader()?.readLines()
//        Log.d("Yep2", "$lines")
//        val fileContent = this::class.java.getResource("/yep.xml/")?.readText()
//        Log.d("Yep", "$fileContent")
//
//        val fileContentt = this::class.java.getResourceAsStream("yep.xml")?.bufferedReader()?.readLines()
//        Log.d("Yep3", "$fileContentt")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tbTitle = findViewById(R.id.tbTitle)
//        tbTitle.title = "yep"
        setSupportActionBar(tbTitle)

        rvBoard = findViewById(R.id.rvBoard)
        // performance improvement
        rvBoard.hasFixedSize()
        // remove default fade transition when using setImageResource
        rvBoard.itemAnimator = null

        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)


        newGame()

        doDaRainbow(window, tbTitle, ::toolbarHue)

//        val intent = Intent(this, CreateActivity::class.java)
//        intent.putExtra(EXTRA_BOARD_SIZE, BoardSize.HARD)
//        intent.putExtra("EXTRA_TOOLBAR_HUE", toolbarHue)
//        startActivityForResult(intent, CREATE_REQUEST_CODE)
    }

    private fun showAlertDialog(title: String, view: View?, positiveFunction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") {_, _ ->
                positiveFunction()
            }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
//        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRefresh -> {
                if (memoryGame.numPairsFound > 0 && !memoryGame.isGameWon) {
                    showAlertDialog("Refresh game and reset progress?", null, ::newGame)
                } else {
                    newGame()
                }
            }
            R.id.miNewSize -> {
                showRadioDialog()
            }
            R.id.miNewCustom -> {
                showCreationDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val rgBoardSize = boardSizeView.findViewById<RadioGroup>(R.id.rgBoardSize)
        showAlertDialog("Choose new size", boardSizeView) {
            val desiredBoardSize = when (rgBoardSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            intent.putExtra("EXTRA_TOOLBAR_HUE", toolbarHue)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra("EXTRA_GAME_NAME")
            if (customGameName == null) {
                return
            }
            downloadGame(customGameName)
        }
//        when (requestCode) {
//            CREATE_REQUEST_CODE -> {
//
//            }
//        }
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            document.toObject(UserImageList::class.java)
        }.addOnFailureListener {
            // TODO
        }
    }

    private fun showRadioDialog(/*title: String, options: Map<String, String>, execFun: (String) -> Any?*/) {
        // variable names here are for board size but they can be turned to generic
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        // NVM this was gonna be hard and useless so i stoped
        val rgBoardSize = boardSizeView.findViewById<RadioGroup>(R.id.rgBoardSize)
        rgBoardSize.check(when (boardSize) {
            BoardSize.EASY -> R.id.rbEasy
            BoardSize.MEDIUM -> R.id.rbMedium
            else -> R.id.rbHard
        })
//        for (entry in options) {
//            val radioButton = RadioButton(this)
//            radioButton.text = entry.value
//            radioButton.setId(entry.key )
//            rgBoardSize.addView(radioButton)
//        }
        showAlertDialog("Choose new size", boardSizeView) {
            boardSize = when (rgBoardSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            newGame()
        }
    }

    private fun newGame(boardSize: BoardSize = this.boardSize) {

        memoryGame = MemoryGame(boardSize)

        tbTitle.title = "${getString(R.string.app_name)} :: ${boardSize.name.lowercase().capitalize()} ${boardSize.cols} x ${boardSize.rows}"

        textNumMoves = getString(R.string.num_moves)
        tvNumMoves.text = textNumMoves.format(0)
        textNumPairs = getString(R.string.num_pairs)
        tvNumPairs.text = textNumPairs
        val numPairsSpan = SpannableString(" ${memoryGame.numPairsFound} / ${boardSize.numPairs}")
        numPairsSpan.setSpan(
            ForegroundColorSpan(this.getColor(R.color.progressNone)),
            0,
            numPairsSpan.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvNumPairs.append(numPairsSpan)

        memoryAdapter = MemoryBoardAdapter(rvBoard, boardSize, memoryGame.cards, ::cardOnClick)


        rvBoard.layoutManager = GridLayoutManager(this, boardSize.cols)
        rvBoard.adapter = memoryAdapter
    }


    private fun cardOnClick(position: Int, currentHolder: MemoryBoardAdapter.ViewHolder, rvContext: Context) {
        val currentCard = memoryGame.cards[position]

        // will only run if card was facing down and is not matched
        if (!currentCard.isFaceUp and !currentCard.isMatched) {
            flipCard(currentCard, currentHolder, position, rvContext)
        }
//        https://stackoverflow.com/a/68604493
//        memoryAdapter.notifyDataSetChanged()

//        memoryAdapter.notifyItemChanged(position)
    }

    private fun flipCard(
        currentCard: MemoryCard,
        currentHolder: MemoryBoardAdapter.ViewHolder,
        currentPosition: Int,
        rvContext: Context
    ) {
        currentCard.isFaceUp = true

        // animate if card isn't already up and waiting to animate flipping back down
        if (!currentCard.isWaitingFlipBack) {
            animateFlipCard(currentHolder, currentCard)
            currentCard.isWaitingFlipBack = true
        } else {
            // if it was already waiting to get flipped down, cancel it
            // THIS FUCKING WORKS!!!
            handler.removeCallbacksAndMessages(currentCard.position)
        }

        //             already has one card selected and is now selecting 2nd
        if (memoryGame.alreadySelectedCardPosition != null) {
            val otherCard = memoryGame.cards[memoryGame.alreadySelectedCardPosition!!]
            val otherHolder =
                rvBoard.findViewHolderForLayoutPosition(otherCard.position) as MemoryBoardAdapter.ViewHolder

            // if 2 selected cards are equal
            tryMatchCards(currentHolder, currentCard, otherCard, otherHolder, rvContext)

            memoryGame.alreadySelectedCardPosition = null
        } else {
            memoryGame.alreadySelectedCardPosition = currentPosition
        }
    }

    private fun tryMatchCards(
        currentHolder: MemoryBoardAdapter.ViewHolder,
        currentCard: MemoryCard,
        otherCard: MemoryCard,
        otherHolder: MemoryBoardAdapter.ViewHolder,
        rvContext: Context
    ) {
        memoryGame.numMoves++
        tvNumMoves.text = textNumMoves.format(memoryGame.numMoves)
        if (otherCard.id == currentCard.id) {
            currentCard.isMatched = true
            otherCard.isMatched = true
            animateMatchCard(currentHolder, rvContext)
            animateMatchCard(otherHolder, rvContext)

            memoryGame.numPairsFound++

            val numPairsColor = interpolateColor(
                ContextCompat.getColor(rvContext, R.color.progressNone),
                ContextCompat.getColor(rvContext, R.color.progressFull),
                memoryGame.numPairsFound.toFloat() / boardSize.numPairs
            )
            tvNumPairs.text = textNumPairs.format(memoryGame.numPairsFound, boardSize.numPairs)
            val numPairsSpan = SpannableString(" ${memoryGame.numPairsFound} / ${boardSize.numPairs}")
            numPairsSpan.setSpan(ForegroundColorSpan(numPairsColor), 0, numPairsSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvNumPairs.append(numPairsSpan)

            if (memoryGame.numPairsFound == boardSize.numPairs) memoryGame.isGameWon = true

        } else {
            currentCard.isFaceUp = false
            otherCard.isFaceUp = false

            /* if you assign primitives, it doesnt reference the value. rather it "instances"
                    directly, which is neat */
            //                    val positionBeforeResetting = memoryGame.alreadySelectedCardPosition

            handler.postDelayed(
                AnimateRunnable(currentHolder, currentCard),
                currentCard.position,
                500
            )
            handler.postDelayed(
                AnimateRunnable(otherHolder, otherCard),
                otherCard.position,
                500
            )

            // empirically figured that this is not necessary, but makes sense to have it anyway ig
            memoryAdapter.notifyItemChanged(currentCard.position)
            memoryAdapter.notifyItemChanged(otherCard.position)
        }
    }
}



// https://stackoverflow.com/a/7871291
private fun interpolate(a: Float, b: Float, proportion: Float): Float {
    return a + (b - a) * proportion
}

/** Returns an interpoloated color, between `a` and `b`  */
private fun interpolateColor(RGB1: Int, RGB2: Int, proportion: Float): Int {
    val hsv1 = FloatArray(3)
    val hsv2 = FloatArray(3)
    Color.colorToHSV(RGB1, hsv1)
    Color.colorToHSV(RGB2, hsv2)
    for (i in 0..2) {
        hsv2[i] = interpolate(hsv1[i], hsv2[i], proportion)
    }
    return Color.HSVToColor(hsv2)
}

class AnimateRunnable(private val holder: MemoryBoardAdapter.ViewHolder, private val card: MemoryCard) : Runnable {
    override fun run() {
        /* say we click the same card twice. then it'll be still facing up,
        so we shouldn't animate it facing down.
        so we shouldn't set isWaitingFlipBack to false since it's not supposed
        to turn down. */
        if (!card.isFaceUp) {
            animateFlipCard(holder, card)
            card.isWaitingFlipBack = false
        }
    }
}

fun animateFlipCard(holder: MemoryBoardAdapter.ViewHolder, card: MemoryCard) {
//            val holder = rvBoard.findViewHolderForLayoutPosition(position) as MemoryBoardAdapter.ViewHolder

    // remember that to animate we need the previous iteration of the adapter data to be, for erik_kz,
    // facing down, and the next facing up. but we update the card.isFaceUp before animating,
    // so it's like we're looking into the future iteration already.
    val willFaceUp = if (card.isWaitingFlipBack) false else card.isFaceUp

    val cvCard = holder.cvCard
    val ibvCard = holder.ibvCard
    cvCard.bringToFront()
    cvCard.animate().apply {
        duration = 125
        rotationY(90F)
        interpolator = AccelerateInterpolator()

    }.withEndAction {
        ibvCard.setImageResource(if (willFaceUp) card.imageRef else R.drawable.ic_launcher_background)
        ibvCard.scaleX = if (willFaceUp) 1F else -1F
        cvCard.animate().apply {
            duration = 125
            rotationY(if (willFaceUp) 180F else 0F)
            interpolator = DecelerateInterpolator()
        }.start()
    }
}

fun animateMatchCard(holder: MemoryBoardAdapter.ViewHolder, rvContext: Context) {
    val colorFrom = ContextCompat.getColor(rvContext, R.color.bgSecondary)
    val colorTo = ContextCompat.getColor(rvContext, R.color.bgPrimary)

    /* by using this evaluator we would be sure the objects are ARGB. important bc you can actually
    omit the A and so the integers would be drastically different */
//     USE THIS FOR COOL WEIRD RAINBOW EFFECT!!
//    val backgroundAnimation = ValueAnimator.ofInt(colorFrom, colorTo).apply {
    val backgroundAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
        duration = 2000
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            holder.ibvCard.setBackgroundColor(it.animatedValue as Int)
        }
    }
    val foregroundAnimation = ValueAnimator.ofInt(255, 100).apply {
        duration = 500
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            holder.ibvCard.imageAlpha = it.animatedValue as Int
        }
    }
    AnimatorSet().apply {
        playTogether(backgroundAnimation)
        start()
    }
}