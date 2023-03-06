package com.example.pawprints

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

class participantesComunidad : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

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

        val BotonAPart = findViewById<Button>(R.id.buttonPart)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participantes_comunidad)

        val mProjection = arrayOf(ContactsContract.Profile.ID,ContactsContract.Profile.DISPLAY_NAME_PRIMARY )


    }
}