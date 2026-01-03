
package com.example.skillmorph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.skillmorph.presentation.auth.AuthScreen
import com.example.skillmorph.presentation.auth.AuthViewModel
import com.example.skillmorph.presentation.navigation.AppNavigation
import com.example.skillmorph.ui.theme.SkillMorphTheme
import com.example.skillmorph.ui.theme.TransparentBlack
import com.example.skillmorph.utils.glassEffect
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillMorphTheme {
                AppNavigation()
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SkillMorphTheme {

    }
}
