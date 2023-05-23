package com.example.pawprints

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.pawprints.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.example.pawprints.databinding.ActivityMapsBinding
import com.example.pawprints.R
import org.osmdroid.views.MapView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.location.Criteria
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util




class Maps : AppCompatActivity(), MapEventsReceiver{
    private lateinit var binding: ActivityMapsBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private var mapView: MapView? = null
    private var mapController: IMapController? = null
    private var polyline: Polyline? = null
    private var currentZoomLevel: Double = 18.0
    private var lastRecordedLocation: GeoPoint? = null
    private val distanceThreshold = 30.0
    private var currentLocationMarker: Marker? = null
    private val markersList = mutableListOf<Marker>()

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    val BOGOTA_NORTH_EAST = LatLng(4.826389, -74.014305)
    val BOGOTA_SOUTH_WEST = LatLng(4.469611, -74.217694)
    val bogotaBounds = org.osmdroid.util.BoundingBox(BOGOTA_NORTH_EAST.latitude, BOGOTA_NORTH_EAST.longitude, BOGOTA_SOUTH_WEST.latitude, BOGOTA_SOUTH_WEST.longitude)

    private fun calculateDistanceInMeters(a: GeoPoint, b: GeoPoint): Double {
        val locationA = android.location.Location("pointA").apply {
            latitude = a.latitude
            longitude = a.longitude
        }
        val locationB = android.location.Location("pointB").apply {
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

    private fun createLocationRequest(): GeoPoint {
        val locationRequest = GeoPoint(0.0, 0.0);
        // Configurar los parámetros de la solicitud de ubicación
        val priority = Criteria.ACCURACY_FINE // Prioridad alta de precisión
        val interval = 10000L // Intervalo de actualización de ubicación en milisegundos
        val fastestInterval = 5000L // Intervalo más rápido de actualización de ubicación en milisegundos

        // Actualizar los valores de la solicitud de ubicación
        locationRequest.setAccuracy(priority)
        locationRequest.setInterval(interval)
        locationRequest.setFastestInterval(fastestInterval)

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
        val currentLocation = GeoPoint(latitude, longitude)
        if (currentLocationMarker == null) {
            currentLocationMarker = Marker(mapView)
            currentLocationMarker?.position = currentLocation
            currentLocationMarker?.title = "Ubicación actual"
            currentLocationMarker?.icon = resources.getDrawable(R.drawable.marker_blue)
            mapView.overlays.add(currentLocationMarker)
        } else {
            currentLocationMarker?.position = currentLocation
        }
        polyline?.remove()
        val mapController: MapController = mapView.controller
        mapController.animateTo(currentLocation)
        currentZoomLevel = mapController.zoomLevelDouble
        mapController.setZoom(currentZoomLevel)
    }


    private fun setMapStyleBasedOnLightLevel(lightLevel: Float) {
        val styleId = if (lightLevel < 1000) {
            R.raw.style_dark
        } else {
            R.raw.style_json
        }
        val mapController: MapController = mapView.controller
        mapController.setZoom(currentZoomLevel)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        mapView.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                currentZoomLevel = event?.zoomLevel ?: currentZoomLevel
                return false
            }
        })
        mapView.setUseDataConnection(false)
        mapView.setMapStyleFile(resources.openRawResourceFd(styleId))
        mapView.invalidate()
    }



    private fun searchAddress(address: String) {
        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocationName(address, 1)

        if (addresses?.isNotEmpty() == true) {
            val location = GeoPoint(addresses[0].latitude, addresses[0].longitude)

            if (bogotaBounds.contains(location)) {
                val marker = Marker(mapView)
                marker.position = location
                marker.title = "Dirección buscada"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = resources.getDrawable(R.drawable.marker_icon)
                mapView.overlays.add(marker)
                mapView.controller.setCenter(location)
                mapView.controller.setZoom(15)
                mapView.invalidate()
            } else {
                Toast.makeText(this, "Dirección fuera de Bogotá", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }


    private fun drawStraightLine(startLatLng: GeoPoint, endLatLng: GeoPoint) {
        // Elimina la línea anterior si existe
        polyline?.remove()

        // Crea una nueva línea recta con los puntos de inicio y fin
        val polylineOptions = PolylineOptions()
            .add(startLatLng)
            .add(endLatLng)
            .color(Color.BLUE)
            .width(5f)

        // Añade la línea al mapa
        polyline = Polyline(mapView)
        polyline?.applyOptions(polylineOptions)
        mapView.overlays.add(polyline)
        mapView.invalidate()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mLocationRequest = createLocationRequest()

        // Obtain the MapView from the layout
        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Obtain the MapController
        mapController = mapView.controller

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    Log.i("LOCATION", "Location update in the callback: $location")
                    if (location != null) {
                        val newLocation = GeoPoint(location.latitude, location.longitude)
                        if (lastRecordedLocation == null || calculateDistanceInMeters(
                                lastRecordedLocation!!, newLocation
                            ) >= distanceThreshold
                        ) {
                            lastRecordedLocation = newLocation
                            saveLocationToJson(location.latitude, location.longitude)
                            updateLocationOnMap(location.latitude, location.longitude)
                            var zoomI = LatLng(location.latitude, location.longitude)
                            mapController.animateTo(zoomI)
                            val jsonContent = readJsonFile()
                            Log.i("JSON_CONTENT", "Contenido del archivo JSON: $jsonContent")
                        }
                    }
                }
            }
            LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
            mapView.onResume()
            mapView.postDelayed({
                mapController.animateTo(mapView.mapCenter)
                mapController.setZoom(currentZoomLevel.toInt())
            }, 1000)

            startLocationUpdates()
            lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val light = event.values[0]
                    if (mapView != null) {
                        if (light < 10) {
                            // Load dark style
                            mapView.setMapStyle(this@Maps, R.raw.style_dark)
                        } else {
                            // Load default style
                            mapView.setMapStyle(this@Maps, R.raw.style_json)
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

                        // Hide the keyboard after pressing the search button
                        val inputMethodManager =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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


    override fun onMapReady(mapView: MapView) {
        mMap = mapView.controller
        mMap.setZoom(15.0)

        val plazaBolivar = GeoPoint(4.647318, -74.058199)
        val parqueNovios = GeoPoint(4.669249, -74.054816)
        val cqVet = GeoPoint(4.635309, -74.064249)
        val vetPC = GeoPoint(4.689831, -74.068760)

        markersList.add(Marker(mapView).apply {
            position = plazaBolivar
            title = "Parque de los hippies"
            snippet = "Espacio abierto para pasear"
            alpha = 0.5f
        })
        markersList.add(Marker(mapView).apply {
            position = parqueNovios
            title = "Parque de Los Novios"
            snippet = "Espacio abierto para pasear"
            alpha = 0.5f
        })
        markersList.add(Marker(mapView).apply {
            position = cqVet
            title = "Centro quirurgico veterinario"
            snippet = "Centro especializado en el cuidado animal"
            alpha = 0.5f
        })
        markersList.add(Marker(mapView).apply {
            position = vetPC
            title = "Veterinaria Pet Company"
            snippet = "Veterinaria para la atención animal"
            alpha = 0.5f
        })

        markersList.forEach { marker ->
            mapView.overlays.add(marker)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapView.isMyLocationEnabled = true

            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = GeoPoint(location.latitude, location.longitude)
                    mMap.setCenter(currentLocation)
                    mMap.animateTo(currentLocation)
                    mMap.setOnMarkerClickListener { marker, mapView ->
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