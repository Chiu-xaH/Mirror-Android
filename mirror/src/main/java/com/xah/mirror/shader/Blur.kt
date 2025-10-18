package com.xah.mirror.shader

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xah.mirror.util.ShaderState
import com.xah.mirror.util.recordPosition

// 层级模糊

// 绘制内容
fun Modifier.blurLayer(
    state: ShaderState,
) : Modifier = composed {
    var rect by remember { mutableStateOf<Rect?>(null) }

    this
        .drawWithCache {
            onDrawBehind {
                // 绘制
                val contentRect = state.rect ?: return@onDrawBehind
                val surfaceRect = rect ?: return@onDrawBehind

                val offset = surfaceRect.topLeft - contentRect.topLeft
                withTransform({
                    translate(-offset.x, -offset.y)
                }) {
                    drawLayer(state.graphicsLayer)
                }
            }
        }
        .recordPosition {
            rect = it
        }
}

// 记录内容
fun Modifier.blurSource(
    state : ShaderState,
    blur : Dp,
    enhanceColor : Boolean = false,
) : Modifier =
    if(Build.VERSION.SDK_INT < 32)
        this
    else
        this
            .drawWithContent {
                drawContent()

                val blurEffect = RenderEffect.createBlurEffect(blur.toPx(), blur.toPx(), Shader.TileMode.CLAMP)
                val enhanceEffect = enhanceColorShader(enhanceColor)
                val chained = RenderEffect.createChainEffect(enhanceEffect, blurEffect)

                state.graphicsLayer.renderEffect = chained.asComposeRenderEffect()
                // 模糊后的画面录制下拉
                state.graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
            }
            .onGloballyPositioned { layoutCoordinates ->
                state.rect = layoutCoordinates.boundsInRoot()
            }

