package com.example.shader.ui.style.shader

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shader.ui.util.ShaderState
import java.util.UUID

// 层级模糊

// 绘制内容
fun Modifier.blurLayer(
    state: ShaderState,
    clipShape: Shape,
    id: Any = UUID.randomUUID()
) : Modifier =
    this
        .clip(clipShape)
        .drawWithCache {
            onDrawBehind {
                val contentRect = state.rect ?: return@onDrawBehind
                val surfaceRect = state.componentRects[id] ?: return@onDrawBehind

                val offset = surfaceRect.topLeft - contentRect.topLeft
                withTransform({
                    translate(-offset.x, -offset.y)
                    clipRect(0f, 0f, surfaceRect.width, surfaceRect.height)
                }) {
                    drawLayer(state.graphicsLayer)
                }
            }
        }
        .onGloballyPositioned { layoutCoordinates ->
            val pos = layoutCoordinates.positionInWindow()
            val size = layoutCoordinates.size
            state.componentRects[id] = Rect(
                pos.x,
                pos.y,
                pos.x + size.width,
                pos.y + size.height
            )
        }

// 记录内容
fun Modifier.blurSource(
    state : ShaderState,
    blur : Dp,
) : Modifier =
    this
        .drawWithContent {
            drawContent()

            state.graphicsLayer.record {
                this@drawWithContent.drawContent()
            }

            val blurEffect = RenderEffect.createBlurEffect(blur.toPx(), blur.toPx(), Shader.TileMode.CLAMP)
            val enhanceEffect = enhanceColorShader(blur != 0.dp)
            val chained = RenderEffect.createChainEffect(enhanceEffect, blurEffect)

            state.graphicsLayer.renderEffect = chained.asComposeRenderEffect()
        }
        .onGloballyPositioned { layoutCoordinates ->
            state.rect = layoutCoordinates.boundsInRoot()
        }

