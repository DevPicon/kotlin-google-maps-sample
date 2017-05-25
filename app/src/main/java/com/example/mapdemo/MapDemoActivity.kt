package com.example.mapdemo

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MapDemoActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private var map: GoogleMap? = null
    private val mGoogleApiClient: GoogleApiClient by lazy { initGoogleApiClient() }
    private val mLocationRequest: LocationRequest by lazy { initLocationRequest() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_demo_activity)

        if (TextUtils.isEmpty(resources.getString(R.string.google_maps_api_key))) {
            throw IllegalStateException("You forgot to supply a Google Maps API key")
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map -> loadMap(map) }

    }

    fun loadMap(googleMap: GoogleMap) {
        map = googleMap
        if (map != null) {
            // Map is ready
            Toast.makeText(this, "Map Fragment was loaded properly!", Toast.LENGTH_SHORT).show()
            //MapDemoActivityPermissionsDispatcher.getMyLocationWithCheck(this)
            MapDemoActivityPermissionsDispatcher.getMyLocationWithCheck(this)
        } else {
            Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MapDemoActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    fun getMyLocation() {
        // Now that map has loaded, let's get our location!
        map?.let {
            (map as GoogleMap).isMyLocationEnabled = true
            connectClient()
        }


    }

    private fun initGoogleApiClient(): GoogleApiClient {
        return GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build()
    }

    protected fun connectClient() {
        // Connect the client.
        if (isGooglePlayServicesAvailable && mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }
    }

    /*
     * Called when the Activity becomes visible.
    */
    override fun onStart() {
        super.onStart()
        connectClient()
    }

    /*
	 * Called when the Activity is no longer visible.
	 */
    override fun onStop() {
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.disconnect()
        }
        super.onStop()
    }

    /*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Decide what to do based on the original request code
        when (requestCode) {

            CONNECTION_FAILURE_RESOLUTION_REQUEST ->
                /*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
                when (resultCode) {
                    Activity.RESULT_OK -> mGoogleApiClient!!.connect()
                }
        }
    }

    private // Check that Google Play services is available
            // If Google Play services is available
            // In debug mode, log the status
            // Get the error dialog from Google Play services
            // If Google Play services can provide an error dialog
            // Create a new DialogFragment for the error dialog
    val isGooglePlayServicesAvailable: Boolean
        get() {
            val resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
            if (ConnectionResult.SUCCESS == resultCode) {
                Log.d("Location Updates", "Google Play services is available.")
                return true
            } else {
                val errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST)
                if (errorDialog != null) {
                    val errorFragment = ErrorDialogFragment()
                    errorFragment.dialog = errorDialog
                    errorFragment.show(supportFragmentManager, "Location Updates")
                }

                return false
            }
        }

    /*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
    override fun onConnected(dataBundle: Bundle?) {
        // Display the connection status
        val location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        if (location != null) {
            Toast.makeText(this, "GPS location was found!", Toast.LENGTH_SHORT).show()
            val latLng = LatLng(location.latitude, location.longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
            map!!.animateCamera(cameraUpdate)
        } else {
            Toast.makeText(this, "Current location was null, enable GPS on emulator!", Toast.LENGTH_SHORT).show()
        }
        startLocationUpdates()
    }

    fun startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this)
    }

    private fun initLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
        }
        return locationRequest
    }

    override fun onLocationChanged(location: Location) {
        // Report to the UI that the location was updated
        val msg = "Updated Location: ${location.latitude},${location.longitude}"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    }

    /*
     * Called by Location Services if the connection to the location client
     * drops because of an error.
     */
    override fun onConnectionSuspended(i: Int) {
        when (i) {
            GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED -> Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show()
            GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST -> Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show()
        }
    }

    /*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        /*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST)
                /*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (e: IntentSender.SendIntentException) {
                // Log the error
                e.printStackTrace()
            }

        } else {
            Toast.makeText(applicationContext,
                    "Sorry. Location services not available to you", Toast.LENGTH_LONG).show()
        }
    }

    // Define a DialogFragment that displays the error dialog
    class ErrorDialogFragment : DialogFragment() {

        // Global field to contain the error dialog
        private lateinit var mDialog: Dialog

        // Set the dialog to display
        fun setDialog(dialog: Dialog) {
            mDialog = dialog
        }

        // Return a Dialog to the DialogFragment.
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return mDialog
        }
    }

    companion object {

        /*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
        private val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000

        private val UPDATE_INTERVAL: Long = 60000  /* 60 secs */
        private val FASTEST_INTERVAL: Long = 5000 /* 5 secs */
    }

}
