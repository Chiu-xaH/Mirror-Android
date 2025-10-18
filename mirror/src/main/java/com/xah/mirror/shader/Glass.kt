package com.xah.mirror.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xah.mirror.util.ShaderState
import com.xah.mirror.style.mask
import com.xah.mirror.util.recordPosition
import org.intellij.lang.annotations.Language


data class GlassStyle(
    val blur : Dp = 7.5.dp,
    val border : Float = 20f,
    val dispersion : Float = 0f,
    val distortFactor : Float = 0.05f,
)

// 绘制内容
fun Modifier.glassLayer(
    state: ShaderState,
    tint : Color,
    style : GlassStyle
) : Modifier =
    this
        .mask(color = tint)
        .glassLayer(state,style)

fun Modifier.glassLayer(
    state: ShaderState,
    style: GlassStyle
) : Modifier =
    if(Build.VERSION.SDK_INT < 33)
        this
    else {
        composed {
            var rect by remember { mutableStateOf<Rect?>(null) }
            val localLayer = rememberGraphicsLayer()
            val density = LocalDensity.current
            val customRenderEffect = remember(rect, style) {
                if (rect == null) return@remember null
                val r = rect!!
                val runtimeShader = RuntimeShader(GLASS_SHADER_CODE.trimIndent()).apply {
                    setFloatUniform("size", r.width, r.height)
                    setFloatUniform("border", style.border)
                    setFloatUniform("dispersion", style.dispersion)
                    setFloatUniform("distortFactor", style.distortFactor)
                }

                val enhanceEffect = enhanceColorShader(style.blur != 0.dp)

                val blurDp = with(density) { style.blur.toPx() }
                val blurEffect =  RenderEffect.createBlurEffect(blurDp, blurDp, Shader.TileMode.CLAMP)

                val mirrorShader = RenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                val chained = RenderEffect.createChainEffect(enhanceEffect, mirrorShader)
                RenderEffect.createChainEffect(blurEffect, chained).asComposeRenderEffect()
            }

            this
                .drawWithCache {
                    onDrawWithContent {
                        localLayer.apply {
                            val contentRect = state.rect ?: return@apply
                            val surfaceRect = rect ?: return@apply

                            val offset = surfaceRect.topLeft - contentRect.topLeft

                            record {
                                withTransform({
                                    translate(-offset.x, -offset.y)
                                }) {
                                    drawLayer(state.graphicsLayer)
                                }
                            }
                        }
                        localLayer.renderEffect = customRenderEffect
                        rect?.let {
                            withTransform({
                                clipRect(0f, 0f, it.width, it.height)
                            }) {
                                drawLayer(localLayer)
                            }
                        }
                        drawContent()
                    }
                }
                // 记录位置
                .recordPosition {
                    rect = it
                }
        }
    }



@Language("ASGL")
private const val GLASS_SHADER_CODE = """
uniform shader content;
uniform float2 size;
uniform float border;   // 折射边缘宽度 (建议 20~80)
uniform float dispersion; // 色散强度，建议 1~5 像素
uniform float distortFactor; // 离心系数，越大扭曲越明显 (0.0~1.0)

half4 main(float2 fragCoord) {
    float2 innerMin = float2(border, border);
    float2 innerMax = size - innerMin;

    // 主体区域：完全不变
    if (fragCoord.x >= innerMin.x && fragCoord.x <= innerMax.x &&
        fragCoord.y >= innerMin.y && fragCoord.y <= innerMax.y) {
        return content.eval(fragCoord);
    }

    // 最近的内区点（在 innerRect 边上）
    float2 nearest = clamp(fragCoord, innerMin, innerMax);

    // 到内区边缘的距离（0..border）
    float dist = distance(fragCoord, nearest);
    float edgeFactor = clamp(dist / border, 0.0, 1.0);

    // --- 镜面对称采样点 ---
    float2 mirrored = 2.0 * nearest - fragCoord;

    // 中心点
    float2 center = size * 0.5;

    // 离心扭曲向量：越靠外，向四角拉伸
    float2 radial = (center - fragCoord) * distortFactor * edgeFactor; // 反向

    // 镜面采样加上离心扭曲
    float2 distorted = mirrored + radial;

    // 方向向量：从内区边缘指向当前像素，用于色散
    float2 dir = normalize(fragCoord - nearest);
    if (dir.x == 0.0 && dir.y == 0.0) dir = float2(0.0, 0.0);

    // 色散偏移
    float2 redOffset   = distorted + dir * dispersion * 0.5;
    float2 greenOffset = distorted;
    float2 blueOffset  = distorted - dir * dispersion * 0.5;

    // 保证采样点在内区
    redOffset   = clamp(redOffset, innerMin, innerMax);
    greenOffset = clamp(greenOffset, innerMin, innerMax);
    blueOffset  = clamp(blueOffset, innerMin, innerMax);

    // 分通道采样
    half r = content.eval(redOffset).r;
    half g = content.eval(greenOffset).g;
    half b = content.eval(blueOffset).b;
    half a = content.eval(distorted).a; // alpha 保持原样

    return half4(r, g, b, a);
}
"""
