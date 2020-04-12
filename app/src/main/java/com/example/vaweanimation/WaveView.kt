package com.example.vaweanimation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader

class WaveView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) :
    View(context, attributeSet, defStyle), TiltListener {


    private val paint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE

    }

    private val gradientPaint = Paint(ANTI_ALIAS_FLAG).apply {
        // Highlight only the areas already touched on the canvas
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private val green = Color.GREEN

    private val gradientColors =
        intArrayOf(green,
           // modifyAlpha(green, 0.10f),
           // modifyAlpha(green, 0.05f)
            Color.RED,
            Color.BLUE
        )

    private val delta = 30f
    private val center = PointF(0f, 0f)
    private var maxRadius = 0f
    private val initialRadius = 0f

    private var waveAnimator: ValueAnimator? = null
    private var radiusOffSet = 0f
    set(value) {
        field = value
        postInvalidateOnAnimation()
    }

    private val gradientMatrix = Matrix()

    private var path = Path()

    val tiltSensor = WaveTiltSensor(context)


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        waveAnimator = ValueAnimator.ofFloat(0f,delta).apply {
            duration = 1500L
           // repeatMode =  ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                radiusOffSet = it.animatedValue as Float
            }
            start()
        }
        tiltSensor.addListener(this)
        tiltSensor.registerListeners()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
        tiltSensor.unRegisterListeners()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w > 0 && h > 0) {
            center.set(w / 2f, h / 2f)
            maxRadius = max(w, h).toFloat()

            //Create gradient after getting sizing information
            gradientPaint.shader = RadialGradient(
                center.x, center.y, maxRadius,
                gradientColors, null, Shader.TileMode.CLAMP
            )
        }
    }

    override fun onTilt(pitchRollRad: Pair<Double, Double>) {
        val pitchRad = pitchRollRad.first
        val rollRad = pitchRollRad.second

        // Use half view height/width to calculate offset instead of full view/device measurement
        val maxYOffset = center.y.toDouble()
        val maxXOffset = center.x.toDouble()

        val yOffset = (sin(pitchRad) * maxYOffset)
        val xOffset = (sin(rollRad) * maxXOffset)

        updateGradient(xOffset.toFloat() + center.x, yOffset.toFloat() + center.y)

    }

    private fun updateGradient(x: Float, y: Float) {
        gradientMatrix.setTranslate(x - center.x, y - center.y)
        gradientPaint.shader.setLocalMatrix(gradientMatrix)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        var currentRadius = initialRadius + radiusOffSet
        while (currentRadius < maxRadius) {
           // canvas?.drawCircle(center.x, center.y, currentRadius, paint)
            canvas?.drawPath(createPath(currentRadius,path),paint)
            currentRadius += delta
        }
        canvas?.drawPaint(gradientPaint)

    }

    private fun createPath(radius: Float,
                           path : Path = Path(),
                           points: Int = 20
    ): Path {
        path.reset()

        val pointDelta = 0.7f
        val angleInRadians = 2*Math.PI / points
        val startAngleInRadians = 0.0

        path.moveTo(
            center.x + (radius * pointDelta * cos(startAngleInRadians)).toFloat(),
            center.y + (radius * pointDelta * sin(startAngleInRadians)).toFloat()
        )

        for (i in 1 until points){
            val hypotenuse = if (i % 2 == 0) {
                pointDelta * radius
            } else {
                radius
            }
            val nextPointX = center.x + (hypotenuse * cos(startAngleInRadians - angleInRadians * i)).toFloat()
            val nextPointY = center.y + (hypotenuse * sin(startAngleInRadians - angleInRadians * i)).toFloat()
            path.lineTo(nextPointX, nextPointY)
        }
        path.close()
        return path
    }

}