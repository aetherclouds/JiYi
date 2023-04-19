package com.erik_kz.jiyi.utils

import android.graphics.Bitmap
import kotlin.math.max

object BitmapScaler {
    fun cropBmpToSquare(bitmap: Bitmap): Bitmap {
        lateinit var finalBitmap: Bitmap
        if (bitmap.width >= bitmap.height){

            finalBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width/2 - bitmap.height/2,
                0,
                bitmap.height,
                bitmap.height
            );

        }else{

            finalBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height/2 - bitmap.width/2,
                bitmap.width,
                bitmap.width
            );
        }
        return finalBitmap
    }

    fun scaleBmpToLargestSide(bitmap: Bitmap, targetLength: Int): Bitmap {
//      basic algebra: length * factor = targetLength
        val scaleFactor = targetLength.toFloat() / max(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scaleFactor).toInt(),
            (bitmap.height * scaleFactor).toInt(),
            true
        )
    }

}
