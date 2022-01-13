package com.example.mypdfapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class MainRepository {

    suspend fun makeRequest(myUrl : String) : InputStream? {

        var inputStream: InputStream? = null

            return withContext(Dispatchers.IO) {
                val url = URL(myUrl)
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    inputStream = BufferedInputStream(urlConnection.inputStream)
                } catch (e: Exception) {
                    Log.d("error_connection", e.message.toString())
                }
                inputStream
            }
    }
}