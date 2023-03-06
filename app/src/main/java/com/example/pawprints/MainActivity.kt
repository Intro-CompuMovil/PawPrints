package com.example.pawprints

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity

private fun logIn(log:EditText, pass:EditText, cont: Context){

        val user = log.text.toString()
        val passw = pass.text.toString() //getting username and password
        //TODO
        //create the validation for user and password required (prob. firebase as the main DB)
        if(user != "" && passw != ""){
            Toast.makeText(cont, "¡Hola, $user!", Toast.LENGTH_LONG).show()
            goToMenuActivity(cont)
        }else{
            log.setText("")
            pass.setText("")
            Toast.makeText(cont,"Ingrese usuario y contraseña",Toast.LENGTH_LONG).show()
        }

}

private fun register(log:EditText,pass:EditText,cont:Context){
    log.setText("")
    pass.setText("")
    val intent = Intent(cont, Register::class.java)
    cont.startActivity(intent)
}

private fun goToMenuActivity(cont: Context) {
    val intent = Intent(cont, Menu::class.java)
    cont.startActivity(intent)
}

class MainActivity : AppCompatActivity() {

    companion object{
        const val GALLERY_REQUEST = 0
        const val CAMERA_REQUEST = 1
        const val PICK_IMAGE = 8
        const val CONTACTS_REQUEST =0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide() //hiding the large purple bar
        var log = findViewById<EditText>(R.id.user)
        var pass = findViewById<EditText>(R.id.password)
        var logger = findViewById<Button>(R.id.Log_in)
        var reg = findViewById<Button>(R.id.registermain)
        logger.setOnClickListener {
            logIn(log,pass,this)
        }
        reg.setOnClickListener{
            register(log,pass,this)
        }
    }
}