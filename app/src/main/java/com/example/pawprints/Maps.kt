package com.example.pawprints

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
import com.google.android.gms.maps.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.ArrayList

import com.google.maps.model.LatLng as ApiLatLng

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
    private var polyline: Polyline? = null
    private var currentZoomLevel: Float = 18F
    private var lastRecordedLocation: LatLng? = null
    private val distanceThreshold = 30.0
    private var currentLocationMarker: Marker? = null
    private val markersList = mutableListOf<MarkerOptions>()
    private val db = Firebase.firestore

    val BOGOTA_NORTH_EAST = LatLng(4.826389, -74.014305)
    val BOGOTA_SOUTH_WEST = LatLng(4.7756032, -74.9552260)
    val bogotaBounds = LatLngBounds(BOGOTA_SOUTH_WEST, BOGOTA_NORTH_EAST)


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
        val currentLocation = LatLng(latitude, longitude)
        if (currentLocationMarker == null) {
            currentLocationMarker = mMap?.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Ubicación actual")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            currentLocationMarker?.position = currentLocation
        }
        polyline?.remove()
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
        currentZoomLevel = mMap?.cameraPosition?.zoom ?: currentZoomLevel
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

            if (bogotaBounds.contains(location)) {
                mMap?.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Dirección buscada")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } else {
                Toast.makeText(this, "Dirección fuera de Bogotá", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }


    private fun drawStraightLine(startLatLng: LatLng, endLatLng: LatLng) {
        // Elimina la línea anterior si existe
        polyline?.remove()

        // Crea una nueva línea recta con los puntos de inicio y fin
        val polylineOptions = PolylineOptions()
            .add(startLatLng)
            .add(endLatLng)
            .color(Color.BLUE)
            .width(5f)

        // Añade la línea al mapa
        polyline = mMap?.addPolyline(polylineOptions)
    }

    private fun searchAddressVet(address: String, name: String) {
        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocationName(address, 1, 4.5628072, -74.2279343, 4.7756032, -73.9552260)
        Log.d("DEBUG", "Addresses for $name: $addresses")
        if (addresses?.isNotEmpty() == true && addresses!=null) {
            val location = LatLng(addresses[0].latitude, addresses[0].longitude)
            mMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
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
                            var zoomI = LatLng(location.latitude, location.longitude)
                            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomI, currentZoomLevel))
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
            binding.texto.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val address = binding.texto.text.toString()
                    if (address.isNotBlank()) {
                        searchAddress(address)

                        // Oculta el teclado después de presionar el botón de búsqueda
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    true
                } else {
                    false
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
        mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        //mMap!!.setLatLngBoundsForCameraTarget(bogotaBounds)
        val geocoder = Geocoder(this)
        val plazaBolivar = LatLng(4.647318, -74.058199)
        val parqueNovios = LatLng(4.669249, -74.054816)
        val cqVet = LatLng(4.635309, -74.064249)
        val vetPC = LatLng(4.689831, -74.068760)
        val list: MutableList<Vet> = ArrayList()
        val mQuery = db.collection("vets")
        mQuery.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result!!) {
                    val vet = Vet(
                        email = document.get("email") as? String ?: "",
                        location = document.get("location") as? String ?: "",
                        name_vet = document.get("name_vet") as? String ?: ""
                    )
                    list.add(vet)
                    Log.i("VET", "Contenido de VET: ${vet.name_vet} + ${vet.location}")

                    val addresses = geocoder.getFromLocationName(vet.location, 1, 4.5628072, -74.2279343, 4.7756032, -73.9552260)
                    Log.d("DEBUG", "Addresses for ${vet.name_vet}: $addresses")
                    if (addresses?.isNotEmpty() == true && addresses!=null) {
                        val location = LatLng(addresses[0].latitude, addresses[0].longitude)
                        markersList.add(
                            MarkerOptions()
                                .position(location)
                                .title(vet.name_vet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .alpha(0.5f)
                        )
                    } else {
                        Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }

                // Ahora que todos los marcadores de Firestore han sido agregados a la lista, agrega los marcadores estáticos y luego a todos ellos al mapa
                markersList.add(MarkerOptions().position(plazaBolivar).title("Parque de los hippies").snippet("Espacio abierto para pasear").alpha(0.5f))
                markersList.add(MarkerOptions().position(parqueNovios).title("Parque de Los Novios").snippet("Espacio abierto para pasear").alpha(0.5f))
                markersList.add(MarkerOptions().position(cqVet).title("Centro quirurgico veterinario").snippet("Centro especializado en el cuidado animal").alpha(0.5f))
                markersList.add(MarkerOptions().position(vetPC).title("Veterinaria Pet Company").snippet("Veterinaria para la atención animal").alpha(0.5f))

                markersList.forEach { markerOptions ->
                    mMap?.addMarker(markerOptions)?.tag = "routeMarker"
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap!!.isMyLocationEnabled = true

            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, currentZoomLevel))
                    mMap!!.setOnMarkerClickListener { marker ->
                        val destinationLatLng = marker.position
                        lastRecordedLocation?.let { currentLocation ->
                            drawStraightLine(currentLocation, destinationLatLng)
                        }
                        false
                    }


                }
            }
        }

    }

}