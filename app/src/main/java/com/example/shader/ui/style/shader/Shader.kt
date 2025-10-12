package com.example.shader.ui.style.shader

import android.graphics.RenderEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.example.shader.ui.util.ShaderState

fun Modifier.shaderLayer(
    state: ShaderState,
    shader: RenderEffect?
) : Modifier =
    this
        .graphicsLayer {
            renderEffect = shader?.asComposeRenderEffect()
            clip = true
        }
        .drawWithCache {
            onDrawBehind {
                val contentRect = state.rect ?: return@onDrawBehind
                val surfaceRect = state.sRect ?: return@onDrawBehind

                val offset = surfaceRect.topLeft - contentRect.topLeft
                // 绘制原画面
                withTransform({
                    translate(-offset.x, -offset.y)
                }) {
                    drawLayer(state.graphicsLayer)
                }
            }
        }
        // 记录位置
        .onGloballyPositioned { layoutCoordinates ->
            val pos = layoutCoordinates.positionInWindow()
            val size = layoutCoordinates.size
            state.sRect = Rect(
                pos.x,
                pos.y,
                pos.x + size.width,
                pos.y + size.height
            )
        }