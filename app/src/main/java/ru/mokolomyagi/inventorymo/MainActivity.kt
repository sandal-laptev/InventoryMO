package ru.mokolomyagi.inventorymo

import android.content.Intent
import android.os.Bundle
import com.google.android.material.button.MaterialButton

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<MaterialButton>(R.id.button_camera)
        val inventoryButton = findViewById<MaterialButton>(R.id.button_inventory)

        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        inventoryButton.setOnClickListener {
            val intent = Intent(this, InventoryActivity::class.java)
            startActivity(intent)
        }
    }
}