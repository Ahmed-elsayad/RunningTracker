package com.example.runnigtracker.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.readPendingIntentOrNullFromParcel

import com.google.android.gms.location.FusedLocationProviderClient
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runnigtracker.R
import com.example.runnigtracker.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runnigtracker.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runnigtracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runnigtracker.other.Constants.ACTION_STOP_SERVICE
import com.example.runnigtracker.other.Constants.FAST_LOCATION_INTERVAL
import com.example.runnigtracker.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runnigtracker.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runnigtracker.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runnigtracker.other.Constants.NOTIFICATION_ID
import com.example.runnigtracker.other.Constants.TIME_UPDATE_INTERVAL
import com.example.runnigtracker.other.TrackingUtility
import com.example.runnigtracker.services.TrackingServices.Companion.isTracking
import com.example.runnigtracker.ui.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingServices: LifecycleService() {

    var isFirstRun = true
    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)

    }

    private fun addEmptyPolyLine() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))


    private fun addPathPoints(location: Location?) {

        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(position)
                pathPoints.postValue(this)

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean){

        if (isTracking){
            if (TrackingUtility.hasLocationPermissions(this)){
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FAST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
        else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }




      val locationCallback = object: LocationCallback(){
          override fun onLocationResult(result: LocationResult) {
              super.onLocationResult(result)
              if (isTracking.value!!){
                  result?.locations?.let { locations ->
                      for (location in locations){
                       addPathPoints(location)
                          Timber.d("THE LOCATION: ${location.latitude}, ${location.longitude}")
                      }
                  }
              }
          }
      }

    override fun onCreate() {
        super.onCreate()
        currentNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }


    private fun updateNotificationTrackingState(isTracking: Boolean){

      val notificationActionText = if(isTracking) "Pause" else "Resume"

        val pendingIntent = if(isTracking){
            val pauseIntent = Intent(this, TrackingServices::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this,1,pauseIntent, FLAG_UPDATE_CURRENT)
        }else{
           val resumeIntent = Intent(this,TrackingServices::class.java).apply {
               action = ACTION_START_OR_RESUME_SERVICE
           }
            PendingIntent.getService(this,2,resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }


         if(!serviceKilled) {
             currentNotificationBuilder = baseNotificationBuilder
                 .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
             notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
         }
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

         intent?.let {


             when(it.action){

                 ACTION_START_OR_RESUME_SERVICE -> {
                     if (isFirstRun){
                         startForegroundService()
                         isFirstRun = false

                     }else{
                         Timber.d("Resuming service.....")
                         startTimer()
                     }

                 }


               ACTION_PAUSE_SERVICE -> {
                     Timber.d("pause service")
                   pauseService()
                 }


             ACTION_STOP_SERVICE -> {
                     Timber.d("stop service")
                     killService()
                 }


             }
         }


        return super.onStartCommand(intent, flags, startId)


    }

    private fun killService(){
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer(){
        addEmptyPolyLine()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
         isTimerEnabled = true


        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIME_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }

    }

    private fun pauseService (){
        isTracking.postValue(false)
        isTimerEnabled = false

    }

    private fun startForegroundService() {

        startTimer()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
        as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            notificationChannel(notificationManager)
        }
// 3

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, Observer {
           if(!serviceKilled) {
               val notification = currentNotificationBuilder
                   .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000L))
               notificationManager.notify(NOTIFICATION_ID, notification.build())
           }
        })





    }

// 1
    // create notification channel
    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannel(notificationManager: NotificationManager){

        val channel = NotificationChannel(

             NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)


    }
// 2


}