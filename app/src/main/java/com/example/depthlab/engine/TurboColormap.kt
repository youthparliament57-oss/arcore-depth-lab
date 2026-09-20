package com.example.depthlab.engine

import androidx.compose.ui.graphics.Color
import com.example.depthlab.data.model.ColormapType
import kotlin.math.sin

object ColorMaps {

    /**
     * Exact polynomial evaluation of Google's Turbo Colormap from TurboColormap.glslinc
     * Reference: https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html
     */
    fun turbo(normalizedDepth: Float): Color {
        val x = (1.0f - normalizedDepth.coerceIn(0f, 1f))
        val x2 = x * x
        val x3 = x2 * x
        val x4 = x3 * x
        val x5 = x4 * x

        val r = (0.13572138f + 4.61539260f * x - 42.66032258f * x2 + 132.13108234f * x3 - 152.94239396f * x4 + 59.28637943f * x5).coerceIn(0f, 1f)
        val g = (0.09140261f + 2.19418839f * x + 4.84296658f * x2 - 14.18503333f * x3 + 4.27729857f * x4 + 2.82956604f * x5).coerceIn(0f, 1f)
        val b = (0.10667330f + 12.64194608f * x - 60.58204836f * x2 + 110.36276771f * x3 - 89.90310912f * x4 + 27.34824973f * x5).coerceIn(0f, 1f)

        return Color(r, g, b)
    }

    fun jet(normalizedDepth: Float): Color {
        val d = normalizedDepth.coerceIn(0f, 1f)
        val r = (1.5f - kotlin.math.abs(4.0f * d - 3.0f)).coerceIn(0f, 1f)
        val g = (1.5f - kotlin.math.abs(4.0f * d - 2.0f)).coerceIn(0f, 1f)
        val b = (1.5f - kotlin.math.abs(4.0f * d - 1.0f)).coerceIn(0f, 1f)
        return Color(r, g, b)
    }

    fun viridis(normalizedDepth: Float): Color {
        val t = (1.0f - normalizedDepth.coerceIn(0f, 1f))
        val r = (0.267f + 0.733f * t * t).coerceIn(0f, 1f)
        val g = (0.004f + 0.880f * t).coerceIn(0f, 1f)
        val b = (0.329f + 0.400f * (1f - t) + 0.271f * sin(t * 3.1415f)).coerceIn(0f, 1f)
        return Color(r, g, b)
    }

    fun grayscale(normalizedDepth: Float): Color {
        val v = (1.0f - normalizedDepth.coerceIn(0f, 1f))
        return Color(v, v, v)
    }

    fun mapDepthToColor(normalizedDepth: Float, type: ColormapType): Color {
        return when (type) {
            ColormapType.TURBO -> turbo(normalizedDepth)
            ColormapType.JET -> jet(normalizedDepth)
            ColormapType.VIRIDIS -> viridis(normalizedDepth)
            ColormapType.GRAYSCALE -> grayscale(normalizedDepth)
        }
    }
}
