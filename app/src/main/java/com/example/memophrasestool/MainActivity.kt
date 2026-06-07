package com.example.memophrasestool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.memophrasestool.ui.theme.MemoPhrasesToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemoPhrasesToolTheme {
                val vm: PhraseViewModel = viewModel()
                PhraseApp(vm)
            }
        }
    }
}