package ru.mokolomyagi.inventorymo.ui.inventory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup

class OverlayView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val parentWidth = width
        val parentHeight = height

        // Размер рамки 25% от общего экрана
        val frameWidth = parentWidth / 2
        val frameHeight = parentWidth / 2

        // Центр рамки совпадает с центром экрана
        val left = (parentWidth - frameWidth) / 2
        val top = (parentHeight - frameHeight) / 2

        canvas.drawRect(left.toFloat(), top.toFloat(), (left + frameWidth).toFloat(), (top + frameHeight).toFloat(), paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
}