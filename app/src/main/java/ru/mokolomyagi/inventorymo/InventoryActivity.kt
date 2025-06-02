package ru.mokolomyagi.inventorymo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.mokolomyagi.inventorymo.ui.inventory.ImageAdapter
import ru.mokolomyagi.inventorymo.utilities.QRAnalyzer
import ru.mokolomyagi.inventorymo.utilities.QRDataRequest
import ru.mokolomyagi.inventorymo.utilities.QRResponseModel
import ru.mokolomyagi.inventorymo.utilities.QRSenderService
import ru.mokolomyagi.inventorymo.utilities.ServiceBuilder
import java.util.concurrent.Executors

class InventoryActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var qrTextView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analyzerResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Виджет просмотра и текстовое представление результата
        previewView = findViewById(R.id.previewView)
        qrTextView = findViewById(R.id.qrTextView)

        // Свайп вниз для обновления экрана
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            QRAnalyzer.Companion.isPaused = false // Снимаем флаг паузы после свайпа вниз
            restartCamera()
        }

        // Проверка прав доступа к камере
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Перезапуск камеры (после свайпа вниз)
     */
    private fun restartCamera() {
        stopCamera()
        displayQRData(getString(R.string.waiting_for_recognition))
        startCamera()
        swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Остановка камеры и очистка ресурсов
     */
    private fun stopCamera() {
        camera?.let { cam ->
            cameraProvider?.unbindAll()
            camera = null
        }
    }

    /**
     * Старт камеры
     */
    private fun startCamera() {
        stopCamera() // Останавливаем предыдущее состояние камеры

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            this.cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        QRAnalyzer(::sendQRData, ::displayQRData)
                    ) // Передаем два колбэка
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            this.camera = cameraProvider!!.bindToLifecycle(this, cameraSelector, preview, analyzer)

            // Индикатор обновления после запуска камеры
            runOnUiThread {
                swipeRefreshLayout.isRefreshing = false
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Колбэк для обновления UI
    private fun displayQRData(message: String) {
        runOnUiThread {
            analyzerResult = message
            qrTextView.text = message
        }
    }

    /**
     * Запрашивает разрешение на использование камеры
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera()
            else Toast.makeText(this, "Разрешите доступ к камере", Toast.LENGTH_LONG).show()
        }

    /**
     * Проверяет наличие разрешений
     */
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Отправка QR-данных на сервер
     */
    private fun sendQRData(qrData: String) {
        try {
            val service = ServiceBuilder.createService<QRSenderService>()
            val call = service.sendQRData(QRDataRequest(qrData))

            call.enqueue(object : Callback<QRResponseModel> {
                override fun onResponse(
                    call: Call<QRResponseModel>,
                    response: Response<QRResponseModel>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!

                        // Показываем диалоговое окно с результатом
                        showImageCarouselDialog(serverResponse)
                        Log.d("QR", "onResponse: $serverResponse")
                    } else if (response.code() == 404) {
                        showErrorDialog(response, qrData) // Показываем диалог при ошибке 404
                    } else {
                        Log.e(
                            "QR",
                            "Ошибка отправки: ${response.code()} ${
                                response.errorBody()?.string() ?: ""
                            }"
                        )
                    }
                }

                override fun onFailure(call: Call<QRResponseModel>, t: Throwable) {
                    Log.e("QR", "Ошибка сети: ${t.localizedMessage}", t)
                }
            })
        } catch (e: Exception) {
            Log.e("QR", "Ошибка сети: ${e.localizedMessage}", e)
        }
    }

    /**
     * Метод для показа диалогового окна при ошибке 404
     */
    private fun showErrorDialog(response: Response<*>, qrData: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка 404")
            .setMessage("Ресурс не найден. Желаете повторить попытку?")
            .setPositiveButton("Повторить") { dialog, which ->
                // Перезапустить отправку данных
                CoroutineScope(Dispatchers.IO).launch {
                    sendQRData(qrData)
                }
            }
            .setNegativeButton("Закрыть") { dialog, which ->
                // СНИМАЕМ ПАУЗУ
                QRAnalyzer.Companion.isPaused = false
                dialog.dismiss()
                restartCamera()
            }
            .create()
            .show()
    }

    private fun showImageCarouselDialog(serverResponse: QRResponseModel) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_image_list, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.imageRecyclerView)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager

        val images = serverResponse.images

        val adapter = ImageAdapter(images)
        recyclerView.adapter = adapter

        // Начать с середины списка — создаёт эффект бесконечности
        val startIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % images.size)
        recyclerView.scrollToPosition(startIndex)

        AlertDialog.Builder(this)
            .setTitle(serverResponse.title)
            .setMessage(analyzerResult)
            .setView(view)
            .setMessage(serverResponse.description)
            .setPositiveButton("Закрыть") { dialog, _ ->
                QRAnalyzer.Companion.isPaused = false
                dialog.dismiss()
                restartCamera()
            }
            .show()
    }
}