package com.example.picturemarker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.picturemarker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImages.setOnClickListener {
            startActivity(Intent(this, ImageSelectionActivity::class.java))
        }
    }

    companion object {
        const val REQUEST_CODE_SELECT_IMAGES = 1001
    }
}
