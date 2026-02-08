package com.minimal.launcher

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

data class AppInfo(
    val label: String,
    val packageName: String,
    val customLabel: String? = null,
    val isSelected: Boolean = false
) {
    fun getDisplayName(): String = customLabel ?: label
}

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val pm: PackageManager = application.packageManager
    private val prefs = PreferencesManager(application)
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val selectedApps: StateFlow<List<AppInfo>> = _selectedApps.asStateFlow()
    
    init {
        prefs.migrateFromLegacyIfNeeded()
        loadApps()
    }
    
    fun loadApps() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val savedApps = prefs.getSavedApps() // List<SavedApp>
            val savedPackageNames = savedApps.map { it.packageName }.toSet()
            
            // Map installed apps
            val allApps = resolveInfos.map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                
                // Check if saved
                val savedApp = savedApps.find { it.packageName == packageName }
                
                AppInfo(
                    label = label,
                    packageName = packageName,
                    customLabel = savedApp?.customLabel,
                    isSelected = savedApp != null
                )
            }.sortedBy { it.label.lowercase() }
            
            _installedApps.value = allApps
            
            // Reconstruct selected apps in saved order
            val selectedList = mutableListOf<AppInfo>()
            savedApps.forEach { saved ->
                val app = allApps.find { it.packageName == saved.packageName }
                if (app != null) {
                    selectedList.add(app)
                }
            }
            _selectedApps.value = selectedList
        }
    }
    
    fun toggleAppSelection(app: AppInfo) {
        val currentSelected = _selectedApps.value.toMutableList()
        val isSelecting = !app.isSelected
        
        if (isSelecting && currentSelected.size >= 10) {
            return // Max limit reached
        }
        
        // Update installed list selection state
        _installedApps.value = _installedApps.value.map { 
            if (it.packageName == app.packageName) it.copy(isSelected = isSelecting) else it
        }
        
        if (isSelecting) {
            // Add to end
            currentSelected.add(app.copy(isSelected = true))
        } else {
            // Remove
            currentSelected.removeAll { it.packageName == app.packageName }
        }
        
        _selectedApps.value = currentSelected
        saveCurrentState()
    }
    
    fun reorderApps(fromIndex: Int, toIndex: Int) {
        val list = _selectedApps.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            Collections.swap(list, fromIndex, toIndex)
            _selectedApps.value = list
            saveCurrentState()
        }
    }
    
    fun renameApp(app: AppInfo, newName: String) {
        val nameToSave = if (newName.isBlank() || newName == app.label) null else newName
        
        // Update selected list
        _selectedApps.value = _selectedApps.value.map {
            if (it.packageName == app.packageName) it.copy(customLabel = nameToSave) else it
        }
        
        // Update installed list
        _installedApps.value = _installedApps.value.map {
            if (it.packageName == app.packageName) it.copy(customLabel = nameToSave) else it
        }
        
        saveCurrentState()
    }
    
    private fun saveCurrentState() {
        val savedApps = _selectedApps.value.map { 
            SavedApp(it.packageName, it.customLabel)
        }
        prefs.saveApps(savedApps)
    }
}
