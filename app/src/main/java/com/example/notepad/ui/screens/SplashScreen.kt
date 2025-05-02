package com.example.notepad.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    val scaleAnimation = remember {
        Animatable(0f)
    }
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
        // Navigate to home screen after animation
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NotePad",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                modifier = Modifier.scale(scaleAnimation.value)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Image Notes Companion",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.scale(scaleAnimation.value)
            )
        }
    }
} 