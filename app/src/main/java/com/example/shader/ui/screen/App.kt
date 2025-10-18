package com.example.shader.ui.screen

import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.shader.R
import com.example.shader.ui.component.APP_HORIZONTAL_DP
import com.example.shader.ui.component.CustomSlider
import com.example.shader.ui.style.layout.RowHorizontal
import com.xah.mirror.shader.blurLayer
import com.xah.mirror.shader.extraLargeStyle
import com.xah.mirror.shader.glassLayer
import com.xah.mirror.shader.largeStyle
import com.xah.mirror.shader.scaleMirror
import com.xah.mirror.shader.smallStyle
import com.xah.mirror.util.ShaderState
import com.xah.mirror.util.rememberShaderState
import com.xah.mirror.util.shaderSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private suspend fun getDrawOpenOffset(drawerState : DrawerState) : Float = withContext(Dispatchers.IO) {
    drawerState.close()
    val newValue = drawerState.currentOffset
    return@withContext newValue
}


@Composable
fun App() {
    var sliderBlur by remember { mutableFloatStateOf(largeStyle.blur.value) }
    var sliderBorder by remember { mutableFloatStateOf(largeStyle.border) }
    var sliderDistortFactor by remember { mutableFloatStateOf(largeStyle.distortFactor) }
    var sliderOverlayAlpha by remember { mutableFloatStateOf(0.4f) }
    var sliderDispersion by remember { mutableFloatStateOf(largeStyle.dispersion) }

    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { imageUri ->
            // 使用 context 读取图片为 ImageBitmap
            imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }
    }
    var showTool by remember { mutableStateOf(true) }

    var showBlur by remember { mutableStateOf(false) }
    val drawerState =  rememberDrawerState(DrawerValue.Closed)
    val configuration = LocalConfiguration.current
    var maxOffset by rememberSaveable { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.screenWidthDp }
            .collect {
                maxOffset = getDrawOpenOffset(drawerState)
            }
    }
    val shaderState = rememberShaderState()
    val blurDp by remember {
        derivedStateOf {
            if (maxOffset == 0f) {
                0.dp // 未校准前不模糊
            } else {
                val fraction = 1 - (drawerState.currentOffset / maxOffset).coerceIn(0f, 1f)
                (fraction * 7.5).dp//37.5
            }
        }
    }
    val scale by remember {
        derivedStateOf {
            if (maxOffset == 0f) {
                1f
            } else {
                val fraction =  (drawerState.currentOffset / maxOffset).coerceIn(0f, 1f)
                (0.9f) * (1 - fraction) + fraction
            }
        }
    }
    ModalNavigationDrawer  (
        scrimColor =
            if(showBlur) {
                MaterialTheme.colorScheme.onSurface.copy(.2f)
            } else MaterialTheme.colorScheme.onSurface.copy(.3f),
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                Text("控制中心", modifier = Modifier.align(Alignment.Center))
            }
        },
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .blur(if (showBlur) blurDp else 0.dp)
                .scaleMirror(scale, RoundedCornerShape(0.dp))
            ) {
                val squareBlur by animateDpAsState(
                    if(showBlur) sliderBlur.dp else 0.dp
                )
                val color2 by animateColorAsState(
                    if(showBlur) {
                        MaterialTheme.colorScheme.surface.copy(sliderOverlayAlpha)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(sliderOverlayAlpha)
                    }
                )
                Square(shaderState,color2,squareBlur,false,sliderBorder,sliderDispersion,sliderDistortFactor)

                Box(
                    modifier = Modifier
                        .shaderSource(shaderState)
                ){
                    if(imageBitmap == null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Spacer(Modifier.statusBarsPadding())
                            }
                            items(100) { index ->
                                ListItem(
                                    headlineContent = {
                                        Text("测试")
                                    },
                                    trailingContent = {
                                        Icon(
                                            painterResource(R.drawable.ic_launcher_foreground),
                                            null
                                        )
                                    }
                                )
                            }
                            item {
                                Spacer(Modifier.navigationBarsPadding())
                            }
                        }
                    } else {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(
                    Modifier
                        .zIndex(3f)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(APP_HORIZONTAL_DP)
                ) {
                    if(showTool) {
                        Spacer(Modifier.height(APP_HORIZONTAL_DP))
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            border =BorderStroke(1.dp,Color.White),
                            colors = CardDefaults. cardColors(Color.Transparent),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .shadow(
                                    25.dp,
                                    MaterialTheme.shapes.extraLarge
                                )
                                .glassLayer(
                                    shaderState,
                                    style = extraLargeStyle.copy(
                                        overlayColor = Color.White.copy(.85f)
                                    ),
                                )
                        ) {
                            Column(
                                Modifier.padding(vertical = APP_HORIZONTAL_DP)
                            ) {
                                Text("模糊 ${sliderBlur}Dp", modifier = Modifier.padding(horizontal = APP_HORIZONTAL_DP))
                                CustomSlider(
                                    value = sliderBlur,
                                    onValueChange = {
                                        sliderBlur = it
                                    },
                                    valueRange = 0f..50f
                                )
                                Text("蒙版 ${sliderOverlayAlpha * 100}%", modifier = Modifier.padding(horizontal = APP_HORIZONTAL_DP))
                                CustomSlider(
                                    value = sliderOverlayAlpha,
                                    onValueChange = {
                                        sliderOverlayAlpha = it
                                    },
                                    valueRange = 0f..1f
                                )
                                Text("镜宽 ${sliderBorder}", modifier = Modifier.padding(horizontal = APP_HORIZONTAL_DP))
                                CustomSlider(
                                    value = sliderBorder,
                                    onValueChange = {
                                        sliderBorder = it
                                    },
                                    valueRange = 0f..75f
                                )
                                Text("离心 ${sliderDistortFactor}", modifier = Modifier.padding(horizontal = APP_HORIZONTAL_DP))
                                CustomSlider(
                                    value = sliderDistortFactor,
                                    onValueChange = {
                                        sliderDistortFactor = it
                                    },
                                    valueRange = 0f..0.4f
                                )
                                Text("色散 ${sliderDispersion}", modifier = Modifier.padding(horizontal = APP_HORIZONTAL_DP))
                                CustomSlider(
                                    value = sliderDispersion,
                                    onValueChange = {
                                        sliderDispersion = it
                                    },
                                    valueRange = 0f..15f
                                )
                            }
                        }
                    }
                }
                Column(
                    Modifier
                        .zIndex(3f)
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(APP_HORIZONTAL_DP)
                ) {
                    RowHorizontal {
                        Row (
                            Modifier
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    Color.White.copy(.9f),
                                    CircleShape
                                )
                                .glassLayer(
                                    shaderState,
                                    style = smallStyle.copy(
                                        overlayColor = Color.White.copy(.8f)
                                    ),
                                )
                        ){
                            FilledTonalButton (
                                colors =  ButtonDefaults. filledTonalButtonColors(Color.Transparent, MaterialTheme.colorScheme.onSurface),
                                onClick = {
                                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            ) {
                                Text("选择图片")
                            }
                            FilledTonalButton(
                                colors =  ButtonDefaults. filledTonalButtonColors(Color.Transparent, MaterialTheme.colorScheme.onSurface),
                                onClick = {
                                    showBlur = !showBlur
                                },
                            ) {
                                Text("模糊")
                            }
                            FilledTonalButton(
                                colors =  ButtonDefaults. filledTonalButtonColors(Color.Transparent, MaterialTheme.colorScheme.onSurface),
                                onClick = {
                                    showTool = !showTool
                                },
                            ) {
                                Text("调整")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Square(
    shaderState: ShaderState,
    color: Color,
    squareBlur: Dp,
    isBlurSquare: Boolean = false,
    border: Float = largeStyle.border,
    dispersion: Float = largeStyle.dispersion,
    distortFactor: Float = largeStyle.distortFactor,
) {
    val shape = MaterialTheme.shapes.large
    val density = LocalDensity.current

    // 屏幕尺寸（像素）
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val squareSizePx = with(density) { 250.dp.toPx() }
    val topMarginPx = with(density) { 30.dp.toPx() } // ✅ 距离顶部 15.dp

    // 👉 初始位置：水平居中 + 距顶部15dp
    var offsetX by remember { mutableStateOf((screenWidthPx - squareSizePx) / 2f) }
    var offsetY by remember { mutableStateOf(topMarginPx) }
    var scale by remember { mutableFloatStateOf(1f) }

    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
        shape = shape,
        modifier = Modifier
            .size(250.dp * scale)
            .zIndex(2f)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .shadow(25.dp, shape = shape)
            .let {
                if (!isBlurSquare)
                    it.glassLayer(
                        shaderState,
                        style = largeStyle.copy(
                            blur = squareBlur,
                            border = border,
                            overlayColor = color,
                            dispersion = dispersion,
                            distortFactor = distortFactor
                        )
                    )
                else
                    it.blurLayer(shaderState)
            }
    ) {}
}
