package com.example.runnigtracker.repositories

import com.example.runnigtracker.db.Run
import com.example.runnigtracker.db.RunDao
import javax.inject.Inject


class MainRepository @Inject constructor(
    val runDao: RunDao
) {

    suspend fun insertRun(run: Run) = runDao.insertRun(run)

    suspend fun deleteRun(run: Run) = runDao.insertRun(run)

    fun getAllRunsStoredByDate() = runDao.getALLRunsSortedByDate()

    fun getAllRunsStoredByDistance() = runDao.getAllRunsSortedByDistance()

    fun getAllRunsStoredByCaloriesBurned() = runDao.getAllRunsSortedByCaloriesBurned()

    fun getAllRunsStoredByTimeInMillis() = runDao.getAllRunsSortedByTimeInMillis()

    fun getAllRunsStoredByAvgSpeed() = runDao.getAllRunsSortedByAvrSpeed()

   fun getTotalAvgSpeed() = runDao.getTotalAvgSpeed()

   fun getTotalCaloriesBurned() = runDao.getTotalCaloriesBurned()

   fun getTotalTimeInMillis() = runDao.getTotalTimeInMillis()

   fun getTotalDistance() = runDao.getTotalDistance()

}