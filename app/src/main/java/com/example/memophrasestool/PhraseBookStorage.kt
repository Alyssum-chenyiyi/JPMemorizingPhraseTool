package com.example.memophrasestool

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PhraseBookStorage {

    private const val PREF_NAME = "phrase_books"
    private const val KEY_BOOKS = "books"

    fun saveBooks(context: Context, books: List<PhraseBook>) {
        val json = Gson().toJson(books)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOOKS, json)
            .apply()
    }

    fun loadBooks(context: Context): List<PhraseBook> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BOOKS, null) ?: return emptyList()

        val type = object : TypeToken<List<PhraseBook>>() {}.type
        return Gson().fromJson(json, type)
    }
}