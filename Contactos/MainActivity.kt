package com.example.pawprints

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build.ID
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CloudMediaProviderContract.AlbumColumns.ID
import android.provider.CloudMediaProviderContract.MediaColumns.ID
import android.provider.ContactsContract
import android.widget.Button
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.jetbrains.annotations.Contract

class participantesComunidad : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    fun initView(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            mCursor=contentResolver.query(ContactsContract.Contacts.CONTENT_URI,mProjection,null,null,null)
            mContactsAdapter?.changeCursor(mCursor)
        }
    }

    private fun requestContacsPermicion(){
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS),
                MainActivity.CONTACTS_REQUEST
            )
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS),
                MainActivity.CONTACTS_REQUEST
            )
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_participantes_comunidad)

        val botonAPart = findViewById<Button>(R.id.buttonAnadirP)

        botonAPart.setOnClickListener {
            requestContacsPermicion()

            mlista = findViewById(R.id.Lista)

            mProjection = arrayOf(ContactsContract.Profile._ID,ContactsContract.Profile.DISPLAY_NAME_PRIMARY)

            mContactsAdapter = ContactsAdapter(this,null,0)
            mlista?.adapter = mContactsAdapter
            initView()

        }


    }
}