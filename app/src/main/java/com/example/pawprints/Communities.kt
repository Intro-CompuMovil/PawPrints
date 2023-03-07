package com.example.pawprints

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast


class Communities : AppCompatActivity() {
    fun abrirParticipantes(){
        val intent = Intent(this,participantesComunidad::class.java)
        this.startActivity(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_communities)
        Toast.makeText(this,"Bienvenido a tu comunidad",Toast.LENGTH_LONG).show()
        val BotonVerParticipantes=findViewById<Button>(R.id.buttonPart)

        BotonVerParticipantes.setOnClickListener {
            abrirParticipantes()
        }
    }


}