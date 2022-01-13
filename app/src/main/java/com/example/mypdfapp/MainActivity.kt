package com.example.mypdfapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loader : ProgressBar = findViewById(R.id.loader)
        val mainRepository = MainRepository()
        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.requestPDF(mainRepository, "http://192.168.0.7:8888/cv_protected.pdf", "libe291209", this@MainActivity)

        viewModel.isLoading.observe(this, {
            if(it){
                loader.visibility = View.VISIBLE
            }else{
                loader.visibility = View.GONE
            }
        })
    }
}