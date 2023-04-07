package com.example.pawprints

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

class Register : AppCompatActivity() {


    private val img : ImageView by lazy {
        findViewById<ImageView>(R.id.profilePicReg)
    }

    private var tempImageUri: Uri? = null
    private var tempImageFilePath = ""
    private val albumLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        img.setImageURI(it)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
        success ->
        if (success){
            img.setImageURI(tempImageUri)
        }
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
                    requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST)
                }
                return
            }
        }
    }

    private fun showInContextUI() : Boolean{
        var acepto = false
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permiso necesario")
        builder.setMessage("Esta función requiere acceso. Si deniegas el permiso, algunas funciones estarán deshabilitadas.")
        builder.setPositiveButton("Aceptar") { dialog, which ->
            acepto = true
        }
        builder.setNegativeButton("Volver") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
        return acepto
    }

    private fun askImageMethod(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Insertar Imagen")
        builder.setMessage("Como desea seleccionar la foto??")
        builder.setPositiveButton("Camara") { dialog, which ->
            askCameraPermission()
        }
        builder.setNegativeButton("Galeria") { dialog, which ->
            // Si el usuario hace clic en "Cancelar", cierra el diálogo.
            dialog.dismiss()
            askGalleryPermission()
        }
        builder.show()

    }

    private fun askCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }else{
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission(){
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            val acepto = showInContextUI()
            if(acepto){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST)
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }else{
                    return
                }
            }else{
                return
            }
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST)
        }
    }

    private fun startCamera(){
        tempImageUri = FileProvider.getUriForFile(this, "com.example.pawprints.provider", createImageFile().also {
            tempImageFilePath = it.absolutePath
        })
        cameraLauncher.launch(tempImageUri)
    }

    private fun askGalleryPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            startGallery()
        }else{
            requestGalleryPermission()
        }
    }
    private fun requestGalleryPermission(){
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showInContextUI()
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST)
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST)
        }
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