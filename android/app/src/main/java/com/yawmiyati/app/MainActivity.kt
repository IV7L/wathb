package com.yawmiyati.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yawmiyati.app.navigation.YawmiyatiNavGraph
import com.yawmiyati.app.ui.theme.YawmiyatiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YawmiyatiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    YawmiyatiNavGraph()
                }
            }
        }
    }
}
