package com.example.runnigtracker.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runnigtracker.R
import com.example.runnigtracker.adapters.RunAdapter
import com.example.runnigtracker.databinding.FragmentRunBinding
import com.example.runnigtracker.databinding.FragmentSetupBinding
import com.example.runnigtracker.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.runnigtracker.other.SortType
import com.example.runnigtracker.other.TrackingUtility
import com.example.runnigtracker.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import pub.devrel.easypermissions.EasyPermissions.onRequestPermissionsResult

@AndroidEntryPoint
class RunFragment: Fragment(R.layout.fragment_run), PermissionCallbacks {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var runAdapter: RunAdapter


    private var _binding: FragmentRunBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRunBinding.inflate(inflater, container, false)
        val view = binding.root




        viewModel.runsSortedByDate.observe(viewLifecycleOwner, Observer {
            runAdapter.submitList(it)
        })

        requestPermissions()
        setUpRecyclerView()
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when(viewModel.sortType){

            SortType.DATE -> binding.spFilter.setSelection(0)
            SortType.RUNNING_TIME -> binding.spFilter.setSelection(1)
            SortType.DISTANCE -> binding.spFilter.setSelection(2)
            SortType.AVG_SPEED -> binding.spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> binding.spFilter.setSelection(4)
        }


        binding.spFilter.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                when(position){

                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.DISTANCE)
                    3 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }



    }

    private fun setUpRecyclerView() = binding.rvRuns.apply {
        runAdapter  = RunAdapter()
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }





    private fun requestPermissions(){
        if(TrackingUtility.hasLocationPermissions(requireContext())){
            return
        }
        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.R){
            EasyPermissions.requestPermissions(
                this,
                "You need to Accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }else{
            EasyPermissions.requestPermissions(
                this,
                "You need to Accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION


            )
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

        if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()

        }else{
            requestPermissions()
        }

    }



}