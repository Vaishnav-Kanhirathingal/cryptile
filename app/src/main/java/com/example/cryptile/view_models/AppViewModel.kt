package com.example.cryptile.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cryptile.app_data.room_files.SafeDao
import com.example.cryptile.app_data.room_files.SafeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TAG = "AppViewModel"

class AppViewModel(private val safeDao: SafeDao) : ViewModel() {

    private val default = "UNKNOWN"
    private var _userDisplayName = MutableLiveData(default)
    private var _userEmail = MutableLiveData(default)
    private var _userPhotoUrl = MutableLiveData(default)

    val userDisplayName: LiveData<String> get() = _userDisplayName
    val userEmail: LiveData<String> get() = _userEmail
    val userPhotoUrl: LiveData<String> get() = _userPhotoUrl

    fun setData(displayName: String, email: String, photoUrl: String) {
        this._userDisplayName.value = displayName
        this._userEmail.value = email
        this._userPhotoUrl.value = photoUrl
        Log.d(TAG, "displayName = $displayName, email = $email, photoUrl = $photoUrl")
    }

    fun getListOfIds(): Flow<List<Int>> = safeDao.getListOfIds()
    fun getById(id: Int): Flow<SafeData> = safeDao.getById(id)

    fun insert(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.insert(safeData) }
    }

    fun update(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.update(safeData) }
    }

    fun delete(safeData: SafeData) {
        CoroutineScope(Dispatchers.IO).launch { safeDao.delete(safeData) }
    }

    fun deleteAll() {
        CoroutineScope(Dispatchers.IO).launch { safeDao.deleteAll() }
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