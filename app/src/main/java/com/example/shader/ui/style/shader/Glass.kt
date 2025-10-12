package com.example.shader.ui.style.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shader.ui.util.ShaderState
import org.intellij.lang.annotations.Language
import java.util.UUID


// 绘制内容
fun Modifier.glassLayer(
    state: ShaderState,
    scale : Float,
    clipShape: Shape,
    tint : Color,
    blur : Dp = 0.dp,
) : Modifier =
    this
        .clip(clipShape)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(tint)
            }
        }
        .glassLayer(state,scale,blur)

fun Modifier.glassLayer(
    state: ShaderState,
    scale : Float,
    blur : Dp = 0.dp,
    id: Any = UUID.randomUUID()
) : Modifier =
    this
        .blur(blur)
        .graphicsLayer {
            state.componentRects[id]?.let { r ->
                val runtimeShader = RuntimeShader(GLASS_SHADER_CODE .trimIndent())
                runtimeShader.setFloatUniform("size", r.width, r.height)
                runtimeShader.setFloatUniform("border", 75f)
                runtimeShader.setFloatUniform("strength", scale)

                val enhanceEffect = enhanceColorShader(blur != 0.dp)
                val mirrorShader = RenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                val chained = RenderEffect.createChainEffect(enhanceEffect, mirrorShader)

                renderEffect = chained.asComposeRenderEffect()
            }
            clip = true
        }
        .drawWithCache {
            onDrawBehind {
                val contentRect = state.rect ?: return@onDrawBehind
                val surfaceRect = state.componentRects[id] ?: return@onDrawBehind

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
            state.componentRects[id] = Rect(
                pos.x,
                pos.y,
                pos.x + size.width,
                pos.y + size.height
            )
        }



@Language("ASGL")
private const val GLASS_SHADER_CODE = """
uniform shader content;
uniform float2 size;
uniform float border;   // 折射边缘宽度 (建议 20~80)
uniform float strength; // 折射强度 (建议 0.1~0.3)

half4 main(float2 fragCoord) {
    float2 uv = fragCoord;

    // 判断在边缘的程度
    float2 edgeDist = min(fragCoord, size - fragCoord);
    float edgeAmount = min(edgeDist.x, edgeDist.y);
    
    // 如果在边缘范围内，进行折射偏移
    if (edgeAmount < border) {
        float edgeFactor = 1.0 - edgeAmount / border;  // 边缘内归一化 0~1
        float2 dir = normalize(fragCoord - size * 0.5); // 朝外方向
        // 向外偏移（折射）
        uv += dir * border * strength * edgeFactor;
    }

    // 镜面反射边界处理，防止采样越界
    if (uv.x < 0.0) uv.x = -uv.x;
    if (uv.x > size.x) uv.x = 2.0 * size.x - uv.x;
    if (uv.y < 0.0) uv.y = -uv.y;
    if (uv.y > size.y) uv.y = 2.0 * size.y - uv.y;

    return content.eval(uv);
}
"""
