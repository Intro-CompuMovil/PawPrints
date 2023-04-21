package com.example.pawprints
/*
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.pawprints.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import android.hardware.*
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.*
import com.example.pawprints.MainActivity.Companion.ACCESS_FINE_LOCATION*/

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.pawprints.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.example.pawprints.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.example.pawprints.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Maps : AppCompatActivity(), OnMapReadyCallback{
    private var mMap: GoogleMap? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    private var longitud: Double? = null
    private var latitud: Double? = null
    private var currentZoomLevel: Float = 18F
    private var lastRecordedLocation: LatLng? = null
    private val distanceThreshold = 30.0

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

    private fun calculateDistanceInMeters(a: LatLng, b: LatLng): Double {
        val locationA = Location("pointA").apply {
            latitude = a.latitude
            longitude = a.longitude
        }
        val locationB = Location("pointB").apply {
            latitude = b.latitude
            longitude = b.longitude
        }
        return locationA.distanceTo(locationB).toDouble()
    }

    private fun saveLocationToJson(latitude: Double, longitude: Double) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLocation = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", timeStamp)
        }

        val jsonFile = File(filesDir, "locations.json")
        val locationsArray = if (jsonFile.exists()) {
            JSONArray(jsonFile.readText())
        } else {
            JSONArray()
        }

        locationsArray.put(newLocation)

        FileOutputStream(jsonFile).use { outputStream ->
            outputStream.write(locationsArray.toString().toByteArray())
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
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
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            ACCESS_FINE_LOCATION
        )
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000L
            fastestInterval = 5000L
        }

        return locationRequest
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
        }
    }

    private fun readJsonFile(): String {
        val fileName = "locations.json"
        return try {
            val inputStream = openFileInput(fileName)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            "Archivo no encontrado"
        } catch (e: IOException) {
            "Error al leer el archivo"
        }
    }


    private fun updateLocationOnMap(latitude: Double, longitude: Double) {

        mMap?.clear()

        // Crea un nuevo marcador con la ubicación actualizada
        val currentLocation = LatLng(latitude, longitude)
        mMap?.addMarker(
            MarkerOptions()
                .position(currentLocation)
                .title("Ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        // Mueve la cámara a la nueva ubicación
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))

        currentZoomLevel = mMap?.cameraPosition?.zoom ?: currentZoomLevel


        // Anima la cámara al nivel de zoom guardado
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, currentZoomLevel))

    }

    private fun setMapStyleBasedOnLightLevel(lightLevel: Float) {
        val styleId = if (lightLevel < 1000) {
            R.raw.style_dark
        } else {
            R.raw.style_json
        }
        mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, styleId))
    }

    private fun searchAddress(address: String) {
        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocationName(address, 1)

        if (addresses?.isNotEmpty() == true) {
            val location = LatLng(addresses[0].latitude, addresses[0].longitude)

            mMap?.clear()
            mMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Dirección buscada")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mLocationRequest = createLocationRequest()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        checkLocationPermission()
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    Log.i("LOCATION", "Location update in the callback: $location")
                    if (location != null) {
                        val newLocation = LatLng(location.latitude, location.longitude)
                        if(lastRecordedLocation==null || calculateDistanceInMeters(
                                lastRecordedLocation!!, newLocation) >= distanceThreshold){
                            lastRecordedLocation = newLocation
                            saveLocationToJson(location.latitude, location.longitude)
                            updateLocationOnMap(location.latitude, location.longitude)
                            val jsonContent = readJsonFile()
                            Log.i("JSON_CONTENT", "Contenido del archivo JSON: $jsonContent")
                        }
                    }
                }
            }
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            startLocationUpdates()
            lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val light = event.values[0]
                    if (mMap != null) {
                        if (light < 10) {
                            mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Maps, R.raw.style_dark))
                        } else {
                            mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Maps, R.raw.style_json))
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

                }
            }


        }

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightSensorListener)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
        mMap!!.uiSettings?.isZoomControlsEnabled = true
        mMap!!.uiSettings?.isZoomGesturesEnabled = true
        // Add a marker in Sydney and move the camera
        mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))

    }

}