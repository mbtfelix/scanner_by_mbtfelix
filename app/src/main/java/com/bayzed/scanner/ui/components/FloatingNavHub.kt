package com.bayzed.scanner.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.bayzed.scanner.ui.viewmodel.AppScreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FloatingNavHub(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = expanded) {
        expanded = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val rotationAnim by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rotation"
    )

    val transition = updateTransition(targetState = expanded, label = "hubTransition")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        expanded = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            )
        }

        val nodeOffset1 by transition.animateDp(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
            label = "node1"
        ) { if (it) 90.dp else 0.dp }

        val nodeOffset2 by transition.animateDp(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
            label = "node2"
        ) { if (it) 90.dp else 0.dp }

        val nodeOffset3 by transition.animateDp(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
            label = "node3"
        ) { if (it) 90.dp else 0.dp }

        Box(
            modifier = Modifier
                .padding(end = 24.dp, bottom = 28.dp)
                .size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (expanded) {
                val angle1 = Math.toRadians(-180.0)
                val x1 = (nodeOffset1.value * cos(angle1)).dp
                val y1 = (nodeOffset1.value * sin(angle1)).dp
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScreenSelected(AppScreen.HISTORY)
                    },
                    modifier = Modifier
                        .offset(x = x1, y = y1)
                        .size(48.dp),
                    containerColor = if (currentScreen == AppScreen.HISTORY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (currentScreen == AppScreen.HISTORY) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }

                val angle2 = Math.toRadians(-135.0)
                val x2 = (nodeOffset2.value * cos(angle2)).dp
                val y2 = (nodeOffset2.value * sin(angle2)).dp
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScreenSelected(AppScreen.SCANNER)
                    },
                    modifier = Modifier
                        .offset(x = x2, y = y2)
                        .size(48.dp),
                    containerColor = if (currentScreen == AppScreen.SCANNER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (currentScreen == AppScreen.SCANNER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                }

                val angle3 = Math.toRadians(-90.0)
                val x3 = (nodeOffset3.value * cos(angle3)).dp
                val y3 = (nodeOffset3.value * sin(angle3)).dp
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScreenSelected(AppScreen.GENERATOR)
                    },
                    modifier = Modifier
                        .offset(x = x3, y = y3)
                        .size(48.dp),
                    containerColor = if (currentScreen == AppScreen.GENERATOR) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (currentScreen == AppScreen.GENERATOR) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = "Create QR")
                }
            }

            FloatingActionButton(
                onClick = {
                    expanded = !expanded
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.MenuOpen,
                    contentDescription = "Navigation Hub",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationAnim)
                )
            }
        }
    }
}
