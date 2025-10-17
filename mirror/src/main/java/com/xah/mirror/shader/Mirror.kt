package com.xah.mirror.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xah.mirror.util.ShaderState
import com.xah.mirror.util.recordPosition
import org.intellij.lang.annotations.Language


// 绘制内容
fun Modifier.scaleMirror(
    scale: Float = 1f,
    clipShape: Shape = RoundedCornerShape(0.dp),
): Modifier =
    if(Build.VERSION.SDK_INT < 33 || scale == 1f) {
        this
    } else {
        composed {
            // 绘制面
            var rect by remember { mutableStateOf<Rect?>(null) }

            this
                .graphicsLayer {
                    clip = true
                    shape = clipShape
                    rect?.let { r ->
                        val runtimeShader = RuntimeShader(MIRROR_SHADER_CODE.trimIndent())
                        runtimeShader.setFloatUniform("size", r.width, r.height)
                        runtimeShader.setFloatUniform("scale", scale)

                        renderEffect = RenderEffect
                            .createRuntimeShaderEffect(runtimeShader, "content")
                            .asComposeRenderEffect()
                    }
                }
                .recordPosition {
                    rect = it
                }
        }
    }

// 绘制内容
fun Modifier.mirrorLayer(
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
        .mirrorLayer(state, scale, blur)

fun Modifier.mirrorLayer(
    state: ShaderState,
    scale : Float,
    blur : Dp = 0.dp,
) : Modifier =
    if(Build.VERSION.SDK_INT < 33) {
        this
    } else {
        composed {
            var rect by remember { mutableStateOf<Rect?>(null) }

            this
                .blur(blur)
                .graphicsLayer {
                    rect?.let { r ->
                        val runtimeShader = RuntimeShader(MIRROR_SHADER_CODE.trimIndent())
                        runtimeShader.setFloatUniform("size", r.width, r.height)
                        runtimeShader.setFloatUniform("scale", scale)

                        val mirrorShader =
                            RenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                        renderEffect = mirrorShader.asComposeRenderEffect()
                    }
                    clip = true
                }
                .drawWithCache {
                    onDrawBehind {
                        val contentRect = state.rect ?: return@onDrawBehind
                        val surfaceRect = rect ?: return@onDrawBehind

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
                .recordPosition {
                    rect = it
                }
        }
    }




@Language("ASGL")
private const val MIRROR_SHADER_CODE = """
    uniform shader content;
    uniform float2 size;   // 原始画面宽高
    uniform float scale;   // 缩放比例，
        
    half4 main(float2 fragCoord) {
        float2 center = size * 0.5;
        float2 offset = fragCoord - center;
        
        // 缩放
        float2 scaled = offset / scale;
        float2 sampleCoord = center + scaled;
        
        // 镜面反射逻辑
        if(sampleCoord.x < 0.0) sampleCoord.x = -sampleCoord.x;
        if(sampleCoord.x > size.x) sampleCoord.x = 2.0*size.x - sampleCoord.x;
        
        if(sampleCoord.y < 0.0) sampleCoord.y = -sampleCoord.y;
        if(sampleCoord.y > size.y) sampleCoord.y = 2.0*size.y - sampleCoord.y;

        return content.eval(sampleCoord);
    }
"""

