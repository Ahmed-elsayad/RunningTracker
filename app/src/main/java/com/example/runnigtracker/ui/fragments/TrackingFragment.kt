package com.example.runnigtracker.ui.fragments

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.runnigtracker.R
import com.example.runnigtracker.databinding.FragmentTrackingBinding
import com.example.runnigtracker.db.Run
import com.example.runnigtracker.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runnigtracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runnigtracker.other.Constants.ACTION_STOP_SERVICE
import com.example.runnigtracker.other.Constants.MAP_ZOOM
import com.example.runnigtracker.other.Constants.POLYLINE_COLOR
import com.example.runnigtracker.other.Constants.POLYLINE_WIDTH
import com.example.runnigtracker.other.TrackingUtility
import com.example.runnigtracker.services.Polyline
import com.example.runnigtracker.services.TrackingServices
import com.example.runnigtracker.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val viewModel: MainViewModel by viewModels()

    private var menu: Menu? = null


    var weight = 80f



    private var isTracking = false

    private var pathPoints = mutableListOf<Polyline>()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    private var map: GoogleMap? = null

    private var currentTimeInMillis = 0L





    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun toggleRun(){

        if (isTracking){

            sendCommandToService(ACTION_PAUSE_SERVICE)
            menu?.getItem(0)?.isVisible = true

        }else{

            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

private fun stopRun(){

    sendCommandToService(ACTION_STOP_SERVICE)

    findNavController()
        .navigate(R.id.action_trackingFragment_to_runFragment)
}

    private fun showCancelTrackingDialog(){

        val dialog = MaterialAlertDialogBuilder(requireContext(),R.style.AlertDialogTheme)
            .setTitle("Cancel the Run ?")
            .setMessage("Are you sure to cancel the current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("yes"){_,_ ->
                stopRun()
            }
            .setNegativeButton("No") { dialogInterface , _ ->
                dialogInterface.cancel()

            }
            .create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu,menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (currentTimeInMillis > 0L){
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.btnCancelTracking ->{
                showCancelTrackingDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }




    private fun updateTracking(isTracking: Boolean){
        this.isTracking = isTracking

        if (!isTracking){
           binding.btnToggleRun.text = "Start"
           binding.btnFinishRun.visibility = View.VISIBLE
        }else{
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.GONE
            menu?.getItem(0)?.isVisible = true

        }
    }

    private fun addAllPolyLines(){
        for(polyline in pathPoints){

            val polyLineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polyLineOptions)
        }
    }

    private fun addLatestPolyline(){

        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){

            val preLastLatLng = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLng = pathPoints.last().last()
            val polyLineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polyLineOptions)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         setHasOptionsMenu(true)
        _binding = FragmentTrackingBinding.inflate(inflater,container,false)
        val view = binding.root


        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)

        binding.btnFinishRun.setOnClickListener {

            zoomToSeeWholeTrack()
            endRunAndSaveToDb()

        }


        binding.btnToggleRun.setOnClickListener {

            toggleRun()
        }

        binding.mapView.getMapAsync {
            map = it

            addAllPolyLines()
        }
        subscribeToObservers()


    }

    private fun endRunAndSaveToDb() {

        map?.snapshot {

            bitmap ->
            var distanceInMeters = 0

            for (polyline in pathPoints){

                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            val avgSpeed = round((distanceInMeters / 1000f) / (currentTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()

            val run = Run(bitmap, dateTimestamp, avgSpeed, distanceInMeters, currentTimeInMillis, caloriesBurned)

            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()

        }
    }

    private fun zoomToSeeWholeTrack() {

        val bounds = LatLngBounds.Builder()

        for (polyline in pathPoints){
            for (pos in polyline){
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height * 0.05f).toInt()
            )
        )

    } 


    private fun subscribeToObservers() {

        TrackingServices.isTracking.observe(viewLifecycleOwner , Observer {
            updateTracking(it)
        })
        TrackingServices.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingServices.timeRunInMillis.observe(viewLifecycleOwner, Observer {
               currentTimeInMillis = it
               val formattedTime = TrackingUtility.getFormattedStopWatchTime(currentTimeInMillis,true)
               binding.tvTimer.text = formattedTime

        })
    }


    private fun sendCommandToService (action: String) =
        Intent(requireContext(), TrackingServices::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }





}