package com.example.pawprints

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pawprints.MainActivity.Companion.CAMERA_REQUEST
import com.example.pawprints.MainActivity.Companion.GALLERY_REQUEST
import com.example.pawprints.MainActivity.Companion.PICK_IMAGE
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import android.provider.MediaStore

class Register : AppCompatActivity() {


    private val img : ImageView by lazy {
        findViewById<ImageView>(R.id.profilePicReg)
    }

    private var tempImageUri: Uri? = null
    private var tempImageFilePath = ""
    private val albumLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        img.setImageURI(it)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            lifecycleScope.launch {
                val imageUri = saveImageToGallery()
                loadImageWithGlide(imageUri)
            }
        }
    }

    private suspend fun saveImageToGallery(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val contentResolver = applicationContext.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            var outStream: FileOutputStream? = null
            var inStream: FileInputStream? = null

            try {
                outStream = contentResolver.openOutputStream(imageUri) as FileOutputStream
                inStream = FileInputStream(File(tempImageFilePath))

                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                }

            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Error al guardar la imagen en la galería", Toast.LENGTH_SHORT).show()
                }
                return null
            } finally {
                outStream?.close()
                inStream?.close()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Register, "Error al guardar la imagen en la galería", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        return imageUri
    }

    private fun loadImageWithGlide(imageUri: Uri?) {
        Glide.with(this)
            .load(imageUri)
            .into(img)
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GALLERY_REQUEST -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    startGallery()
                }else{
                    showInContextUI()
                    requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST)
                }
                return
            }
            CAMERA_REQUEST -> {
                if((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    startCamera()

                }else{
                    showInContextUI()
                    requestPermissions(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_REQUEST)
                }
                return
            }
        }
    }

    private fun showInContextUI(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permiso necesario")
        builder.setMessage("Esta función requiere acceso. Si deniegas el permiso, algunas funciones estarán deshabilitadas.")
        builder.setNegativeButton("Volver") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun askImageMethod(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Insertar Imagen")
        builder.setMessage("Como desea seleccionar la foto??")
        builder.setPositiveButton("Camara") { dialog, which ->
            checkCameraPermission()
            if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ){
                startCamera()
            }
        }
        builder.setNegativeButton("Galeria") { dialog, which ->
            // Si el usuario hace clic en "Cancelar", cierra el diálogo.
            dialog.dismiss()
            checkGalleryPermission()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ){
                startGallery()
            }
        }
        builder.show()

    }

    private fun startCamera(){
        tempImageUri = FileProvider.getUriForFile(this, "com.example.pawprints.provider", createImageFile().also {
            tempImageFilePath = it.absolutePath
        })
        cameraLauncher.launch(tempImageUri)
    }



    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            MainActivity.CAMERA_REQUEST
        )
    }

    private fun checkCameraPermission(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.CAMERA
                ) || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Permisos de camara necesarios")
                    .setMessage("Para su funcionamiento correcto, es necesario que acepte los permisos")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestCameraPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestCameraPermission()
            }
        }
    }

    private fun checkGalleryPermission(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Permisos de galeria necesarios")
                    .setMessage("Para su funcionamiento correcto, es necesario que acepte los permisos")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestGalleryPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestGalleryPermission()
            }
        }
    }

    private fun requestGalleryPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            MainActivity.GALLERY_REQUEST
        )
    }

    private fun startGallery(){
        albumLauncher.launch("image/*")
    }

    private fun createImageFile() : File{
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("temp-image", ".jpg", storageDir)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)
        val reg = findViewById<Button>(R.id.Button_register)
        val nom = findViewById<EditText>(R.id.NombreReg)
        val raza = findViewById<EditText>(R.id.raza)
        val owner = findViewById<EditText>(R.id.nomDueñoReg)
        val foto = findViewById<Button>(R.id.cambiarFoto)


        foto.setOnClickListener{
            askImageMethod()
        }


        reg.setOnClickListener{
            val nombre = nom.text.toString()
            val razareg = raza.text.toString()
            val ownerreg = owner.text.toString()
            if(nombre != "" && razareg!="" && ownerreg!=""){
                Toast.makeText(this,"Bienvenido a la familia, $nombre",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Llene todos los valores de registro",Toast.LENGTH_LONG).show()
            }
        }
    }
}