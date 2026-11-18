package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

object LocationHelper {
    /**
     * Rounds raw coordinates to ~1.1km grid precision so exact user GPS is never exposed publicly.
     */
    fun quantizeCoordinate(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    fun formatApproximateDistance(meters: Float): String {
        return when {
            meters < 150f -> "~100 m away"
            meters < 1000f -> "~${((meters / 50f).roundToInt() * 50)} m away"
            meters < 10000f -> "~${String.format("%.1f", meters / 1000f)} km away"
            else -> "~${(meters / 1000f).roundToInt()} km away"
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchApproximateLocation(context: Context): Pair<Double, Double>? {
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(quantizeCoordinate(loc.latitude) to quantizeCoordinate(loc.longitude))
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    cont.resume(quantizeCoordinate(last.latitude) to quantizeCoordinate(last.longitude))
                                } else {
                                    cont.resume(null)
                                }
                            }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }
}
