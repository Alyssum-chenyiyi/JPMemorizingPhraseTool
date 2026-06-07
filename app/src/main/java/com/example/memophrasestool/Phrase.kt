package com.example.memophrasestool

data class PhraseBook(
    val id: Long,
    val name: String,
    val phrases: List<Phrase>
)

data class Phrase(
    val id: Long,
    val chinese: String,
    val english: String,
    val stage1Forgot: Int = 0,
    val stage2Wrong: Int = 0,
    val stage3Wrong: Int = 0,
    val passed: Boolean = false
)