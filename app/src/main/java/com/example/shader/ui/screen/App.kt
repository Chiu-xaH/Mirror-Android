package com.example.shader.ui.screen

import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
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
import com.xah.mirror.shader.blurLayer
import com.xah.mirror.shader.glassLayer
import com.xah.mirror.shader.scaleMirror
import com.xah.mirror.util.ShaderState
import com.xah.mirror.util.rememberShaderState
import com.xah.mirror.util.shaderSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun getDrawOpenOffset(drawerState : DrawerState) : Float = withContext(Dispatchers.IO) {
    drawerState.close()
    val newValue = drawerState.currentOffset
    return@withContext newValue
}


@Composable
fun App() {
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
        modifier = Modifier.statusBarsPadding().navigationBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .blur(if(showBlur)blurDp else 0.dp)
                .scaleMirror(scale,RoundedCornerShape(0.dp))
            ) {
                val squareBlur by animateDpAsState(
                    if(showBlur) 10.dp else 0.dp
                )
                val color2 by animateColorAsState(
                    if(showBlur) {
                        MaterialTheme.colorScheme.surface.copy(.15f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(.15f)
                    }
                )
                Square(shaderState,color2,squareBlur)

                Box(
                    modifier = Modifier.shaderSource(shaderState)
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

                    Row (
                        Modifier.zIndex(3f).align(Alignment.TopCenter).statusBarsPadding().padding(15.dp).background(
                            MaterialTheme.colorScheme.secondaryContainer,CircleShape)
                    ){
                        FilledTonalButton (
                            onClick = {
                                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Text("选择图片")
                        }
                        FilledTonalButton(
                            onClick = {
                                showBlur = !showBlur
                            },
                        ) {
                            Text("模糊")
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
) {
    val shape = MaterialTheme.shapes.large

    var offsetX by remember { mutableStateOf(15.dp) }
    var offsetY by remember { mutableStateOf(15.dp) }
    var scale by remember { mutableFloatStateOf(1f) } // 缩放比例

    // 屏幕密度
    val density = LocalDensity.current

    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
        shape = shape,
        modifier = Modifier
            .size(250.dp * scale) // 缩放
            .zIndex(2f)
            .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 缩放
                    scale = (scale * zoom).coerceIn(0.5f, 3f) // 最小0.5倍，最大3倍

                    // 平移
                    offsetX += with(density) { pan.x.toDp() }
                    offsetY += with(density) { pan.y.toDp() }
                }
            }
            .shadow(25.dp, shape = shape)
            .let {
                if (!isBlurSquare)
                    it.glassLayer(
                        shaderState,
                        dispersion = 0f,
                        clipShape = shape,
                        tint = color,
                        blur = squareBlur
                    )
                else
                    it.blurLayer(
                        shaderState,
                        clipShape = shape
                    )
            }
    ) {}
}
