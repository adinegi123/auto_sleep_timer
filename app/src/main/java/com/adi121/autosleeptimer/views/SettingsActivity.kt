package com.adi121.autosleeptimer.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.adi121.autosleeptimer.databinding.ActivitySettingsBinding
import com.adi121.autosleeptimer.utils.SharedPrefs

class SettingsActivity : AppCompatActivity() {

    private val sharedPrefs by lazy {
        SharedPrefs.getInstance(this)
    }

    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchQuote.isChecked = sharedPrefs?.iSQuoteEnabled ?: true

        binding.switchQuote.setOnCheckedChangeListener { p0, p1 ->
            sharedPrefs?.iSQuoteEnabled = p1
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvRateApp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("https://play.google.com/store/apps/details?id=com.adi121.autosleeptimer")
            startActivity(intent)
        }


    }


}