package com.example.pawprints

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.pawprints.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

private fun changeToComsAct(cont: Context){
    val intent = Intent(cont, Communities::class.java)
    cont.startActivity(intent)
}

private fun changeToProfAct(cont: Context){
    val intent = Intent(cont, Profile::class.java)
    cont.startActivity(intent)
}

private fun changeToMapsAct(cont: Context){
    val intent = Intent(cont, Maps::class.java)
    cont.startActivity(intent)
}



class Menu : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)
        auth = FirebaseAuth.getInstance()
        database =FirebaseDatabase.getInstance()
        val comsbutton = findViewById<Button>(R.id.button_gocom)
        val profButton = findViewById<Button>(R.id.button_go_profile)
        val mapsButton = findViewById<Button>(R.id.button_gomap)
        val logout = findViewById<Button>(R.id.logout)
        comsbutton.setOnClickListener{
            changeToComsAct(this)
        }
        profButton.setOnClickListener{
            changeToProfAct(this)
        }
        mapsButton.setOnClickListener{
            checkLocationPermission()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ){
                changeToMapsAct(this)
            }
        }
        logout.setOnClickListener{
            auth.signOut()
            val logIntent =Intent(this, MainActivity::class.java)
            startActivity(logIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    //Aqui va el proceso pa sacar los datos
                    Toast.makeText(this, "permission granted :)", Toast.LENGTH_LONG).show()

                }else{
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
            /*ACCESS_COARSE_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    //Aqui va el proceso pa sacar los datos
                    Toast.makeText(this, "permission granted :)", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }*/
        }

    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) /*|| ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            */) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            ACCESS_FINE_LOCATION
        )
    }

}