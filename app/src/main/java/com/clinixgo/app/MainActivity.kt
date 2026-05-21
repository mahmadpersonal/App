package com.clinixgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.clinixgo.app.ai.SafeOfflineAiService
import com.clinixgo.app.data.ClinixRepository
import com.clinixgo.app.ui.screens.ClinixGoApp
import com.clinixgo.app.ui.theme.ClinixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repository = remember { ClinixRepository(applicationContext) }
            val aiService = remember { SafeOfflineAiService() }
            ClinixTheme {
                ClinixGoApp(repository = repository, aiService = aiService)
            }
        }
    }
}
