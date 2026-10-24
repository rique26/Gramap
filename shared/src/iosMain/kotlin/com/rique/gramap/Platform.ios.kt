package com.rique.gramap

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import platform.UIKit.UIDevice
import gramap.shared.generated.resources.Res
import gramap.shared.generated.resources.ilha_a
import gramap.shared.generated.resources.nuvem

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun SvgAsset(assetName: String, modifier: Modifier) {
    val painter = when (assetName) {
        "ilha_a" -> painterResource(Res.drawable.ilha_a)
        else -> painterResource(Res.drawable.nuvem)
    }
    Image(painter = painter, contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds)
}
