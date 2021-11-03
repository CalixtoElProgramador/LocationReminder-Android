package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.createTitle
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = SelectLocationFragment::class.java.simpleName
    private val REQUEST_PERMISSION_LOCATION = 1

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var selectedPoi: PointOfInterest
    private lateinit var snackbar: Snackbar
    private lateinit var snackbar02: Snackbar
    private lateinit var marker: Marker

    lateinit var geofencingClient: GeofencingClient

    companion object {
        const val ACTION_GEOFENCE_EVENT = "GEOFENCE_EVENT"
        internal const val GEOFENCE_RADIUS_METERS = 50f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveLocation.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snackbar = Snackbar.make(
            view,
            R.string.location_required_error,
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar02 = Snackbar.make(
            view,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d(TAG, "Map is ready")

        enableLocation()
        setMapLongClick(map)
        setMapStyle(map)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (isPermissionGranted()) {
            Log.d(TAG, "permissions granted, preparing user location")
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    run {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val lat = location.latitude
                            val long = location.longitude
                            val currentPosition = LatLng(lat, long)
                            val zoom = 15f
                            marker = map.addMarker(
                                MarkerOptions().position(currentPosition)
                                    .title(getString(R.string.you_are_here))
                            )
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom))
                        }

                        setPoiClick(map)
                    }
                }

        } else {
            requestLocationPermission()
        }
    }

    /*
    * MAP STYLING FUNCTIONS
    * */
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear(); marker.remove()
            selectedPoi = poi
            marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            marker.showInfoWindow()
            binding.saveLocation.visibility = View.VISIBLE
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val title = createTitle(latLng)
            selectedPoi = PointOfInterest(latLng, title, title)
            map.clear(); marker.remove()
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            binding.saveLocation.visibility = View.VISIBLE
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Unable to find style. Error: ", e)
        }
    }

    /*
    * LOCATION PERMISSION FUNCTIONS
    * */
    @TargetApi(29)
    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    }

    @TargetApi(29)
    private fun requestLocationPermission() {
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (shouldProvideRationale) {
            snackbar.setAction(R.string.settings) {
                requestPermissions(permissionsArray, REQUEST_PERMISSION_LOCATION)
            }.show()
        } else {
            requestPermissions(permissionsArray, REQUEST_PERMISSION_LOCATION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        Log.d(TAG, "onRequestPermissionsResult")
        if (grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED
        ) {
            // Create an action that opens the settings for the specific app
            snackbar02.setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            enableLocation()
        }
    }

    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = selectedPoi
        _viewModel.latitude.value = selectedPoi.latLng.latitude
        _viewModel.longitude.value = selectedPoi.latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = selectedPoi.name
        parentFragmentManager.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::snackbar.isInitialized && ::snackbar02.isInitialized) {
            snackbar.dismiss(); snackbar02.dismiss()
        }
    }
}
