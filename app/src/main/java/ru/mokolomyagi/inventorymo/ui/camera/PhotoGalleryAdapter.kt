package ru.mokolomyagi.inventorymo.ui.camera

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import ru.mokolomyagi.inventorymo.R

class PhotoGalleryAdapter(
    private val imageUris: List<Uri>,
    private val onPhotoClick: (Uri) -> Unit,
    private val onDeleteClick: (Uri) -> Unit
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    private var isEditMode = false
    private var targetRotation = 0

    fun isInEditMode(): Boolean = isEditMode

    fun toggleEditMode() {
        isEditMode = !isEditMode
        notifyDataSetChanged()
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode != enabled) {
            isEditMode = enabled
            notifyDataSetChanged()
        }
    }

    fun setThumbnailRotation(degrees: Int) {
        if (targetRotation != degrees) {
            targetRotation = degrees
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        Log.d("GALLERY", "*** onBindViewHolder: position = $position ***")
        val uri = imageUris[position]

        val currentRotation = holder.getCurrentRotation()

        Log.d("GALLERY", "onBindViewHolder: targetRotation = $targetRotation")
        Log.d("GALLERY", "onBindViewHolder: currentRotation = $currentRotation")

        Glide.with(holder.thumbnailPhoto.context)
            .load(uri)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(20))) // 20 — радиус в px
            .into(holder.thumbnailPhoto)

        var delta = targetRotation - currentRotation
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f

        Log.d("GALLERY", "onBindViewHolder: delta = $delta")

        holder.containerPhoto.animate()
            .rotationBy(delta)
            .setDuration(500)
            .start()

        holder.setCurrentRotation(targetRotation.toFloat())

        Log.d(
            "GALLERY",
            "onBindViewHolder: holder.currentRotation = ${(currentRotation + delta + 360f) % 360f} \n"
        )

        holder.thumbnailPhoto.setOnClickListener {
            if (!isEditMode) {
                onPhotoClick(uri)
            }
        }

        holder.thumbnailPhoto.setOnLongClickListener {
            toggleEditMode()
            true
        }

        holder.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
        holder.deleteButton.setOnClickListener {
            onDeleteClick(uri)
        }
    }

    override fun getItemCount(): Int = imageUris.size

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val containerPhoto: ConstraintLayout = view.findViewById(R.id.photo_container)
        val thumbnailPhoto: ImageView = view.findViewById(R.id.photo_thumbnail)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        private var currentRotation: Float = 0f

        fun getCurrentRotation(): Float {
            return currentRotation
        }

        fun setCurrentRotation(currentRotation: Float) {
            this.currentRotation = currentRotation
        }
    }
}
