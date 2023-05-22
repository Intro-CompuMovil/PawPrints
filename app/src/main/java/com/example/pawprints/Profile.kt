package com.example.pawprints

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)
        auth = FirebaseAuth.getInstance()
        myRef = database.getReference("usuarios/").child(auth.getUid().toString())
        val nombre = findViewById<TextView>(R.id.NombreReg)
        val raza = findViewById<TextView>(R.id.razaShow)
        val owner = findViewById<TextView>(R.id.nomDueñoReg)
        val foto = findViewById<ImageButton>(R.id.profilePicReg)
        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val user = dataSnapshot.getValue(User::class.java)
                if(user!=null){
                    nombre.text = user.nombre
                    raza.text = user.raza
                    owner.text = user.owner
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@Profile, "Error en la base de datos", Toast.LENGTH_LONG).show()
            }
        })
    }
}