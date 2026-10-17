package com.mofa.paymentalert.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(val title: String, val icon: ImageVector) {
    SETUP("Setup", Icons.Filled.Settings),
    ALERTS("Owner alerts", Icons.Filled.Notifications),
    PAYMENTS("Payments", Icons.AutoMirrored.Filled.List),
    LOGS("Logs", Icons.Filled.Build),
    ABOUT("About", Icons.Filled.Info),
}
