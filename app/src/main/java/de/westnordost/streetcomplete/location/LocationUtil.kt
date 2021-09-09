package de.westnordost.streetcomplete.location

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

fun isLocationEnabled(context: Context): Boolean =
    hasLocationPermission(context) && LocationManagerCompat
        .isLocationEnabled(ContextCompat.getSystemService(context, LocationManager::class.java)!!)

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun createLocationAvailabilityIntentFilter(): IntentFilter =
    IntentFilter(LocationManager.MODE_CHANGED_ACTION)

fun Location.toLatLon(): LatLon = LatLon(latitude, longitude)
