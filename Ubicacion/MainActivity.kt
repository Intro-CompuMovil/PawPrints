package com.example.locationproject

import android.Manifest
import android.content.pm.PackageManager
//import android.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.locationproject.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACCESS_FINE_LOCATION = 0
        const val ACCESS_COARSE_LOCATION = 1
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    //Funcion para pedir permiso
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Revuerda agregar a la libreria de gradle el binding
        //android{
        //...
        //  viewBinding{
        //      enabled=true
        //  }
        //}

        setSupportActionBar(binding.toolbar)
        mLocationRequest = createLocationRequest()
        binding.gpsButton.setOnClickListener{
            //Primero revisa los permisos
            checkLocationPermission()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //Solo procede si se acepto. mLocationCallback sirve para ir actualizando la ubicacion
                mLocationCallback = object : LocationCallback(){
                    override fun onLocationResult(locationResult: LocationResult) {
                        startLocationUpdates()
                        super.onLocationResult(locationResult)
                        val location = locationResult.lastLocation
                        //location recibira la ubicacion actualizada
                        Log.i("LOCATION", "Location update in the callback: $location")
                        if(location!=null){
                            binding.elevacion.text = location.altitude.toString()
                            binding.longitud.text = location.longitude.toString()
                            binding.latitud.text = location.latitude.toString()
                        }
                    }
                }
                //Necesario para seguir pidiendo actualizaciones. En vez de LocationServices.getFused... puede ser directamente mFusedLocationClient(this).request...
                LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
            }
        }

    }

    //Suscripcion de servicio. NO implementado solamente por que al pedir permisos or primera vez causa que se invoque estas funciones
    // y en ese momento mLocationCallback no ha sido inicializado. Es el unico problema
    /*override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {

            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onStop(){
        super.onStop()
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback)

    }*/

    /* Distancia entre dos puntos
    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
val latDistance = Math.toRadians(lat1 - lat2)
val lngDistance = Math.toRadians(long1 - long2)
val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
* Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
val result = RADIUS_OF_EARTH_KM * c
return (result * 100.0).roundToInt() / 100.0
}
     */

    /*private fun checkLocationSettings(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            Log.i("LOCATION", "GPS IS ON")
            //settingsOK = true
            startLocationUpdates()
        }
    }*/

    //Funcion que establece el intervalo de tiempo que buscara una actualizacion de la ubicacion
    private fun createLocationRequest(): LocationRequest {
        val locationRequest: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        )
            .setMinUpdateIntervalMillis(5000)
            .build()

        //locationRequest.setFastestInterval(5000)
        return locationRequest
    }

    //Actualizacion de ubicacion
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED*/
        ) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
        }
    }

    //Funcion que revisa los permisos y muestra la razon etc.
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
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
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            ACCESS_FINE_LOCATION
        )
    }

    //Funciones no usadas y auto generadas al crear esto pero aja me dio pereza borrarlas
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    //CODIGO REFERIDO A GUARDAR LAS UBICACIONES EN JSON (NO HECHO)
    /*
    private fun writeJSONObject() {
localizaciones.put(MyLocation(Date(System.currentTimeMillis()), location.latitude,
location.longitude).toJSON())
var output: Writer?
val filename = "locations.json"
try {
val file = File(baseContext.getExternalFilesDir(null), filename)
Log.i("LOCATION", "Ubicacion de archivo: $file")
output = BufferedWriter(FileWriter(file))
output.write(localizaciones.toString())
output.close()
Toast.makeText(applicationContext, "Location saved", Toast.LENGTH_LONG).show()
} catch (e: Exception) {
//Log error
}
}

class MyLocation(var fecha: Date, var latitud: Double, var
longitud: Double) {
fun toJSON(): JSONObject {
val obj = JSONObject()
try {
obj.put("latitud", latitud)
obj.put("longitud", longitud)
obj.put("date", fecha)
} catch (e: JSONException) {
e.printStackTrace()
}
return obj
}
}
private fun readJSONArrayFromFile(fileName: String): JSONArray {
val file = File(baseContext.getExternalFilesDir(null), fileName)
if (!file.exists()) {
Log.i("LOCATION", "Ubicacion de archivo: $file no encontrado")
return JSONArray()
}
val jsonString = file.readText()
return JSONArray(jsonString)
}
     */
}