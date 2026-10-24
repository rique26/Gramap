package com.rique.gramap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun SvgAsset(assetName: String, modifier: Modifier = Modifier)
