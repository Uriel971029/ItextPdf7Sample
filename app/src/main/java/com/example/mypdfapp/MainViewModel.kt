package com.example.mypdfapp

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class MainViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading : LiveData<Boolean> = _isLoading

    fun requestPDF(mainRepository: MainRepository, pdfUrl : String, pdfPassword : String, context: Context){
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {

            try {

                val resultStream = mainRepository.makeRequest(pdfUrl)
                val out = ByteArrayOutputStream()
                val charset = Charsets.UTF_8

                val document =  PdfDocument(
                    PdfReader(
                        resultStream,
                        ReaderProperties().setPassword(pdfPassword.toByteArray(charset))),
                        PdfWriter(out))

                document.close()

                //val textFromPage = PdfTextExtractor.getTextFromPage(document.firstPage)
                //Log.d("extracto", textFromPage)

                //save file
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "adios.pdf")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { output ->
                        val newPdfStream = ByteArrayInputStream(out.toByteArray())
                        newPdfStream.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                    }
                }

                _isLoading.postValue(false)
            }catch (ex: Exception) {
                Log.e("error_reading_pdf", ex.message.toString())
                _isLoading.postValue(false)
            }
        }


    }
}