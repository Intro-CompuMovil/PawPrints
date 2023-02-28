package com.example.pawprints

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)
        val comsbutton = findViewById<Button>(R.id.button_gocom)
        val profButton = findViewById<Button>(R.id.button_go_profile)
        val mapsButton = findViewById<Button>(R.id.button_gomap)
        comsbutton.setOnClickListener{
            changeToComsAct(this)
        }
        profButton.setOnClickListener{
            changeToProfAct(this)
        }
        mapsButton.setOnClickListener{
            changeToMapsAct(this)
        }
    }
}