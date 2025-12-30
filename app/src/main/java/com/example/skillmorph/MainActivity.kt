
package com.example.skillmorph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.skillmorph.ui.theme.SkillMorphTheme
import com.example.skillmorph.ui.theme.TransparentBlack
import com.example.skillmorph.utils.glassEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillMorphTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = TransparentBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassyBox()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassyBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(200.dp)
            .glassEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "I am Glassy!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SkillMorphTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassyBox()
        }
    }
}
