package com.example.pawprints

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

class Communities : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        Toast.makeText(this,"Bienvenido a tu comunidad",Toast.LENGTH_LONG).show()
        setContentView(R.layout.activity_communities)

        val BotonVerParticipantes=findViewById<Button>(R.id.buttonPart)

        BotonVerParticipantes.setOnClickListener {
            abrirParticipantes()
        }
    }

    fun abrirParticipantes(){
        val intent = Intent(this,participantesComunidad::class.java)
        this.startActivity(intent)
    }
}