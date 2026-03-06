
package com.skillmorph.skillmorph.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.RepeatMode.Reverse
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.skillmorph.skillmorph.R
import com.skillmorph.skillmorph.presentation.main.MainScreen
import com.skillmorph.skillmorph.utils.GoogleAuthUiClient
import com.skillmorph.skillmorph.utils.glassEffect
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    appNavController: NavHostController
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Show a toast for any errors from the ViewModel
    LaunchedEffect(authState.error) {
        authState.error?.let {
            Toast.makeText(context, "Sign-in failed: $it", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Auth Logic
        if (authState.isLoading) {
            // Show a loading indicator
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        } else if (authState.user != null) {
            // If user is logged in, show a welcome message and sign-out button
//            Text("Welcome, ${authState.user?.displayName ?: "User"}!")
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = { viewModel.signOut() }) {
//                Text("Sign Out")
//            }
            MainScreen(appNavController = appNavController)
        } else {
            // If user is not logged in, show the sign-in button
            Text(
                text = "SkillMorph",
                fontSize = 28.sp, // Use fontSize with sp for text scaling
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Text(
                    text = "O",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color.Red)
                Text(
                    text = "S",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color(0xFF9C27B0))
            }

            Spacer(modifier = Modifier.height(100.dp)) // Added space for better layout

            AgentRingFace(
                modifier = Modifier.size(300.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))

            SignInButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val googleIdOption = GoogleAuthUiClient.getGoogleIdOption()
                            val credentialRequest = GoogleAuthUiClient.getCredentialRequest(googleIdOption)
                            val credentialManager = GoogleAuthUiClient.getCredentialManager(context)

                            val activity = context.findActivity()
                            if (activity != null) {
                                val result = credentialManager.getCredential(activity, credentialRequest)
                                viewModel.signInWithGoogle(result)
                            }

                        } catch (e: GetCredentialCancellationException) {
                            // User cancelled, do nothing
                        } catch (e: GetCredentialException) {
                            Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(80.dp))

            TermsAndPrivacyText()
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun SignInButton(onClick: () -> Unit) {
    GlowingButtonContainer(
        modifier = Modifier.size(width = 280.dp, height = 60.dp),
        glowColor = Color(0xFF25C0CB) // Google blue glow
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .glassEffect(),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(35.dp)
                    )
                    Text(
                        text = "Continue With Google",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// Simple particle data
data class LoginParticle(
    val initialAngle: Float,
    val distanceFromCenter: Float,
    val size: Float,
    val opacity: Float
)

@Composable
fun AgentRingFace(
    modifier: Modifier = Modifier,
    circleColor: Color = Color.Cyan
) {
    // 1. Create the Particles (Once)
    val particles = remember {
        val list = mutableListOf<LoginParticle>()
        repeat(450) {
            list.add(
                LoginParticle(
                    initialAngle = Random.nextFloat() * 360f,
                    // 200f is the inner radius (How big the hole in the middle is)
                    // 80f is the thickness of the ring band
                    distanceFromCenter = 250f + Random.nextFloat() * 100f,
                    size = Random.nextFloat() * 10f + 2f,
                    opacity = Random.nextFloat()
                )
            )
        }
        list
    }

    // 2. Infinite Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "ring_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing), // 8 seconds for full circle
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    // 3. Pulse Animation (Subtle breathing effect)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = Reverse
        ), label = "pulse"
    )

    // 4. Drawing
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)

        particles.forEach { p ->
            // Add global rotation to the particle's fixed angle
            val currentAngle = (p.initialAngle + rotationAngle) % 360
            val rad = Math.toRadians(currentAngle.toDouble())

            // Apply Pulse to distance
            val currentDist = p.distanceFromCenter * pulseScale // This scales the ring size slightly

            // Polar to Cartesian
            val x = center.x + (cos(rad) * currentDist).toFloat()
            val y = center.y + (sin(rad) * currentDist).toFloat()

            drawCircle(
                color = circleColor.copy(alpha = p.opacity * 0.8f), // Varied opacity for depth
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun GlowingButtonContainer(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF03E6FB),
    cornerRadius: Dp = 30.dp,
    glowRadius: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val paint = Paint().asFrameworkPaint().apply {
                color = glowColor.copy(alpha = 0.8f).toArgb()
                maskFilter =
                    android.graphics.BlurMaskFilter(
                        glowRadius.toPx(),
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
            }

            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadius.toPx(),
                    cornerRadius.toPx(),
                    paint
                )
            }
        }

        content()
    }
}


@Composable
fun TermsAndPrivacyText() {
    val uriHandler = LocalUriHandler.current
    val termsUrl = "https://docs.google.com/document/d/1s4IG5uGuQZm4R59xJAQJV_SZ6Vu2ndNc_3tLmrk7BX8/edit?tab=t.9yo9c1ekezmq"
    val privacyUrl = "https://docs.google.com/document/d/1s4IG5uGuQZm4R59xJAQJV_SZ6Vu2ndNc_3tLmrk7BX8/edit?tab=t.0"

    val annotatedString = buildAnnotatedString {
        append("By continuing, you agree to our \n")
        
        pushStringAnnotation(tag = "terms", annotation = termsUrl)
        withStyle(
            style = SpanStyle(
                color = Color(0xFF00E5FF),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append("Terms of Service")
        }
        pop()
        
        append(" and ")
        
        pushStringAnnotation(tag = "privacy", annotation = privacyUrl)
        withStyle(
            style = SpanStyle(
                color = Color(0xFF00E5FF), 
                textDecoration = TextDecoration.Underline, 
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append("Privacy Policy")
        }
        pop()
        
        append(".")
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset).firstOrNull()?.let {
                uriHandler.openUri(it.item)
            }
            annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset).firstOrNull()?.let {
                uriHandler.openUri(it.item)
            }
        },
        style = androidx.compose.ui.text.TextStyle(
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
