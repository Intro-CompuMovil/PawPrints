package com.example.pawprints

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)
        val reg = findViewById<Button>(R.id.Button_register)
        val nom = findViewById<EditText>(R.id.NombreReg)
        val raza = findViewById<EditText>(R.id.raza)
        val owner = findViewById<EditText>(R.id.nomDueñoReg)


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