package com.flowspark.app.ui.screens.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** 图片预览区域（顶部） */
@Composable
fun ImagePreview(
    uri: Uri?,
    modifier: Modifier = Modifier,
) {
    if (uri == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = uri,
                contentDescription = "预览图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
