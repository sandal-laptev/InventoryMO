package ru.mokolomyagi.inventorymo.utilities

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalGetImage::class)
internal class QRAnalyzer(
    private val onSendCallback: (String) -> Unit,
    private val onDisplayCallback: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused) return // Пропускаем анализ, если камера приостановлена

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Операции с распознаванием баркодов
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            val type = barcode.format
                            Log.d("QR", "QR: $value")

                            // Форматируем вывод в зависимости от типа баркода
                            val displayMessage = when (barcode.format) {
                                Barcode.FORMAT_QR_CODE -> "QR-код: $value"
                                Barcode.FORMAT_UPC_A, Barcode.FORMAT_EAN_13 -> "Штрихкод ($type): $value"
                                else -> "Неизвестный тип: $value"
                            }

                            // Обновляем UI и отправляем данные на сервер
                            onDisplayCallback(displayMessage)
                            onSendCallback(value)

                            // ПАУЗА ПОСЛЕ УСПЕШНОГО РАСПОЗНАВАНИЯ
                            isPaused = true
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("QR", "Ошибка: ${it.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    companion object {
        var isPaused = false // Флаг для паузы камеры после распознавания
    }
}