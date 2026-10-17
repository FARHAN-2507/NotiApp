package com.mofa.paymentalert.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun StatusPill(text: String, container: Color, content: Color) {
    Surface(color = container, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

fun sourceLabel(source: String, sourceDetail: String): String = when (source) {
    "NOTIFICATION" -> when (sourceDetail) {
        "com.phonepe.app" -> "PhonePe"
        else -> sourceDetail.substringAfterLast('.')
    }
    else -> "SMS"
}
