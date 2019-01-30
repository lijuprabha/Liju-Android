package com.trivand.sql

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Looper
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.dialogue_set_radius_and_wifi.*
import org.jetbrains.anko.*
import org.jetbrains.anko.db.update


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    val Context.database: MyDatabaseOpenHelper
        get() = MyDatabaseOpenHelper.getInstance(applicationContext)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback? = null
    private var mLocationRequest = LocationRequest()
    private var mZoom = false
    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 101
    private var user: User? = null
    private var circle: Circle? = null
    private var alaramMarker: Marker? = null
    private var userCurrentLatLng: LatLng? = null
    private var snackbar: Snackbar? = null
    private var textView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        user = intent.getSerializableExtra("user") as? User
        infoMessage()
        mLocationRequest.interval = 1000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = (LocationRequest.PRIORITY_HIGH_ACCURACY)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_LONG);
        textView = snackbar!!.view.findViewById(android.support.design.R.id.snackbar_text) as TextView

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    userCurrentLatLng = LatLng(location.latitude, location.longitude)
                    checkGeographicRadiusPont_And_Wifi()
                    if (!mZoom) {
                        mZoom = true
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userCurrentLatLng, 17f))
                    }
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.mapsettings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_drawer_item1 -> {
                radiusWifiSettings()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapClickListener(this)
        if (mMap != null) {
            val permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)

            if (permission == PackageManager.PERMISSION_GRANTED) {
                mMap?.isMyLocationEnabled = true

            } else {
                requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_REQUEST_CODE)
            }
            createCircle()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when (requestCode) {
            LOCATION_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Unable to show location - permission required",
                            Toast.LENGTH_SHORT).show()
                } else {

                    val mapFragment = supportFragmentManager
                            .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this)

                }
            }
        }
    }


    override fun onMapClick(p0: LatLng) {

        if (user!!.lat.equals("00")) {
            drwaRoundOnMap(p0)
        } else
            alert(resources.getString(R.string.warning)) {
                title = "Alert"
                yesButton {
                    drwaRoundOnMap(p0)
                }
                noButton { }
            }.show()


    }

    override fun onMapLongClick(p0: LatLng) {

    }


    private fun drwaRoundOnMap(p0: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(p0))
        mMap.clear()
        alaramMarker = mMap.addMarker(MarkerOptions()
                .position(p0)
                .title("Geographic point with " + user!!.radius + " radius")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        updateAlaramLoc(p0.latitude.toString(), p0.longitude.toString())
        circle = mMap.addCircle(CircleOptions().center(LatLng(p0.latitude, p0.longitude)).radius(user!!.radius.toDouble()).strokeColor(Color.RED))


    }

    private fun updateAlaramLoc(lat: String, lon: String) {
        doAsync {
            val result = database.use {
                update("User",
                        "lat" to lat,
                        "lon" to lon)
                        .whereSimple("email = ?", user!!.email)
                        .exec()
            }

            uiThread {
                user!!.lat = lat
                user!!.lon = lon
                this@MapsActivity.toast("Geographic points are updated with " + user!!.radius + " radius")
            }
        }

    }

    private fun requestPermission(permissionType: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionType), requestCode)
    }

    private fun Context.toast(message: CharSequence) =
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun radiusWifiSettings() {
        var dialogs = Dialog(this)
        dialogs.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogs.setCancelable(false)
        dialogs.setContentView(R.layout.dialogue_set_radius_and_wifi)
        dialogs.ext_radius.setText(user!!.radius)
        dialogs.ext_wifi_name.setText(user!!.wifi)
        dialogs.btn_cancel.setOnClickListener { dialogs.dismiss() }
        dialogs.btn_add.setOnClickListener { updateRadiusWifi(dialogs.ext_radius.text.toString(), dialogs.ext_wifi_name.text.toString(),dialogs) }
        dialogs.show()
    }

    private fun updateRadiusWifi(radius: String, wifi: String,dialogs:Dialog) {
        doAsync {
            val result = database.use {
                update("User", "radius" to radius, "wifi" to wifi)
                        .whereSimple("email = ?", user!!.email)
                        .exec()
            }

            uiThread {
                if (result == 1) {
                    user!!.radius = radius
                    user!!.wifi = wifi
                    toast(" Radius & Wifi updated ")
                    createCircle()
                    dialogs.dismiss()
                } else toast("Opps..try again ")

            }
        }

    }

    private fun createCircle() {

        if (user!!.lat != "00" && user!!.lon != "00") {
            mMap.clear()
            alaramMarker = mMap.addMarker(MarkerOptions()
                    .position(LatLng(user!!.lat.toDouble(), user!!.lon.toDouble()))
                    .title("Geographic point with " + user!!.radius + " radius")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            circle = mMap.addCircle(CircleOptions().center(LatLng(user!!.lat.toDouble(), user!!.lon.toDouble())).radius(user!!.radius.toDouble()).strokeColor(Color.RED))
        }
    }

    private fun findSpecificWifiConnected(): Boolean {
        if (isNetworkAvailable()) {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ssid = wifi.connectionInfo.ssid
            ssid = ssid.replace("\"", "")
            if (ssid.equals(user!!.wifi)) {
                return true
            }
        }
        return false
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }

    private fun startLocationUpdates() {
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper())
        } else {
            requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_REQUEST_CODE)
            requestPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION, LOCATION_REQUEST_CODE)
        }
    }

    private fun checkGeographicRadiusPont_And_Wifi() {
        if (circle != null) {
            val distance = FloatArray(2)
            Location.distanceBetween(userCurrentLatLng!!.latitude, userCurrentLatLng!!.longitude, circle!!.getCenter().latitude, circle!!.getCenter().longitude, distance)
            var msg = ""
            if (distance[0] <= circle!!.getRadius()) {
                msg = "You are inside the geographic point"
            } else {
                msg = "You are outside the geographic point"
            }
            if (findSpecificWifiConnected()) {
                msg += " and connted to '" + user!!.wifi + "' wifi"
            }
            textView!!.setText(msg)
            snackbar!!.show()
        } else {
            if (findSpecificWifiConnected()) {
                textView!!.setText("Connted to '" + user!!.wifi + "' wifi and outside the geographic point")
                snackbar!!.show()

            }
        }
    }
    private fun infoMessage(){
        if(user!!.lat.equals("00")&&user!!.radius.equals("60")&&user!!.wifi.equals(""))
        alert(resources.getString(R.string.instructions)) {
            title = "Instructions"
            yesButton {

            }
        }.show()
    }
}
