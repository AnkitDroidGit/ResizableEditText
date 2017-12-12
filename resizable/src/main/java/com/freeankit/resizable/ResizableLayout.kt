package com.freeankit.resizable

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import java.util.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 12/12/2017 (MM/DD/YYYY )
 */
class ResizableLayout constructor(context: Context, attrs: AttributeSet, defStyle: Int) : FrameLayout(context, attrs, defStyle) {
    private var mEnabled: Boolean = false
    private var mMinTextSize: Float = 0.toFloat()
    private var mPrecision: Float = 0.toFloat()
    private val mHelpers = WeakHashMap<View, ResizableHelper>()

    init {
        var sizeToFit = true
        var minTextSize = -1
        var precision = -1f

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

        mEnabled = sizeToFit
        mMinTextSize = minTextSize.toFloat()
        mPrecision = precision
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        val textView = child as TextView
        val helper = ResizableHelper.create(textView)
                .setEnabled(mEnabled)
        if (mPrecision > 0) {
            helper.setPrecision(mPrecision)
        }
        if (mMinTextSize > 0) {
            helper.setMinTextSize(TypedValue.COMPLEX_UNIT_PX, mMinTextSize)
        }
        mHelpers.put(textView, helper)
    }

    /**
     * Returns the [ResizableHelper] for this child View.
     */
    fun getResizableHelper(textView: TextView): ResizableHelper? {
        return mHelpers[textView]
    }

    /**
     * Returns the [ResizableHelper] for this child View.
     */
    fun getResizableHelper(index: Int): ResizableHelper? {
        return mHelpers[getChildAt(index)]
    }
}