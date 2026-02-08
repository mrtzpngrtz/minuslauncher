package com.minimal.launcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
    
    // Store as JSON: [{"packageName": "com.example.app", "customLabel": "My App"}, ...]
    fun getSavedApps(): List<SavedApp> {
        val jsonString = prefs.getString("saved_apps_list", null) ?: return emptyList()
        val list = mutableListOf<SavedApp>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(SavedApp(
                    packageName = obj.getString("packageName"),
                    customLabel = if (obj.has("customLabel")) obj.getString("customLabel") else null
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
    
    fun saveApps(apps: List<SavedApp>) {
        val jsonArray = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject()
            obj.put("packageName", app.packageName)
            if (app.customLabel != null) {
                obj.put("customLabel", app.customLabel)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_apps_list", jsonArray.toString()).apply()
    }

    // Migration helper for old format (Set<String>)
    fun migrateFromLegacyIfNeeded() {
        if (!prefs.contains("saved_apps_list") && prefs.contains("selected_apps")) {
            val oldSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
            val savedApps = oldSet.map { SavedApp(it, null) }
            saveApps(savedApps)
            prefs.edit().remove("selected_apps").apply()
        }
    }
}

data class SavedApp(
    val packageName: String,
    val customLabel: String?
)
