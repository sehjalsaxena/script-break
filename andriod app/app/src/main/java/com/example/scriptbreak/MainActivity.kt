package com.example.scriptbreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.scriptbreak.ui.DemoNavHost
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScriptBreakTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoNavHost()
                }
            }
        }
    }
}