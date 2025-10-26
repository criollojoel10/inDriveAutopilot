package dev.joel.indriveautopilot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout del sistema para compilar sin XML propio:
        setContentView(android.R.layout.simple_list_item_1)
        title = "InDrive Autopilot"
    }
}