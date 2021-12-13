package com.example.runnigtracker.other

import android.graphics.Color

object Constants {

    const val RUNNING_DATABASE_NAME = "running_db"
    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    const val ACTION_START_OR_RESUME_SERVICE = "action start or resume service"
    const val ACTION_PAUSE_SERVICE = "action pause service"
    const val ACTION_STOP_SERVICE = "action stop service"

   const val ACTION_SHOW_TRACKING_FRAGMENT ="show tracking fragment"

    const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME ="Tracking"
    const val NOTIFICATION_ID = 1

    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FAST_LOCATION_INTERVAL = 2000L

    const val  POLYLINE_COLOR = Color.RED
    const val POLYLINE_WIDTH = 8f
    const val  MAP_ZOOM = 15f

    const val TIME_UPDATE_INTERVAL = 50L


}