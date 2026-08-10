package io.jadu.promptpong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.jadu.promptpong.di.initPromptPong

class MainActivity : ComponentActivity() {
    // Hey, you found the front door. Open this when you are ready to launch the game.
    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initPromptPong(this)

        setContent {
            App()
        }
    }
    */
}
