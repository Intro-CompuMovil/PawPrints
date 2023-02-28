package com.example.pawprints

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class Communities : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        Toast.makeText(this,"Bienvenido a tu comunidad",Toast.LENGTH_LONG).show()
        setContentView(R.layout.activity_communities)
    }
}