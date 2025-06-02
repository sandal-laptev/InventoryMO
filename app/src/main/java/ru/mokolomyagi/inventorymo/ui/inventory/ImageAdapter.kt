package ru.mokolomyagi.inventorymo.ui.inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.mokolomyagi.inventorymo.R

class ImageAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    // Inner class для представления ячейки (item) в RecyclerView
    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ищем в itemView элемент ImageView, который будет отображать изображение
        val imageView: ImageView = view.findViewById(R.id.itemImageView)
    }

    // Метод создает ячейку (view holder) для каждой позиции
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        // Создаем новый элемент View с использованием LayoutInflater из R.layout.item_image
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        // Возвращаем новый ImageViewHolder с созданным элементом
        return ImageViewHolder(view)
    }

    // Метод связывает данные с конкретным элементом на позиции
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Реальная позиция берется по остатку от деления на длину массива imageUrls
        // Это гарантирует бесконечную прокрутку, когда список будет повторять свои элементы
        val realPosition = position % imageUrls.size
        // Загружаем изображение из URL в ImageView с помощью Glide
        Glide.with(holder.imageView.context)
            .load(imageUrls[realPosition]) // Загружаем изображение по соответствующему URL
            .into(holder.imageView)        // Записываем изображение в ImageView
    }

    // Количество элементов в списке. Здесь установлено максимальное целое число (Int.MAX_VALUE)
    // Это создаёт иллюзию бесконечности, фактически дублируя одни и те же изображения многократно
    override fun getItemCount(): Int = Int.MAX_VALUE
}