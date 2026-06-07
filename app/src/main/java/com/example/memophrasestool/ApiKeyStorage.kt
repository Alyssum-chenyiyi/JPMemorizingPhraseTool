package com.example.memophrasestool

import android.content.Context

object ApiKeyStorage {

    private const val PREF_NAME = "settings"
    private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"

    fun saveApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEEPSEEK_API_KEY, apiKey)
            .apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DEEPSEEK_API_KEY)
            .apply()
    }
}