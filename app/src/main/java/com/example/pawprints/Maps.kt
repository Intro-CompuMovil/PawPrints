package com.example.pawprints

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.pawprints.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.gpkg.overlay.features.PolylineOptions
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.LocationUtils.getLastKnownLocation
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Maps : AppCompatActivity(){
    private lateinit var binding: ActivityMapsBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private var mapView: MapView? = null
    private var mapController: IMapController? = null
    private var polyline= Polyline()
    private var currentZoomLevel: Double = 18.0
    private var lastRecordedLocation: GeoPoint? = null
    private val distanceThreshold = 30.0
    private var currentLocationMarker: Marker? = null
    private val markersList = mutableListOf<Marker>()


    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    val BOGOTA_NORTH_EAST = GeoPoint(4.826389, -74.014305)
    val BOGOTA_SOUTH_WEST = GeoPoint(4.469611, -74.217694)
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

    private fun createLocationOverlay(mapView: MapView): MyLocationNewOverlay {
        val locationOverlay = MyLocationNewOverlay(mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView.overlays.add(locationOverlay)
        return locationOverlay
    }


    private fun createCriteria(): Criteria {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE // Prioridad alta de precisión
        return criteria
    }

    private fun createLocationListener(): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                // Manejar la ubicación actualizada aquí
            }

            override fun onProviderEnabled(provider: String) {
                // Manejar cuando el proveedor de ubicación está habilitado
            }

            override fun onProviderDisabled(provider: String) {
                // Manejar cuando el proveedor de ubicación está deshabilitado
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                // Manejar cambios de estado del proveedor de ubicación
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationOverlay = mapView?.let { createLocationOverlay(it) }
            if (locationOverlay != null) {
                locationOverlay.runOnFirstFix {
                    // Aquí puedes realizar acciones después de que se obtenga la primera ubicación
                    // Por ejemplo, centrar el mapa en la ubicación del dispositivo
                    val myLocation = locationOverlay.myLocation
                    mapView?.controller?.animateTo(myLocation)
                }
            }
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
            currentLocationMarker?.icon =
                resources.getDrawable(org.osmdroid.gpkg.R.drawable.marker_default)
            mapView?.overlays?.add(currentLocationMarker)
        } else {
            currentLocationMarker?.position = currentLocation
        }
        mapView?.overlays?.remove(polyline)
        val mapController: MapController = mapView?.controller as MapController
        mapController.animateTo(currentLocation)
        currentZoomLevel = mapView!!.getZoomLevel(true)
        mapController.setZoom(currentZoomLevel)
    }



    private fun setMapStyleBasedOnLightLevel(lightLevel: Float) {
        val styleId = if (lightLevel < 1000) {
            R.raw.style_dark
        } else {
            R.raw.style_json
        }
        val mapController: MapController = mapView?.controller as MapController
        mapController.setZoom(currentZoomLevel)
        mapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView!!.setBuiltInZoomControls(true)
        mapView!!.setMultiTouchControls(true)
        mapView!!.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                currentZoomLevel = event?.zoomLevel ?: currentZoomLevel
                return false
            }
        })
        mapView!!.setUseDataConnection(false)
        //mapView!!.setMapStyleFile(resources.openRawResourceFd(styleId))
        mapView!!.invalidate()
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
                marker.icon = resources.getDrawable(org.osmdroid.gpkg.R.drawable.marker_default)
                mapView?.overlays?.add(marker)
                mapView?.controller?.setCenter(location)
                mapView?.controller?.setZoom(15)
                mapView?.invalidate()
            } else {
                Toast.makeText(this, "Dirección fuera de Bogotá", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }


    private fun drawStraightLine(startLatLng: GeoPoint, endLatLng: GeoPoint) {
        // Elimina la línea anterior si existe
        mapView?.overlays?.remove(polyline)

        // Crea una nueva línea recta con los puntos de inicio y fin
        val polyline = Polyline()
        polyline.addPoint(startLatLng)
        polyline.addPoint(endLatLng)
        polyline.color = Color.BLUE
        polyline.width = 5f

        // Añade la línea al mapa
        mapView?.overlays?.add(polyline)
        mapView?.overlays?.add(polyline)
        mapView?.invalidate()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la ruta de almacenamiento para OSM
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.map)
        mapView?.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView?.setMultiTouchControls(true)
        mapView?.setBuiltInZoomControls(true)


        // Configurar el overlay de ubicación
        var locationOverlay = MyLocationNewOverlay(mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView?.overlays?.add(locationOverlay)

        // Configurar el controlador del mapa
        val mapController = mapView?.controller
        val mapEventsReceiver = MapEventsReceiverImpl()


        // Configurar el receptor de eventos del mapa
        mapView?.overlays?.add(MapEventsOverlay(mapEventsReceiver))

        // Configurar el marcador
        val marker = Marker(mapView)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView?.overlays?.add(marker)

        // Configurar la escucha de eventos de zoom y desplazamiento del mapa
        mapView?.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                currentZoomLevel = event?.zoomLevel?.toDouble() ?: currentZoomLevel
                return false
            }
        })

        // Configurar el sensor de luz
        /*
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val light = event.values[0]
                if (mapView != null) {
                    if (light < 10) {
                        // Cargar estilo oscuro
                        mapView.setMapStyle(this@MapsActivity, R.raw.style_dark)
                    } else {
                        // Cargar estilo predeterminado
                        mapView.setMapStyle(this@MapsActivity, R.raw.style_json)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }
        }*/

        // Solicitar permisos de ubicación si no están otorgados
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val LOCATION_PERMISSION_REQUEST_CODE = 0
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Obtener la ubicación actual y centrar el mapa en ella
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Obtener la última ubicación conocida
            val lastLocation = locationOverlay.myLocation
            if (lastLocation != null) {
                val zoomGeoPoint = GeoPoint(lastLocation.latitude, lastLocation.longitude)
                mapController?.setZoom(currentZoomLevel.toInt())
                mapController?.setCenter(zoomGeoPoint)
                marker.position = zoomGeoPoint
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
        mapController = mapView.controller


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
            mapView.isEnabled = true


            /*mFusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = GeoPoint(location.latitude, location.longitude)

                    mapController?.setCenter(currentLocation)
                    mapView.invalidate()

                    mapView.setOnClickListener(View.OnClickListener { view ->
                        val clickedPoint = mapView.projection.fromPixels(
                            view.x.toInt(), view.y.toInt()
                        )
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val currentLocation = getLastKnownLocation(locationManager)

                        if (currentLocation != null) {
                            val currentLatLng = GeoPoint(
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            drawLine(currentLatLng, clickedPoint as GeoPoint)
                        }
                    })
                }
            }*/
        }
    }

    private fun drawLine(startPoint: GeoPoint, endPoint: GeoPoint) {
        val polyline = Polyline(mapView)
        polyline.setColor(Color.BLUE)
        polyline.width = 5f
        polyline.addPoint(startPoint)
        polyline.addPoint(endPoint)
        mapView?.overlayManager?.add(polyline)
        mapView?.invalidate()
    }



    class MapEventsReceiverImpl : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
            // Manejar el evento de toque único en el mapa
            return true
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
            // Manejar el evento de pulsación larga en el mapa
            return true
        }
    }

}