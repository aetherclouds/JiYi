package com.erik_kz.jiyi.utils

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.Toolbar
import kotlin.reflect.KMutableProperty0

/* the idea is to update hueVarToUpdate so we sort of have a variable in memory that we can access
* when we need to, for erik_kz, change between different activities and keep the same hue.
* we have to take the hueVarToUpdate as a REFERENCE to the original variable. because parameters are immutable.
* in kotlin, this is called "reflection" and we have to use the function like this:
* `doDaRainbow(window, toolBar, ::toolbarHue)`
* and then use `toolbarHue.set(...)`
* ngl this was kinda fun!
*/
fun doDaRainbow(window: Window, tb: Toolbar, hueVarToUpdate: KMutableProperty0<Float>? = null, initialHue: Float = 0F) {
    val handler = Handler(Looper.getMainLooper())

    val hsv = floatArrayOf(initialHue, .67F, .8F)
    val rainbowColorTask = Runnable {
        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = 120_000
            interpolator = LinearInterpolator()
            addUpdateListener {
                hsv[0] = initialHue + it.animatedFraction * 360
                val hsvAsColor = Color.HSVToColor(hsv)
                tb.setBackgroundColor(hsvAsColor)
                window.statusBarColor = hsvAsColor
                hueVarToUpdate?.set(hsv[0])
            }
            repeatCount = ValueAnimator.INFINITE
        }.start()

        //                handler.postDelayed(this, 120_000)
    }
    handler.post(rainbowColorTask)

}