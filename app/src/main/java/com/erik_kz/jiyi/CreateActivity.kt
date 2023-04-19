package com.erik_kz.jiyi

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erik_kz.jiyi.models.BoardSize
import com.erik_kz.jiyi.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val PICK_PHOTO_CODE = 2
        private const val READ_STORAGE_CODE = 3
        private const val READ_STORAGE_PERM = android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private lateinit var tbCreate: androidx.appcompat.widget.Toolbar
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbImagesUpload: ProgressBar


    private lateinit var boardSize: BoardSize
    private var currentSelectedSquarePos: Int? = null
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private val chosenImageUris = mutableListOf<Uri>()

    private val storage = Firebase.storage
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        val initialHue = intent.getSerializableExtra("EXTRA_TOOLBAR_HUE") as Float

        tbCreate = findViewById(R.id.tbCreate)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbImagesUpload = findViewById(R.id.pbImagesUpload)

        // this is weird
        setSupportActionBar(tbCreate)
        updateToolbarCount()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        doDaRainbow(window, tbCreate, initialHue = initialHue)

        rvImagePicker.setHasFixedSize(true)
        fun imageOnClickListener() {
            if (isPermissionGranted(this, READ_STORAGE_PERM)) {
                launchIntentForPhotos()
            } else {
                requestPermission(this, READ_STORAGE_PERM, READ_STORAGE_CODE)
                // we launchIntentForPhotos() afterwards (onRequestPermissionResult)
            }
        }
        imagePickerAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize, ::imageOnClickListener, ::updateToolbarCount)
        rvImagePicker.adapter = imagePickerAdapter
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.cols)

        etGameName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun afterTextChanged(p0: Editable?) {
                checkEnableSaveBtn()
            }
        })

        btnSave.setOnClickListener {
            saveToFirebase()
        }

    }

    private fun saveToFirebase() {
        btnSave.isEnabled = false
        pbImagesUpload.visibility = View.VISIBLE
        val customGameName = etGameName.text.toString()
        // check if game already exists
        db.collection("game").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Game already exists!")
                    .setMessage("Try a name other than $customGameName")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                handleUploadImages(customGameName)
            }
        }.addOnFailureListener {
            btnSave.isEnabled = true
            pbImagesUpload.visibility = View.GONE
            Toast.makeText(this, "had an error saving! weird", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUploadImages(customGameName: String) {
        var hasEncounteredError = false
        val uploadedImages = mutableListOf<Uri>()
        for ((index, imageUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(imageUri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-$index.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray).continueWithTask { photoUploadTask ->
                Log.i(
                    "CreateActivity",
                    "uploaded ${photoUploadTask.result?.bytesTransferred} bytes"
                )
                // it expects us to return a task. thing below is a task. we pass the thing and not a call (downloadUrl())
                // https://firebase.google.com/docs/storage/android/upload-files#get_a_download_url
    //                photoReference.getDownloadUrl()
                photoReference.downloadUrl
            }.addOnCompleteListener { downloadUrlTask ->
                if (hasEncounteredError) {
                    return@addOnCompleteListener
                }
                if (!downloadUrlTask.isSuccessful) {
                    Toast.makeText(
                        this@CreateActivity,
                        "failed to upload image!!",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasEncounteredError = true
                    return@addOnCompleteListener
                }
                val downloadUrl = downloadUrlTask.result
                uploadedImages.add(downloadUrl)

                pbImagesUpload.progress =
                    (uploadedImages.size*100 / chosenImageUris.size)
                Log.i(
                    "CreateActivity",
                    "${uploadedImages.size} >= ${chosenImageUris.size} -- ${(uploadedImages.size*100.toFloat() / chosenImageUris.size + 1).toInt()} -- ${pbImagesUpload.progress}"
                )
                if (uploadedImages.size >= chosenImageUris.size) {
                    handleAllImagesUploaded(uploadedImages, customGameName)
                }
            }
        }
    }

    private fun handleAllImagesUploaded(uploadedImages: MutableList<Uri>, gameName: String) {
        Log.i("CreateActivity", "handling images")
        db.collection("game").document(gameName)
            .set("imageUris" to uploadedImages)
            .addOnCompleteListener {
                Log.i("CreateActivity", "handling pt 2")
                if(!it.isSuccessful) {
                    Log.i("CreateActivity", "ERORORORO ${it.result} ${it.exception}")
                    // error yo
                    return@addOnCompleteListener
                }
                btnSave.isEnabled = true
                pbImagesUpload.visibility = View.GONE
                Log.i("CreateActivity", "successfully created $gameName!")
                AlertDialog.Builder(this)
                    .setTitle("your game is up and running! want to test it?")
                    .setPositiveButton("OK") { _,_ ->
                        // https://stackoverflow.com/a/14785924
                        // if we want to return data from activity, we can simply return the primitives
                        // we use Intents for more than 1 piece of data
                        val resultData = Intent()
                        resultData.putExtra("EXTRA_GAME_NAME", gameName)
                        // these are inherited from `this`, that is, they're CreateActivity methods
                        // btw, since we did startActivityForResult, this is what we're returning
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                }.show()
            }
    }

    private fun getImageByteArray(imageUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        }

        Log.d("CreateActivity", "org bpm ${originalBitmap.width}x${originalBitmap.height}")
        val squareBitmap = BitmapScaler.cropBmpToSquare(originalBitmap)
        val scaledSquareBitmap = BitmapScaler.scaleBmpToLargestSide(squareBitmap, 250)
        Log.d("CreateActivity", "scaled bpm ${scaledSquareBitmap.width}x${scaledSquareBitmap.height}")

        val byteOutputStream = ByteArrayOutputStream()
        scaledSquareBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun updateToolbarCount() {
        tbCreate.title = getString(R.string.select_photos).format(chosenImageUris.size, boardSize.numPairs)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this, "to create a custom deck, you must provide access!!", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "choose yer pics"), PICK_PHOTO_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PHOTO_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val clipData = data.clipData
            if (clipData != null) {
                Log.d("YEP", "${clipData.itemCount}")
                val initialListSize = chosenImageUris.size

                var iterationOffset = 0
                for (i in 0 until clipData.itemCount) {
                    val clipItem = clipData.getItemAt(i)
                    if (chosenImageUris.size < boardSize.numPairs) {
                        // we only want unique images. so if it has been added, don't add it but
                        // offset negatively so as to fill the now empty space with the next images
                        if (chosenImageUris.contains(clipItem.uri)) {
                            iterationOffset++
                        } else {
                            chosenImageUris.add(clipItem.uri)
                            imagePickerAdapter.notifyItemChanged(initialListSize + i-iterationOffset)
                        }
                    }
                }
            } else {

                // weird android thing: if user selected only 1 image, this is how you get the data
                data.data?.apply {
                    if (!chosenImageUris.contains(this)) {
                        chosenImageUris.add(this)
                        Log.d("YEP", "1")
                        imagePickerAdapter.notifyItemChanged(chosenImageUris.size-1)
                    }
                }
            }
            updateToolbarCount()
            checkEnableSaveBtn()
        } else {
            // user likely cancelled flow
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkEnableSaveBtn() {
        btnSave.isEnabled = chosenImageUris.size >= boardSize.numPairs && etGameName.text.length > 3
    }
}

//class LWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
//
//    override fun doWork(): ListenableWorker.Result {
//        repeat(100) {
//            try {
//                downloadSynchronously("https://www.google.com")
//            } catch (e: IOException) {
//                return ListenableWorker.Result.failure()
//            }
//        }
//
//        return ListenableWorker.Result.success()
//    }
//}
