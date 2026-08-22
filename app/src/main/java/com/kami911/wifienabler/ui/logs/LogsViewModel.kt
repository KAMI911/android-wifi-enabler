package com.kami911.wifienabler.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kami911.wifienabler.WifiEnablerApp
import com.kami911.wifienabler.data.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as WifiEnablerApp).database.logDao()

    val logs = dao.observeAll().asLiveData()

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAll()
        }
    }
}
