package com.example.mypdfapp

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading : LiveData<Boolean> = _isLoading

    private val _pdfPage = MutableLiveData<Bitmap>()
    val pdfPage : LiveData<Bitmap> = _pdfPage

    private lateinit var context: Context


    fun requestPDF(mainRepository: MainRepository, pdfUrl : String, pdfPassword : String, context: Context){
        _isLoading.value = true
        this.context = context
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val resultStream = mainRepository.makeRequest(pdfUrl)
                val filename = "contract.pdf"
                val fullClientName = "Marcos Uriel Trejo Velázquez"
                val formatter = SimpleDateFormat.getDateInstance()
                val currentDate = formatter.format(Date())

                val uri = savePdfDocument(filename)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri).use { output ->
                        resultStream?.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                    }
                }
                //Edit pdf document and insert client data as well as current date
                val coordinates = arrayListOf(Coordinates(10.0f, 7.0f),
                    Coordinates(27.0f, 5.75f))
                val textList = arrayListOf(fullClientName, currentDate)
                //val desiredScale = getDocumentDetails(resultStream!!)
                writeInPdfDocument(uri!!, 7,coordinates, textList)

                _isLoading.postValue(false)

            }catch (ex: Exception) {
                Log.e("error_reading_pdf", ex.message.toString())
                _isLoading.postValue(false)
            }
        }
    }


    private fun savePdfDocument(filename: String): Uri? {
        //save file
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }

    //Method to write over the bitmap that represents the pdf page selected
    //arguments:
    //uri -> document
    //coordinates -> x and y position to put the text into the document
    //mInfo -> list of strings that will be put into the document
    private fun writeInPdfDocument(uri: Uri, selectedPage : Int, coordinates : List<Coordinates>, mInfo : List<String>) {
        val newDocument = PdfDocument()
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = PdfRenderer(parcelFileDescriptor!!)
        val scale: Float = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until renderer.pageCount) {
            val page: PdfRenderer.Page = renderer.openPage(i)
            val mBitmap = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            if (i == selectedPage) {
                val canvas = Canvas(mBitmap)
                paint.color = Color.rgb(0, 0, 0)
                // text size in pixels
                paint.textSize = (5 * scale)
                val bounds = Rect()

                for (j in mInfo.indices) {
                    paint.getTextBounds(mInfo[j], 0, mInfo[j].length, bounds)
                    val x = (mBitmap.width - bounds.width()) / coordinates[j].xPosition
                    val y = (mBitmap.height + bounds.height()) / coordinates[j].yPosition
                    canvas.drawText(mInfo[j], x * scale, y * scale, paint)
                }

                //pass pdf page to the view
                _pdfPage.postValue(mBitmap)
            }
            //Create new document with the added changes
            val newPageInfo = PdfDocument.PageInfo.Builder(mBitmap.width, mBitmap.height, i + 1).create()
            val newPage = newDocument.startPage(newPageInfo)
            val newCanvas = newPage.canvas
            newCanvas.drawBitmap(mBitmap, Matrix(), Paint())
            newDocument.finishPage(newPage)
            page.close()
        }

        val newUri = savePdfDocument("newContract.pdf")
        context.contentResolver.openOutputStream(newUri!!).use {
            newDocument.writeTo(it)
        }
        // close the renderer
        renderer.close()
    }

    private fun getDocumentDetails(stream : InputStream) : Float {
        val options = BitmapFactory.Options()
        val desiredWidth = 600
        val desiredHeight = 800

        //options.inJustDecodeBounds = true;
        val mBitmap = BitmapFactory.decodeStream(stream)
        var srcWidth = options.outWidth
        var srcHeight = options.outHeight

        var inSampleSize = 1
        while (srcWidth / 2 > desiredWidth) {
            srcWidth /= 2
            srcHeight /= 2
            inSampleSize *= 2
        }
        return desiredWidth.toFloat() / srcWidth
    }


    private fun getDocumentByName(filename: String) : Uri? {

        //read file from media store
        val projection = arrayOf(
            MediaStore.MediaColumns.DOCUMENT_ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        )

        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/" + filename)
        val sortOrder = "${MediaStore.Downloads.DISPLAY_NAME} ASC"
        var contentUri : Uri? = null

        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            while (cursor.moveToNext()) {
                // Use an ID column from the projection to get
                // a URI representing the media item itself.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)


                contentUri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id
                )
            }
        }
        return contentUri
    }

    private fun getDocumentByUri(uri: Uri){
        context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
            }
        }
    }
}