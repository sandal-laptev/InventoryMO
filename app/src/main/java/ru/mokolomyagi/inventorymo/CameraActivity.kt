package ru.mokolomyagi.inventorymo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ru.mokolomyagi.inventorymo.ui.camera.PhotoGalleryAdapter
import ru.mokolomyagi.inventorymo.ui.camera.PhotoPagerAdapter
import ru.mokolomyagi.inventorymo.utilities.OrientationUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var galleryRecyclerView: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerBackground: View

    private lateinit var imageCapture: ImageCapture
    private val imageUris = mutableListOf<Uri>()
    private lateinit var galleryAdapter: PhotoGalleryAdapter
    private lateinit var pagerAdapter: PhotoPagerAdapter

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var orientationListener: OrientationEventListener
    private var currentRotation: Int = Surface.ROTATION_0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.camera_preview)
        captureButton = findViewById(R.id.capture_button)
        sendButton = findViewById(R.id.send_button)
        galleryRecyclerView = findViewById(R.id.photo_gallery)
        viewPager = findViewById(R.id.fullscreen_pager)
        viewPagerBackground = findViewById(R.id.fullscreen_background)

        galleryAdapter = PhotoGalleryAdapter(
            imageUris,
            onPhotoClick = { uri ->
                showPhotoFullscreen(uri)
            },
            onDeleteClick = { uri ->
                val index = imageUris.indexOf(uri)
                if (index != -1) {
                    imageUris.removeAt(index)
                    galleryAdapter.notifyItemRemoved(index)
                    if (imageUris.isEmpty()) {
                        galleryAdapter.setEditMode(false)
                    }
                    updateUiState()
                }
            }
        )

        galleryRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        galleryRecyclerView.adapter = galleryAdapter

        pagerAdapter = PhotoPagerAdapter(imageUris) {
            hidePhotoFullscreen()
        }
        viewPager.adapter = pagerAdapter

        viewPagerBackground.setOnClickListener {
            hidePhotoFullscreen()
        }

        viewPager.visibility = View.GONE
        viewPagerBackground.visibility = View.GONE

        captureButton.setOnClickListener { takePhoto() }
        sendButton.setOnClickListener {
            Toast.makeText(this, "Отправка ${imageUris.size} фото", Toast.LENGTH_SHORT).show()
        }

        sendButton.visibility = View.GONE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewPager.isVisible -> {
                        hidePhotoFullscreen()
                    }

                    galleryAdapter.isInEditMode() -> {
                        galleryAdapter.toggleEditMode()
                    }

                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        setupOrientationListener()
    }

    private fun setupOrientationListener() {
        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val surfaceRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                val surRotation = when (orientation) {
                    in 45..134 -> 270
                    in 135..224 -> 180
                    in 225..314 -> 90
                    else -> 0
                }

                if (currentRotation != surfaceRotation) {
                    currentRotation = surfaceRotation
                }

                val rotation = OrientationUtils.getRotationWithHysteresis(orientation)
                galleryAdapter.setThumbnailRotation(surRotation)
            }
        }

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (::imageCapture.isInitialized && imageUris.size < 5) {
            val photoFile = File(
                externalCacheDir,
                "photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.targetRotation = currentRotation // <-- установка текущего поворота

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                        cropToSquare(photoFile)

                        val uri = FileProvider.getUriForFile(
                            this@CameraActivity,
                            "${packageName}.fileprovider",
                            photoFile
                        )

                        imageUris.add(uri)
                        galleryAdapter.notifyItemInserted(imageUris.lastIndex)
                        pagerAdapter.notifyItemInserted(imageUris.lastIndex)
                        galleryRecyclerView.scrollToPosition(imageUris.lastIndex)
                        updateUiState()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            applicationContext,
                            "Ошибка при съёмке: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    fun cropToSquare(imageFile: File): File {
        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val size = minOf(originalBitmap.width, originalBitmap.height)
        val xOffset = (originalBitmap.width - size) / 2
        val yOffset = (originalBitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, size, size)

        // Перезаписать файл
        imageFile.outputStream().use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        return imageFile
    }

    private fun updateUiState() {
        captureButton.isEnabled = imageUris.size < 5
        captureButton.alpha = if (captureButton.isEnabled) 1f else 0.5f
        sendButton.visibility = if (imageUris.size >= 4) View.VISIBLE else View.GONE
    }

    private fun showPhotoFullscreen(uri: Uri) {
        val index = imageUris.indexOf(uri)
        if (index != -1) {
            viewPager.setCurrentItem(index, false)
            viewPager.visibility = View.VISIBLE
            viewPagerBackground.visibility = View.VISIBLE
            viewPagerBackground.alpha = 0f
            viewPagerBackground.animate().alpha(1f).setDuration(500).start()
        }
    }

    private fun hidePhotoFullscreen() {
        viewPagerBackground.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction {
                viewPager.visibility = View.GONE
                viewPagerBackground.visibility = View.GONE
            }
            .start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                takePhoto()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationListener.disable()
        cameraExecutor.shutdown()
    }
}
