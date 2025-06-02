package ru.mokolomyagi.inventorymo.ui.camera

import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ortiz.touchview.TouchImageView
import ru.mokolomyagi.inventorymo.R
import kotlin.math.abs

class PhotoPagerAdapter(
    private val photoUris: List<Uri>,
    private val onSwipeDown: () -> Unit
) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: TouchImageView = view.findViewById(R.id.zoomable_photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_zoomable_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val imageView = holder.imageView

        // Загружаем изображение с помощью Glide
        Glide.with(imageView.context)
            .load(photoUris[position])
            .override(1024, 1024) // ограничение размера изображения
            .fitCenter()
            .into(imageView)

        var startX = 0f
        var startY = 0f
        val swipeThreshold = 150 // px

        imageView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - startX
                    val deltaY = event.y - startY

                    // если свайп по горизонтали — разрешаем ViewPager перехватывать
                    if (abs(deltaX) > abs(deltaY)) {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    } else {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    false // не перехватываем, чтобы зум работал
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - startX
                    val deltaY = event.y - startY

                    if (deltaY > swipeThreshold && abs(deltaY) > abs(deltaX)) {
                        onSwipeDown()
                        true
                    } else {
                        v.performClick()
                        false
                    }
                }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = photoUris.size
}
