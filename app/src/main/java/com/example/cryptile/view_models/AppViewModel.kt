package com.example.cryptile.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cryptile.app_data.room_files.SafeDao
import com.example.cryptile.app_data.room_files.SafeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppViewModel(private val safeDao: SafeDao) : ViewModel() {
    fun getListOfIds(): Flow<List<Int>> {
        return safeDao.getListOfIds()
    }

    fun getById(id: Int): Flow<SafeData> {
        return safeDao.getById(id)
    }

    fun insert(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.insert(safeData) }
    }

    fun update(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.update(safeData) }
    }

    fun delete(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.delete(safeData) }
    }
}

class AppViewModelFactory(private val safeDao: SafeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(safeDao) as T
        }
        throw IllegalArgumentException("view model factory error, unknown factory")
    }
}