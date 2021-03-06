package com.freeankit.resizable

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Build
import android.text.*
import android.text.method.SingleLineTransformationMethod
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 11/12/2017 (MM/DD/YYYY )
 */
class ResizableHelper(view: TextView) {


    // Minimum size of the text in pixels
    private val DEFAULT_MIN_TEXT_SIZE = 8 //sp
    // How precise we want to be when reaching the target textWidth size
    private val DEFAULT_PRECISION = 0.5f

    companion object {
        private val TAG = "ResizableTextHelper"
        private val SPEW = false

        /**
         * Creates a new instance of `ResizableHelper` that wraps a [TextView] and enables
         * automatically sizing the text to fit.
         */
        fun create(view: TextView): ResizableHelper {
            return create(view, null, 0)
        }

        /**
         * Creates a new instance of `ResizableHelper` that wraps a [TextView] and enables
         * automatically sizing the text to fit.
         */
        fun create(view: TextView, attrs: AttributeSet): ResizableHelper {
            return create(view, attrs, 0)
        }

        /**
         * Creates a new instance of `ResizableHelper` that wraps a [TextView] and enables
         * automatically sizing the text to fit.
         */
        fun create(view: TextView, attrs: AttributeSet?, defStyle: Int): ResizableHelper {
            val helper = ResizableHelper(view)
            var sizeToFit = true
            if (attrs != null) {
                val context = view.context
                var minTextSize = helper.getMinTextSize() as Int
                var precision = helper.getPrecision()

                val ta = context.obtainStyledAttributes(
                        attrs,
                        R.styleable.ResizableEditText,
                        defStyle,
                        0)
                sizeToFit = ta.getBoolean(R.styleable.ResizableEditText_sizeToFit, sizeToFit)
                minTextSize = ta.getDimensionPixelSize(R.styleable.ResizableEditText_minTextSize,
                        minTextSize)
                precision = ta.getFloat(R.styleable.ResizableEditText_precision, precision)
                ta.recycle()

                helper.setMinTextSize(TypedValue.COMPLEX_UNIT_PX, minTextSize.toFloat())
                        .setPrecision(precision)
            }
            helper.setEnabled(sizeToFit)

            return helper
        }

        /**
         * Re-sizes the textSize of the TextView so that the text fits within the bounds of the View.
         */
        private fun Resizablee(view: TextView, paint: TextPaint, minTextSize: Float, maxTextSize: Float, maxLines: Int, precision: Float) {
            if (maxLines <= 0 || maxLines == Integer.MAX_VALUE) {
                // Don't auto-size since there's no limit on lines.
                return
            }

            val targetWidth = view.width - view.paddingLeft - view.paddingRight
            if (targetWidth <= 0) {
                return
            }

            var text = view.text
            val method = view.transformationMethod
            if (method != null) {
                text = method.getTransformation(text, view)
            }

            val context = view.context
            var r = Resources.getSystem()
            val displayMetrics: DisplayMetrics

            var size = maxTextSize
            val high = size
            val low = 0f

            if (context != null) {
                r = context.resources
            }
            displayMetrics = r.displayMetrics

            paint.set(view.paint)
            paint.textSize = size

            if (maxLines == 1 && paint.measureText(text, 0, text.length) > targetWidth || getLineCount(text, paint, size, targetWidth.toFloat(), displayMetrics) > maxLines) {
                size = getResizableTextSize(text, paint, targetWidth.toFloat(), maxLines, low, high, precision,
                        displayMetrics)
            }

            if (size < minTextSize) {
                size = minTextSize
            }

            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        }

        /**
         * Recursive binary search to find the best size for the text.
         */
        private fun getResizableTextSize(text: CharSequence, paint: TextPaint,
                                         targetWidth: Float, maxLines: Int, low: Float, high: Float, precision: Float,
                                         displayMetrics: DisplayMetrics): Float {
            val mid = (low + high) / 2.0f
            var lineCount = 1
            var layout: StaticLayout? = null

            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mid,
                    displayMetrics)

            if (maxLines != 1) {
                layout = StaticLayout(text, paint, targetWidth.toInt(), Layout.Alignment.ALIGN_NORMAL,
                        1.0f, 0.0f, true)
                lineCount = layout.lineCount
            }

            if (SPEW)
                Log.d(TAG, "low=" + low + " high=" + high + " mid=" + mid +
                        " target=" + targetWidth + " maxLines=" + maxLines + " lineCount=" + lineCount)

            if (lineCount > maxLines) {
                // For the case that `text` has more newline characters than `maxLines`.
                return if (high - low < precision) {
                    low
                } else getResizableTextSize(text, paint, targetWidth, maxLines, low, mid, precision,
                        displayMetrics)
            } else if (lineCount < maxLines) {
                return getResizableTextSize(text, paint, targetWidth, maxLines, mid, high, precision,
                        displayMetrics)
            } else {
                var maxLineWidth = 0f
                if (maxLines == 1) {
                    maxLineWidth = paint.measureText(text, 0, text.length)
                } else {
                    for (i in 0 until lineCount) {
                        if (layout!!.getLineWidth(i) > maxLineWidth) {
                            maxLineWidth = layout.getLineWidth(i)
                        }
                    }
                }

                return if (high - low < precision) {
                    low
                } else if (maxLineWidth > targetWidth) {
                    getResizableTextSize(text, paint, targetWidth, maxLines, low, mid, precision,
                            displayMetrics)
                } else if (maxLineWidth < targetWidth) {
                    getResizableTextSize(text, paint, targetWidth, maxLines, mid, high, precision,
                            displayMetrics)
                } else {
                    mid
                }
            }
        }

        private fun getLineCount(text: CharSequence, paint: TextPaint, size: Float, width: Float,
                                 displayMetrics: DisplayMetrics): Int {
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size,
                    displayMetrics)
            val layout = StaticLayout(text, paint, width.toInt(),
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true)
            return layout.lineCount
        }

        @SuppressLint("ObsoleteSdkInt")
        private fun getMaxLines(view: TextView): Int {
            var maxLines = -1 // No limit (Integer.MAX_VALUE also means no limit)

            val method = view.transformationMethod
            if (method != null && method is SingleLineTransformationMethod) {
                maxLines = 1
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // setMaxLines() and getMaxLines() are only available on android-16+
                maxLines = view.maxLines
            }

            return maxLines
        }
    }
    // Attributes

    private var mTextView: TextView
    private var mPaint: TextPaint
    /**
     * Original textSize of the TextView.
     */
    private var mTextSize: Float = 0.toFloat()

    private var mMaxLines: Int = 0
    private var mMinTextSize: Float = 0.toFloat()
    private var mMaxTextSize: Float = 0.toFloat()
    private var mPrecision: Float = 0.toFloat()

    private var mEnabled: Boolean = false
    private var mIsResizableting: Boolean = false

    private var mListeners: ArrayList<OnTextSizeChangeListener>? = null

    private val mTextWatcher = ResizableTextWatcher()

    private val mOnLayoutChangeListener = ResizableOnLayoutChangeListener()

    init {
        val context = view.context
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        mTextView = view
        mPaint = TextPaint()
        setRawTextSize(view.textSize)

        mMaxLines = getMaxLines(view)
        mMinTextSize = scaledDensity * DEFAULT_MIN_TEXT_SIZE
        mMaxTextSize = mTextSize
        mPrecision = DEFAULT_PRECISION
    }

    /**
     * Adds an [OnTextSizeChangeListener] to the list of those whose methods are called
     * whenever the [TextView]'s `textSize` changes.
     */
    fun addOnTextSizeChangeListener(listener: OnTextSizeChangeListener): ResizableHelper {
        if (mListeners == null) {
            mListeners = ArrayList()
        }
        mListeners!!.add(listener)
        return this
    }

    /**
     * Removes the specified [OnTextSizeChangeListener] from the list of those whose methods
     * are called whenever the [TextView]'s `textSize` changes.
     */
    fun removeOnTextSizeChangeListener(listener: OnTextSizeChangeListener): ResizableHelper {
        if (mListeners != null) {
            mListeners!!.remove(listener)
        }
        return this
    }

    /**
     * Returns the amount of precision used to calculate the correct text size to fit within its
     * bounds.
     */
    fun getPrecision(): Float {
        return mPrecision
    }

    /**
     * Set the amount of precision used to calculate the correct text size to fit within its
     * bounds. Lower precision is more precise and takes more time.
     *
     * @param precision The amount of precision.
     */
    fun setPrecision(precision: Float): ResizableHelper {
        if (mPrecision != precision) {
            mPrecision = precision

            Resizablee()
        }
        return this
    }

    /**
     * Returns the minimum size (in pixels) of the text.
     */
    fun getMinTextSize(): Float {
        return mMinTextSize
    }

    /**
     * Set the minimum text size to the given value, interpreted as "scaled pixel" units. This size
     * is adjusted based on the current density and user font size preference.
     *
     * @param size The scaled pixel size.
     * @attr ref me.grantland.R.styleable#ResizableEditText_minTextSize
     */
    fun setMinTextSize(size: Float): ResizableHelper {
        return setMinTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * Set the minimum text size to a given unit and value. See TypedValue for the possible
     * dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     * @attr ref me.grantland.R.styleable#ResizableEditText_minTextSize
     */
    fun setMinTextSize(unit: Int, size: Float): ResizableHelper {
        val context = mTextView.context
        var r = Resources.getSystem()

        if (context != null) {
            r = context.resources
        }

        setRawMinTextSize(TypedValue.applyDimension(unit, size.toFloat(), r.displayMetrics))
        return this
    }

    private fun setRawMinTextSize(size: Float) {
        if (size != mMinTextSize) {
            mMinTextSize = size

            Resizablee()
        }
    }

    /**
     * Returns the maximum size (in pixels) of the text.
     */
    fun getMaxTextSize(): Float {
        return mMaxTextSize
    }

    /**
     * Set the maximum text size to the given value, interpreted as "scaled pixel" units. This size
     * is adjusted based on the current density and user font size preference.
     *
     * @param size The scaled pixel size.
     * @attr ref android.R.styleable#TextView_textSize
     */
    fun setMaxTextSize(size: Float): ResizableHelper {
        return setMaxTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * Set the maximum text size to a given unit and value. See TypedValue for the possible
     * dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     * @attr ref android.R.styleable#TextView_textSize
     */
    fun setMaxTextSize(unit: Int, size: Float): ResizableHelper {
        val context = mTextView.context
        var r = Resources.getSystem()

        if (context != null) {
            r = context.resources
        }

        setRawMaxTextSize(TypedValue.applyDimension(unit, size, r.displayMetrics))
        return this
    }

    private fun setRawMaxTextSize(size: Float) {
        if (size != mMaxTextSize) {
            mMaxTextSize = size

            Resizablee()
        }
    }

    /**
     * @see TextView.getMaxLines
     */
    fun getMaxLines(): Int {
        return mMaxLines
    }

    /**
     * @see TextView.setMaxLines
     */
    fun setMaxLines(lines: Int): ResizableHelper {
        if (mMaxLines != lines) {
            mMaxLines = lines

            Resizablee()
        }
        return this
    }

    /**
     * Returns whether or not automatically resizing text is enabled.
     */
    fun isEnabled(): Boolean {
        return mEnabled
    }

    /**
     * Set the enabled state of automatically resizing text.
     */
    fun setEnabled(enabled: Boolean): ResizableHelper {
        if (mEnabled != enabled) {
            mEnabled = enabled

            if (enabled) {
                mTextView.addTextChangedListener(mTextWatcher)
                mTextView.addOnLayoutChangeListener(mOnLayoutChangeListener)

                Resizablee()
            } else {
                mTextView.removeTextChangedListener(mTextWatcher)
                mTextView.removeOnLayoutChangeListener(mOnLayoutChangeListener)

                mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize)
            }
        }
        return this
    }

    /**
     * Returns the original text size of the View.
     *
     * @see TextView.getTextSize
     */
    fun getTextSize(): Float {
        return mTextSize
    }

    /**
     * Set the original text size of the View.
     *
     * @see TextView.setTextSize
     */
    fun setTextSize(size: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * Set the original text size of the View.
     *
     * @see TextView.setTextSize
     */
    fun setTextSize(unit: Int, size: Float) {
        if (mIsResizableting) {
            // We don't want to update the TextView's actual textSize while we're Resizableting
            // since it'd get set to the ResizableTextSize
            return
        }
        val context = mTextView.context
        var r = Resources.getSystem()

        if (context != null) {
            r = context.resources
        }

        setRawTextSize(TypedValue.applyDimension(unit, size, r.displayMetrics))
    }

    private fun setRawTextSize(size: Float) {
        if (mTextSize != size) {
            mTextSize = size
        }
    }

    private fun Resizablee() {
        val oldTextSize = mTextView.textSize
        val textSize: Float = mTextView.textSize

        mIsResizableting = true
        Resizablee(mTextView, mPaint, mMinTextSize, mMaxTextSize, mMaxLines, mPrecision)
        mIsResizableting = false

        if (textSize != oldTextSize) {
            sendTextSizeChange(textSize, oldTextSize)
        }
    }

    private fun sendTextSizeChange(textSize: Float, oldTextSize: Float) {
        if (mListeners == null) {
            return
        }

        for (listener in mListeners!!) {
            listener.onTextSizeChange(textSize, oldTextSize)
        }
    }

    private inner class ResizableTextWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
            // do nothing
        }

        override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
            Resizablee()
        }

        override fun afterTextChanged(editable: Editable) {
            // do nothing
        }
    }

    private inner class ResizableOnLayoutChangeListener : View.OnLayoutChangeListener {
        override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int,
                                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            Resizablee()
        }
    }

    /**
     * When an object of a type is attached to an `ResizableHelper`, its methods will be called
     * when the `textSize` is changed.
     */
    interface OnTextSizeChangeListener {
        /**
         * This method is called to notify you that the size of the text has changed to
         * `textSize` from `oldTextSize`.
         */
        fun onTextSizeChange(textSize: Float, oldTextSize: Float)
    }
}