package com.example.ejpermisos

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val boton = findViewById<Button>(R.id.boton_foto)
        boton.setOnClickListener(){
            checkPermissions()
        }
    }

    private fun checkPermissions(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA )!= PackageManager.PERMISSION_GRANTED){
            //permiso denegado, volver a solicitar
            requestCameraPermission()
        }else{
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            // Explicar al usuario por qué necesita el permiso y volver a solicitarlo.
            Toast.makeText(this, "Se requiere el permiso de la cámara para tomar fotos.", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // pedir permisos
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private var permissionRetryCount = 0
    private var permissionDenied = false


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                if (permissionRetryCount < 2) { // Intentar solicitar el permiso un máximo de 3 veces
                    permissionRetryCount++
                    requestCameraPermission()
                } else {
                    permissionDenied = true
                    Toast.makeText(this,"Permiso denegado por el usuario",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}