package com.example.mypdfapp

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading : LiveData<Boolean> = _isLoading

    @SuppressLint("StaticFieldLeak")
    private lateinit var context : Context

    private lateinit var resolver : ContentResolver


    fun requestPDF(mainRepository: MainRepository, pdfUrl : String, pdfPassword : String?, context: Context){
        this.context = context
        this.resolver = context.contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val resultStream = mainRepository.makeRequest(pdfUrl)
                /* Working with itext7
                val document =  PdfDocument(
                    PdfReader(
                        resultStream,
                        pdfPassword.toByteArray(charset),
                        PdfWriter(out)
                ))
                document.close()*/

                val uri = savePdfDocument("newContract.pdf", "application/pdf")

                if (uri != null) {
                    val out = resolver.openOutputStream(uri)
                    val reader = PdfReader(resultStream)
                    val stamper = PdfStamper(reader, out)
                    val canvas = stamper.getOverContent(8)
                    val formater = SimpleDateFormat("dd/MM/yyyy")
                    val date = formater.format(Date())
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, Phrase(date),
                    50.0f, 406.0f, 0.0f )
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, Phrase("Marcos Uriel Trejo Velázquez"),
                        110.0f, 478.0f, 0.0f )
                    stamper.close()

                    _isLoading.postValue(false)

                }


            }catch (ex: Exception) {
                Log.e("error_saving_pdf", ex.message.toString())
                _isLoading.postValue(false)
            }
        }
    }

    private fun savePdfDocument(filename: String, mimeTye : String): Uri? {
        //save file
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeTye)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
}