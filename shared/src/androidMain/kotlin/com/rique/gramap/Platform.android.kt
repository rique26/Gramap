package com.rique.gramap

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun SvgAsset(assetName: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(context, assetName) {
        runCatching {
            val path = "composeResources/gramap.shared.generated.resources/drawable/$assetName.svg"
            val svg = context.assets.open(path).use { SVG.getFromInputStream(it) }
            val width = (svg.documentWidth * 3).toInt().coerceAtLeast(1)
            val height = (svg.documentHeight * 3).toInt().coerceAtLeast(1)
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { result ->
                svg.renderToCanvas(android.graphics.Canvas(result), RectF(0f, 0f, width.toFloat(), height.toFloat()))
            }
        }.getOrNull()
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds)
    }
}
