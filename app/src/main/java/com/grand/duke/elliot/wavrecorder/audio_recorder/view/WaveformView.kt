package com.grand.duke.elliot.wavrecorder.audio_recorder.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.grand.duke.elliot.wavrecorder.R
import com.grand.duke.elliot.wavrecorder.audio_recorder.SAMPLE_RATE_IN_HZ
import com.grand.duke.elliot.wavrecorder.audio_recorder.TIMESTAMP_INTERVAL_MILLISECONDS
import com.grand.duke.elliot.wavrecorder.audio_recorder.TIMESTAMP_PATTERN
import com.grand.duke.elliot.wavrecorder.audio_recorder.minBufferSize
import com.grand.duke.elliot.wavrecorder.util.toDateFormat
import com.grand.duke.elliot.wavrecorder.util.toPx
import timber.log.Timber

class WaveformView: View {

    enum class State {
        DragWhilePlaying,
        Initialized,
        OverwriteRecording,
        PausePlaying,
        PauseRecording,
        Playing,
        Recording,
        StopPlaying,
        StopRecording
    }

    private var state = State.Initialized
    private var allowDragWhilePlaying = false

    fun isPlaying() = state == State.Playing
    fun isDragWhilePlaying() = state == State.DragWhilePlaying

    // (minBufferSize * 1000) / (sizeof(short) * sampleRateInHz)
    private val updateIntervalMillis = ((minBufferSize() * 1000) / (2 * SAMPLE_RATE_IN_HZ)).toLong()

    /** OnTouchListener */
    private var onTouchListener: OnTouchListener? = null

    fun setOnTouchListener(onTouchListener: OnTouchListener) {
        this.onTouchListener = onTouchListener
    }

    interface OnTouchListener {
        fun onTouchActionDown()
        fun onTouchActionMove()
        fun onTouchActionUp()
    }

    private var pivot = 0
    private var measured = false
    private var startX = 0F
    private var previousDx = 0F

    private var overwrittenChunkHeights = ArrayList<Float>()
    private var startOverwriting = 0

    private var maxVisibleChunkCount = 0F
    private var halfMaxVisibleChunkCount = 0F

    private var viewWidth = 0F
    private var halfViewWidth = 0

    private var viewHeight = 0F

    private val maxReportableAmplitude = 22760F  // Effective size, maximum amplitude: 32760F
    private val uninitialized = 0F
    private val topBottomPadding = 8.toPx()

    private val chunkHeights = ArrayList<Float>()

    private val chunkPaint = Paint()
    private var maxChunkHeight = uninitialized
    private var minChunkHeight = 2.toPx()
    private var chunkWidth = 1.toPx()
        set(value) {
            chunkPaint.strokeWidth = value
            field = value
        }
    private var chunkSpacing = 0.toPx()
    private var chunkColor = Color.RED
        set(value) {
            chunkPaint.color = value
            field = value
        }
    private var chunkRoundedCorners = false
        set(value) {
            if (value)
                chunkPaint.strokeCap = Paint.Cap.ROUND
            else
                chunkPaint.strokeCap = Paint.Cap.BUTT
            field = value
        }
    private var smoothTransition = false
    private var chunkHorizontalScale = 0F
    private var overwrittenChunkColor =  ContextCompat.getColor(context, R.color.default_overwritten_chunk)

    private val scrubberPaint = Paint()
    private var scrubberColor =  ContextCompat.getColor(context, R.color.default_scrubber)
    private var scrubberWidth = 1.toPx()

    private var textHeight = 0F
    private val timestampTextPaint = TextPaint()
    private val timestampTextBottomPadding = 8.toPx()
    private var timestampTextColor =  ContextCompat.getColor(context, R.color.default_timestamp_text)
    private val timestampTextInterval = (TIMESTAMP_INTERVAL_MILLISECONDS / updateIntervalMillis).toInt()
    private var timestampTextSize = context.resources.getDimension(R.dimen.text_size_small)

    private val timestampBackgroundRect = Rect()
    private val timestampBackgroundPaint = Paint()
    private var timestampBackgroundColor = ContextCompat.getColor(context, R.color.default_timestamp_background)


    private val gridPaint = Paint()
    private var gridColor = ContextCompat.getColor(context, R.color.default_grid)
    private var gridVisibility = true
    private var gridWidth = 0.5F.toPx()

    private val subGridPaint = Paint()
    private var subGridColor = ContextCompat.getColor(context, R.color.default_sub_grid)
    private var subGridCount = 3
    private var subGridVisibility = true
    private var subGridWidth = 0.5F.toPx()

    /** Section repeat. */
    private val sectionNotSelected = -1
    private val sectionDividerPaint = Paint()
    private var sectionDividerColor =  ContextCompat.getColor(context, R.color.section_divider)
    private var sectionDividerCount = 0
    private var sectionDividerPosition1 = sectionNotSelected
    private var sectionDividerPosition2 = sectionNotSelected
    private var sectionDividerWidth = 1.toPx()

    private var sectionBackgroundColor =  ContextCompat.getColor(context, R.color.section_background)
    private var sectionBackgroundPaint = Paint()
    private var sectionBackgroundRect = Rect()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.WaveformView,
            0, 0
        ).apply {
            try {
                chunkColor = getColor(R.styleable.WaveformView_chunkColor, chunkColor)
                chunkRoundedCorners =
                    getBoolean(R.styleable.WaveformView_chunkRoundedCorners, chunkRoundedCorners)
                chunkSpacing = getDimension(R.styleable.WaveformView_chunkSpacing, chunkSpacing)
                chunkWidth = getDimension(R.styleable.WaveformView_chunkWidth, chunkWidth)
                maxChunkHeight =
                    getDimension(R.styleable.WaveformView_maxChunkHeight, maxChunkHeight)
                minChunkHeight =
                    getDimension(R.styleable.WaveformView_minChunkHeight, minChunkHeight)
                smoothTransition =
                    getBoolean(R.styleable.WaveformView_smoothTransition, smoothTransition)

                setWillNotDraw(false)
                chunkPaint.isAntiAlias = true

                scrubberColor = getColor(R.styleable.WaveformView_scrubberColor, scrubberColor)
                scrubberWidth = getDimension(R.styleable.WaveformView_scrubberWidth, scrubberWidth)

                scrubberPaint.color = scrubberColor
                scrubberPaint.isAntiAlias = true
                scrubberPaint.strokeWidth = scrubberWidth
                scrubberPaint.style = Paint.Style.STROKE

                timestampBackgroundColor =
                    getColor(R.styleable.WaveformView_timestampBackgroundColor, timestampBackgroundColor)
                timestampTextColor = getColor(R.styleable.WaveformView_timestampTextColor, timestampTextColor)
                timestampTextSize = getDimension(R.styleable.WaveformView_timestampTextSize, timestampTextSize)

                textHeight = timestampTextSize
                timestampTextPaint.color = timestampTextColor
                timestampTextPaint.isAntiAlias = true
                timestampTextPaint.strokeWidth = 2.toPx()
                timestampTextPaint.textAlign = Paint.Align.CENTER
                timestampTextPaint.textSize = textHeight

                timestampBackgroundPaint.color = timestampBackgroundColor
                timestampBackgroundPaint.style = Paint.Style.FILL

                gridColor = getColor(R.styleable.WaveformView_gridColor, gridColor)
                gridVisibility = getBoolean(R.styleable.WaveformView_gridVisibility, gridVisibility)
                gridWidth = getDimension(R.styleable.WaveformView_gridWidth, gridWidth)

                gridPaint.color = gridColor
                gridPaint.strokeWidth = gridWidth

                subGridColor = getColor(R.styleable.WaveformView_subGridColor, subGridColor)
                subGridCount = getInt(R.styleable.WaveformView_subGridCount, subGridCount)
                subGridVisibility = getBoolean(R.styleable.WaveformView_subGridVisibility, subGridVisibility)
                subGridWidth = getDimension(R.styleable.WaveformView_subGridWidth, subGridWidth)

                subGridPaint.color = subGridColor
                subGridPaint.strokeWidth = subGridWidth

                overwrittenChunkColor = getColor(R.styleable.WaveformView_overwrittenChunkColor, overwrittenChunkColor)

                chunkHorizontalScale = chunkSpacing + chunkWidth

                /** Section repeat. */
                sectionDividerPaint.color = sectionDividerColor
                sectionDividerPaint.isAntiAlias = true
                sectionDividerPaint.strokeWidth = sectionDividerWidth
                sectionDividerPaint.style = Paint.Style.STROKE

                sectionBackgroundPaint.color = sectionBackgroundColor
                sectionBackgroundPaint.style = Paint.Style.FILL
            } finally {
                recycle()
            }
        }

        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    previousDx = 0F

                    if (allowDragWhilePlaying)
                        onTouchListener?.onTouchActionDown()
                }
                MotionEvent.ACTION_MOVE -> {
                    var dx = event.x - startX
                    if (previousDx == 0F)
                        previousDx = dx
                    else {
                        val t = dx
                        dx -= previousDx
                        previousDx = t
                    }

                    shift(-(dx / chunkHorizontalScale).toInt())
                    onTouchListener?.onTouchActionMove()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    performClick()
                    if (allowDragWhilePlaying)
                        onTouchListener?.onTouchActionUp()
                }
            }
            true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measured.not())
            measured = true

        // Reconcile the measured dimensions with the this view's constraints and
        // set the final measured viewWidth and height.
        val width = MeasureSpec.getSize(widthMeasureSpec)
        viewWidth = width.toFloat()
        halfViewWidth = width / 2

        val height = MeasureSpec.getSize(heightMeasureSpec)
        viewHeight = height.toFloat()

        chunkHorizontalScale = chunkSpacing + chunkWidth
        maxVisibleChunkCount = viewWidth / chunkHorizontalScale
        halfMaxVisibleChunkCount = maxVisibleChunkCount / 2

        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            heightMeasureSpec
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridsAndTimestamps(canvas)
        drawChunks(canvas)
        canvas.drawLine(
            halfViewWidth.toFloat(),
            0F,
            halfViewWidth.toFloat(),
            measuredHeight.toFloat(),
            scrubberPaint
        )
    }

    fun addInitAmplitudes(amplitudes: ArrayList<Int>) {
        for (amplitude in amplitudes) {
            chunkHeights.add(chunkHeights.size, adjustChunkHeight(amplitude))
            pivot = chunkHeights.size.dec()
        }

        pivot = 0
        invalidate()
    }

    fun add(amplitude: Int) {
        when(state) {
            State.OverwriteRecording -> overwrite(amplitude)
            State.Recording -> {
                chunkHeights.add(chunkHeights.size, adjustChunkHeight(amplitude))
                pivot = chunkHeights.size.dec()
                invalidate()
            }
            else -> Timber.e("Invalid state: $state")
        }
    }

    private fun overwrite(amplitude: Int) {
        val adjustedChunkHeight = adjustChunkHeight(amplitude)

        if (pivot >= chunkHeights.size.dec()) {
            chunkHeights[pivot] = adjustedChunkHeight
            chunkHeights.add(chunkHeights.size, adjustedChunkHeight)
            pivot = chunkHeights.size.dec()
        } else {
            chunkHeights[pivot++] = adjustedChunkHeight

            if (pivot > chunkHeights.size.dec())
                pivot = chunkHeights.size.dec()
        }

        overwrittenChunkHeights.add(overwrittenChunkHeights.size, adjustedChunkHeight)
        invalidate()
    }

    private fun adjustChunkHeight(amplitude: Int): Float {
        if (amplitude.isZero())
            return minChunkHeight

        if (maxChunkHeight == uninitialized)
            maxChunkHeight = viewHeight - topBottomPadding * 2
        else if (maxChunkHeight > viewHeight - topBottomPadding * 2)
            maxChunkHeight = viewHeight - topBottomPadding * 2

        val verticalDrawScale = maxChunkHeight - minChunkHeight
        if (verticalDrawScale == 0F)
            return minChunkHeight

        val point = maxReportableAmplitude / verticalDrawScale
        if (point.isZero())
            return minChunkHeight

        var amplitudePoint = amplitude / point

        if (smoothTransition) {
            val scaleFactor = calculateScaleFactor()

            when(state) {
                State.Initialized -> {
                    if (chunkHeights.isNotEmpty()) {
                        amplitudePoint = amplitudePoint.smoothTransition(
                                chunkHeights[pivot] - minChunkHeight,
                                2.2F,
                                scaleFactor
                        )
                    }
                }
                State.OverwriteRecording -> {
                    if (overwrittenChunkHeights.isNotEmpty()) {
                        amplitudePoint = amplitudePoint.smoothTransition(
                            overwrittenChunkHeights[overwrittenChunkHeights.size.dec()] - minChunkHeight,
                            2.2F,
                            scaleFactor
                        )
                    }
                }
                State.Recording -> {
                    if (chunkHeights.isNotEmpty()) {
                        amplitudePoint = amplitudePoint.smoothTransition(
                            chunkHeights[pivot] - minChunkHeight,
                            2.2F,
                            scaleFactor
                        )
                    }
                }
                else -> throw IllegalStateException("Invalid state.")
            }
        }

        amplitudePoint += minChunkHeight

        if (amplitudePoint > maxChunkHeight)
            amplitudePoint = maxChunkHeight
        else if (amplitudePoint < minChunkHeight)
            amplitudePoint = minChunkHeight

        return amplitudePoint
    }

    private fun calculateScaleFactor(): Float {
        return when (updateIntervalMillis) {
            in 0..50 -> 1.6F
            in 50..100 -> 2.2F
            in 100..150 -> 2.8F
            in 150..200 -> 3.4F
            in 200..250 -> 4.2F
            in 250..500 -> 4.8F
            else -> 5.4F
        }
    }

    private fun drawChunks(canvas: Canvas) {
        if (chunkHeights.isEmpty())
            return

        val verticalCenter = (height + textHeight) / 2
        var range =
            if (pivot > halfMaxVisibleChunkCount)
                halfMaxVisibleChunkCount.toInt()
            else
                pivot
        var sectionBackgroundStartX1 = 0F
        var sectionBackgroundStartX2 = 0F

        /** The left chunks of the pivot. */
        for (i in 1 until range.inc()) {
            val index = pivot - i

            val startX = halfViewWidth - chunkHorizontalScale * i
            val startY = verticalCenter - chunkHeights[index] / 2
            val stopY = verticalCenter + chunkHeights[index] / 2

            if (state == State.OverwriteRecording && index >= startOverwriting)
                chunkPaint.color = overwrittenChunkColor
            else
                chunkPaint.color = chunkColor

            when (index) {
                sectionDividerPosition1 -> {
                    canvas.drawLine(
                            startX,
                            0F,
                            startX,
                            measuredHeight.toFloat(),
                            sectionDividerPaint
                    )

                    sectionBackgroundStartX1 = startX
                    if (sectionDividerCount == 2) {
                        if (sectionDividerPosition2 < pivot)
                            drawSection(canvas, sectionBackgroundStartX1.toInt(), sectionBackgroundStartX2.toInt())
                    }
                }
                sectionDividerPosition2 -> {
                    canvas.drawLine(
                            startX,
                            0F,
                            startX,
                            measuredHeight.toFloat(),
                            sectionDividerPaint
                    )

                    sectionBackgroundStartX2 = startX
                }
                else -> canvas.drawLine(
                        startX,
                        startY,
                        startX,
                        stopY,
                        chunkPaint)
            }
        }

        /** The Right chunks of the pivot. */
        if (pivot < chunkHeights.size.dec()) {
            chunkPaint.color = chunkColor

            range =
                if (chunkHeights.size - pivot > halfMaxVisibleChunkCount)
                    halfMaxVisibleChunkCount.toInt()
                else
                    chunkHeights.size - pivot

            for (i in 0 until range) {
                val index = pivot + i

                if (index > chunkHeights.size.dec())
                    break

                val startX = halfViewWidth + chunkHorizontalScale * i
                val startY = verticalCenter - chunkHeights[index] / 2
                val stopY = verticalCenter + chunkHeights[index] / 2

                when (index) {
                    sectionDividerPosition1 -> {
                        canvas.drawLine(
                                startX,
                                0F,
                                startX,
                                measuredHeight.toFloat(),
                                sectionDividerPaint
                        )

                        sectionBackgroundStartX1 = startX
                    }
                    sectionDividerPosition2 -> {
                        canvas.drawLine(
                                startX,
                                0F,
                                startX,
                                measuredHeight.toFloat(),
                                sectionDividerPaint
                        )

                        sectionBackgroundStartX2 = startX
                        if (sectionDividerCount == 2) {
                            //if (sectionDividerPosition1 > pivot)
                                drawSection(canvas, sectionBackgroundStartX1.toInt(), sectionBackgroundStartX2.toInt())
                        }
                    }
                    else -> canvas.drawLine(
                            startX,
                            startY,
                            startX,
                            stopY,
                            chunkPaint)
                }
            }
        }
    }

    private fun drawGridsAndTimestamps(canvas: Canvas) {
        drawTimestampBackground(canvas)

        var start = pivot - maxVisibleChunkCount.toInt()
        if (start < 0)
            start = 0
        val end = pivot + maxVisibleChunkCount.toInt()
        val subGridInterval = timestampTextInterval / subGridCount.inc() * chunkHorizontalScale

        for (i in start..end) {
            if (i % timestampTextInterval == 0) {
                val x = if (i <= pivot)
                    halfViewWidth - (pivot - i) * chunkHorizontalScale
                else
                    halfViewWidth + (i - pivot) * chunkHorizontalScale
                canvas.drawText(
                    (i * updateIntervalMillis).toTimestampString(),
                    x,
                    textHeight,
                    timestampTextPaint
                )

                // Grid
                if (gridVisibility) {
                    canvas.drawLine(
                        x,
                        height / 4F,
                        x,
                        textHeight + timestampTextBottomPadding,
                        gridPaint
                    )

                    if (subGridVisibility) {
                        for (j in 1..subGridCount) {
                            canvas.drawLine(
                                x + j * subGridInterval,
                                height / 5F,
                                x + j * subGridInterval,
                                textHeight + timestampTextBottomPadding,
                                subGridPaint
                            )
                        }
                    }
                }
            }
        }
    }

    fun shift(shift: Int) {
        pivot += shift

        if (pivot < 0)
            this.pivot = 0
        else if (pivot > chunkHeights.size.dec())
            this.pivot = chunkHeights.size.dec()

        invalidate()
    }

    fun pivot() = pivot
    fun end() = pivot >= chunkHeights.size.dec()

    fun selectSection() {
        ++sectionDividerCount

        when(sectionDividerCount) {
            1 -> {
                sectionDividerPosition1 = pivot
            }
            2 -> {
                sectionDividerPosition2 = pivot

                if (sectionDividerPosition1 > sectionDividerPosition2) {
                    val temp = sectionDividerPosition1
                    sectionDividerPosition1 = sectionDividerPosition2
                    sectionDividerPosition2 = temp
                }
            }
            else -> {
                sectionDividerCount = 0
                removeSectionDivider()
            }
        }
    }

    private fun removeSectionDivider() {
        sectionDividerPosition1 = sectionNotSelected
        sectionDividerPosition2 = sectionNotSelected
        invalidate()
    }

    private fun drawSection(canvas: Canvas, left: Int, right: Int) {
        sectionBackgroundRect.set(left, 0, right, height)
        canvas.drawRect(sectionBackgroundRect, sectionBackgroundPaint)
    }

    private fun drawTimestampBackground(canvas: Canvas) {
        timestampBackgroundRect.set(0, 0, width, (textHeight + timestampTextBottomPadding).toInt())
        canvas.drawRect(timestampBackgroundRect, timestampBackgroundPaint)
    }

    fun update(state: State) {
        this.state = state

        when(this.state) {
            State.DragWhilePlaying -> {
                allowDragWhilePlaying = true
            }
            State.Initialized -> {
                allowDragWhilePlaying = false
            }
            State.PausePlaying -> {
                allowDragWhilePlaying = false
            }
            State.PauseRecording -> {
                allowDragWhilePlaying = false
                overwrittenChunkHeights.clear()
                startOverwriting = 0
            }
            State.Playing -> {
                allowDragWhilePlaying = true
                if (pivot >= chunkHeights.size.dec())
                    pivot = 0
            }
            State.Recording -> {
                allowDragWhilePlaying = false
            }
            State.StopPlaying -> {
                allowDragWhilePlaying = false
            }
            State.StopRecording -> {
                allowDragWhilePlaying = false
                overwrittenChunkHeights.clear()
                startOverwriting = 0
            }
            State.OverwriteRecording -> {
                allowDragWhilePlaying = false
                overwrittenChunkHeights.clear()
                startOverwriting = pivot
            }
        }
    }

    fun clear() {
        chunkHeights.clear()
        pivot = 0
        invalidate()
        sectionDividerPosition1 = sectionNotSelected
        sectionDividerPosition2 = sectionNotSelected
        sectionDividerCount = 0
        invalidate()
    }

    fun elapsedTime() = pivot * updateIntervalMillis
    fun totalTime() = chunkHeights.size.dec() * updateIntervalMillis

    fun chunkCount() = chunkHeights.count()
    fun setPivot(pivot: Int) {
        this.pivot = pivot
    }

    private fun Float.isZero() = this == 0F
    private fun Int.isZero() = this == 0
    private fun Int.toPx(): Float {
        return this * Resources.getSystem().displayMetrics.density
    }

    private fun Long.toTimestampString(): String {
        return this.toDateFormat(TIMESTAMP_PATTERN)
    }

    private fun Float.smoothTransition(compareWith: Float, allowedDiff: Float, scaleFactor: Float): Float {
        if (scaleFactor.isZero())
            return this

        if (compareWith > this) {
            if (compareWith / this > allowedDiff) {
                val diff = this.coerceAtLeast(compareWith) - this.coerceAtMost(compareWith)
                return this + diff / scaleFactor
            }
        } else if (this > compareWith) {
            if (this / compareWith > allowedDiff) {
                val diff = this.coerceAtLeast(compareWith) - this.coerceAtMost(compareWith)
                return this - diff / scaleFactor
            }
        }

        return this
    }
}