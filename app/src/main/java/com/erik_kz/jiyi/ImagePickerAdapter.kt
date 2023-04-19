package com.erik_kz.jiyi

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.erik_kz.jiyi.models.BoardSize

object UriCallback: DiffUtil.ItemCallback<Uri>() {


    override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
        return oldItem == newItem
    }
}

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImageUris: MutableList<Uri>,
    private val boardSize: BoardSize,
    private val onClickListener: () -> Unit,
    private val updateToolbarCount: () -> Unit
) : ListAdapter<Uri, ImagePickerAdapter.ViewHolder>(UriCallback) {

    private companion object {
        const val TAG = "ImagePickerAdapter"
    }


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var imageUri: Uri? = null
        val ivCustomImage: ImageView = view.findViewById(R.id.ivCustomImage)

        fun onBind(position: Int) {
            // when we use notifyItemRemoved, the other items don't know they're in the new position
            // so they're still bound to the previous position.
            // however, at CreateActivity.kt line 113 we notify the adapter of the change.
            // so some items here and there will not have the right position but
            // the relevant ones will!
            if (position < chosenImageUris.size) {
                imageUri = chosenImageUris[position]
                ivCustomImage.setOnClickListener {
                    val posInList = chosenImageUris.indexOf(imageUri)
                    chosenImageUris.remove(imageUri)
                    imageUri = null
                    notifyItemRemoved(posInList)
                    Log.d(TAG, "removed at $position - $posInList | $chosenImageUris")
                    updateToolbarCount()
                }
            } else {
                ivCustomImage.setOnClickListener{
                    Log.d(TAG, "clicked at $position")
                    onClickListener()
                }
            }
            ivCustomImage.setImageURI(imageUri)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_card, parent, false)
//      dont need all of this since i found a new way (check layout/image_card.xml)
//        val clWidth = parent.width / boardSize.cols
//        val clHeight = parent.height / (boardSize.rows/2).toInt()
//        val clSize = min(clWidth, clHeight)
//        val lpCustomImage = view.findViewById<ConstraintLayout>(R.id.cvCustomImage).layoutParams
//        lpCustomImage.apply {
//            width = clSize
//            height = clSize
//        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "updating at $position")
        holder.onBind(position)
    }

    override fun getItemCount() = boardSize.numPairs

}
