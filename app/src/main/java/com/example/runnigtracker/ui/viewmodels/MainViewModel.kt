package com.example.runnigtracker.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runnigtracker.db.Run
import com.example.runnigtracker.other.SortType
import com.example.runnigtracker.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
            val repository: MainRepository
): ViewModel() {

    fun insertRun(run: Run) = viewModelScope.launch {
        repository.insertRun(run)
    }

    val runsSortedByDate = repository.getAllRunsStoredByDate()
    val runsSortedByDistance = repository.getAllRunsStoredByDistance()
    val runsSortedByCaloriesBurned = repository.getAllRunsStoredByCaloriesBurned()
    val runsSortedByTimeInMillis = repository.getAllRunsStoredByTimeInMillis()
    val runsSortedByAvgSpeed = repository.getAllRunsStoredByAvgSpeed()

    val runs = MediatorLiveData<List<Run>>()

    var sortType = SortType.DATE

    init {

        runs.addSource(runsSortedByDate){
            if (sortType == SortType.DATE){
                it?.let {
                    runs.value = it
                }
            }
        }


        runs.addSource(runsSortedByDistance){
            if (sortType == SortType.DISTANCE){
                it?.let {
                    runs.value = it
                }
            }
        }


        runs.addSource(runsSortedByCaloriesBurned){
            if (sortType == SortType.CALORIES_BURNED){
                it?.let {
                    runs.value = it
                }
            }
        }


        runs.addSource(runsSortedByTimeInMillis){
            if (sortType == SortType.RUNNING_TIME){
                it?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByAvgSpeed){
            if (sortType == SortType.AVG_SPEED){
                it?.let {
                    runs.value = it
                }
            }
        }



    }
    fun sortRuns(sortType: SortType) = when(sortType){
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByAvgSpeed.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
        SortType.DATE -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
    }
}