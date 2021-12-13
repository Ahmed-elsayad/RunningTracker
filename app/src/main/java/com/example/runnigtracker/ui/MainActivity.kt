package com.example.runnigtracker.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runnigtracker.R
import com.example.runnigtracker.databinding.ActivityMainBinding
import com.example.runnigtracker.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

         val navHostFragment = findNavController(R.id.navHostFragment)

        navigateToTrackingFragmentIfNeeded(intent)
        setSupportActionBar(binding.toolbar)
        binding.bottomNavigationView.setupWithNavController(navHostFragment)

        navHostFragment.addOnDestinationChangedListener { _, destination, _ ->

         when(destination.id){
             R.id.settingsFragment,R.id.runFragment,R.id.statisticsFragment->
                 binding.bottomNavigationView.visibility = View.VISIBLE
             else -> binding.bottomNavigationView.visibility = View.GONE
         }


     }



    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?){
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT){
            Navigation.findNavController(this, R.id.navHostFragment) .navigate(R.id.action_global_trackingFragment)

        }
    }
}