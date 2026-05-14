package com.numisproerp.ui.splash

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.numisproerp.R

/**
 * `VideoView`, що зберігає пропорції відео (як `centerInside` / `fitCenter`):
 * відео вписується у наявну ширину/висоту батька без розтягування й без
 * обрізання зверху/знизу. Це уникає ефекту "звуженої емблеми" і чорних
 * горизонтальних смуг від примусового заповнення всієї площі.
 */
class FullScreenVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    /** Зберігає розміри відео й перепризначає layout, щоб onMeasure знав пропорції. */
    fun applyVideoSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            videoWidth = width
            videoHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (videoWidth <= 0 || videoHeight <= 0 || parentWidth <= 0 || parentHeight <= 0) {
            setMeasuredDimension(parentWidth, parentHeight)
            return
        }

        // Збільшуємо ширину на 12%, щоб емблема виглядала ширшою (як прохав
        // користувач), не виходячи за межі екрану.
        val targetMaxWidth = (parentWidth * 1.12f).toInt().coerceAtMost(parentWidth)

        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val parentRatio = targetMaxWidth.toFloat() / parentHeight.toFloat()

        val (w, h) = if (videoRatio > parentRatio) {
            // Відео ширше — впишемо за шириною (висоту обмежить пропорція).
            val width = targetMaxWidth
            val height = (width / videoRatio).toInt()
            width to height
        } else {
            // Відео вище — впишемо за висотою, щоб не обрізати зверху/знизу.
            val height = parentHeight
            val width = (height * videoRatio).toInt().coerceAtMost(targetMaxWidth)
            width to height
        }

        setMeasuredDimension(w, h)
    }
}

/**
 * Compose-обгортка, яка програє відео `R.raw.splash_video`. Викликає
 * [onComplete] коли відео завершилось, було перерване або не змогло завантажитись.
 */
@Composable
fun SplashVideoScreen(onComplete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}") }

    // Safety net: завжди завершуємо після 8с, навіть якщо відео не запустилось.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                FullScreenVideoView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        mp.setVolume(0f, 0f) // mute
                        applyVideoSize(mp.videoWidth, mp.videoHeight)
                        start()
                    }
                    setOnCompletionListener { onComplete() }
                    setOnErrorListener { _, _, _ -> onComplete(); true }
                }
            }
        )
    }
}
