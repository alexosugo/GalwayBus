package com.surrus.galwaybus.ui


import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener

import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.AndroidInjection
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_nearby.*
import java.util.logging.Logger
import javax.inject.Inject


class NearbyFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private lateinit var stopsLocationCenter: Location

    @Inject lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory
    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel

    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter

    companion object {
        fun newInstance(): NearbyFragment {
            return NearbyFragment()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        nearestBusStopsViewModel = ViewModelProviders.of(activity, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize recycler view
        with (busStopsList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            busStopsAdapter = BusStopsRecyclerViewAdapter()
            adapter = busStopsAdapter
        }


        // subscribe to updates
        nearestBusStopsViewModel.busStops.observe(this) {
            busStopsAdapter.busStopList = it!!
            busStopsAdapter.notifyDataSetChanged()
        }


        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    @SuppressLint("MissingPermission")
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {

                        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                        mapFragment.getMapAsync(this@NearbyFragment)
                    }
                }).check()

    }


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nearby, container, false)
    }




    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true


        if (nearestBusStopsViewModel.getLocation() == null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    //val myLocation = Location(53.273849, -9.049695)

                    stopsLocationCenter = Location(location.latitude, location.longitude)
                    val zoomLevel = 12.0f
                    nearestBusStopsViewModel.setLocationZoomLevel(stopsLocationCenter, zoomLevel)

                    val latLng = LatLng(stopsLocationCenter.latitude, stopsLocationCenter.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
                }
            }
        } else {
            stopsLocationCenter = nearestBusStopsViewModel.getLocation() as Location
            val latLng = LatLng(stopsLocationCenter.latitude, stopsLocationCenter.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, nearestBusStopsViewModel.getZoomLevel()))
        }


        nearestBusStopsViewModel.busStops.observe(this) {
            updateMap(it!!)
        }

        map.setOnCameraIdleListener{
            val cameraPosition = map.getCameraPosition()

            stopsLocationCenter = Location(cameraPosition.target.latitude, cameraPosition.target.longitude)
            nearestBusStopsViewModel.setLocationZoomLevel(stopsLocationCenter, cameraPosition.zoom)

            nearestBusStopsViewModel.fetchNearestBusStops(stopsLocationCenter)

        }

    }


    private fun updateMap(busStopList: List<BusStop>) {

        if (map != null && busStopList.size > 0) {

            val builder = LatLngBounds.Builder()
            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude);
                val marker = map.addMarker(MarkerOptions().position(busStopLocation).title(busStop.shortName))
                marker.tag = busStop.stopRef
                builder.include(busStopLocation)
            }
            //map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))


            map.setOnInfoWindowClickListener {
                val stopRef = it.tag as String
            }

            map.setOnMarkerClickListener {
                val stopRef = it.tag as String
                false
            }


        }
    }
}

