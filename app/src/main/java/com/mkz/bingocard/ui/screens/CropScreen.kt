package com.mkz.bingocard.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private enum class DragTarget { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    imageUri: Uri?,
    onConfirm: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onRecapture: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUri) { mutableStateOf(imageUri != null) }

    LaunchedEffect(imageUri) {
        bitmap = null
        isLoading = imageUri != null
        bitmap = imageUri?.let { uri ->
            withContext(Dispatchers.IO) {
                loadCropPreviewBitmap(context, uri)
            }
        }
        isLoading = false
    }

    // Crop rectangle in normalized coordinates (0.0 - 1.0)
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Bingo Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Drag corners to crop around the bingo card",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading image...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (bitmap != null) {
                val loadedBitmap = bitmap ?: return@Column
                val imgBitmap = remember(loadedBitmap) { loadedBitmap.asImageBitmap() }
                val aspectRatio = loadedBitmap.width.toFloat() / loadedBitmap.height.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                    ) {
                        var dragTarget by remember { mutableStateOf(DragTarget.NONE) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val handleRadius = 60f / w // normalized

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val nx = offset.x / w
                                            val ny = offset.y / h

                                            fun dist(ax: Float, ay: Float, bx: Float, by: Float) =
                                                sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by))

                                            dragTarget = when {
                                                dist(nx, ny, cropLeft, cropTop) < handleRadius -> DragTarget.TOP_LEFT
                                                dist(nx, ny, cropRight, cropTop) < handleRadius -> DragTarget.TOP_RIGHT
                                                dist(nx, ny, cropRight, cropBottom) < handleRadius -> DragTarget.BOTTOM_RIGHT
                                                dist(nx, ny, cropLeft, cropBottom) < handleRadius -> DragTarget.BOTTOM_LEFT
                                                nx in cropLeft..cropRight && ny in cropTop..cropBottom -> DragTarget.MOVE
                                                else -> DragTarget.NONE
                                            }
                                        },
                                        onDrag = { _, dragAmount ->
                                            val dx = dragAmount.x / w
                                            val dy = dragAmount.y / h
                                            val minSize = 0.15f

                                            when (dragTarget) {
                                                DragTarget.TOP_LEFT -> {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                                }
                                                DragTarget.TOP_RIGHT -> {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                                }
                                                DragTarget.BOTTOM_RIGHT -> {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                                }
                                                DragTarget.BOTTOM_LEFT -> {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                                }
                                                DragTarget.MOVE -> {
                                                    val cw = cropRight - cropLeft
                                                    val ch = cropBottom - cropTop
                                                    val newLeft = (cropLeft + dx).coerceIn(0f, 1f - cw)
                                                    val newTop = (cropTop + dy).coerceIn(0f, 1f - ch)
                                                    cropLeft = newLeft
                                                    cropTop = newTop
                                                    cropRight = newLeft + cw
                                                    cropBottom = newTop + ch
                                                }
                                                DragTarget.NONE -> {}
                                            }
                                        },
                                        onDragEnd = { dragTarget = DragTarget.NONE }
                                    )
                                }
                        ) {
                            val canvasW = size.width
                            val canvasH = size.height

                            // 1. Draw image
                            drawImage(
                                image = imgBitmap,
                                dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
                            )

                            // 2. Dark overlay outside crop (4 rectangles)
                            val overlay = Color.Black.copy(alpha = 0.55f)
                            val cl = cropLeft * canvasW
                            val ct = cropTop * canvasH
                            val cr = cropRight * canvasW
                            val cb = cropBottom * canvasH

                            // Top
                            drawRect(overlay, Offset.Zero, Size(canvasW, ct))
                            // Bottom
                            drawRect(overlay, Offset(0f, cb), Size(canvasW, canvasH - cb))
                            // Left
                            drawRect(overlay, Offset(0f, ct), Size(cl, cb - ct))
                            // Right
                            drawRect(overlay, Offset(cr, ct), Size(canvasW - cr, cb - ct))

                            // 3. Crop border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(cl, ct),
                                size = Size(cr - cl, cb - ct),
                                style = Stroke(width = 2.5f)
                            )

                            // 4. 5×5 grid lines inside crop (helps align bingo card)
                            val gridColor = Color.White.copy(alpha = 0.3f)
                            for (i in 1..4) {
                                val fx = cl + (cr - cl) * i / 5f
                                drawLine(gridColor, Offset(fx, ct), Offset(fx, cb), strokeWidth = 1f)
                                val fy = ct + (cb - ct) * i / 5f
                                drawLine(gridColor, Offset(cl, fy), Offset(cr, fy), strokeWidth = 1f)
                            }

                            // 5. Corner handles
                            val corners = listOf(
                                Offset(cl, ct), Offset(cr, ct),
                                Offset(cr, cb), Offset(cl, cb)
                            )
                            for (corner in corners) {
                                drawCircle(Color.White, radius = 16f, center = corner)
                                drawCircle(Color(0xFF1E88E5), radius = 12f, center = corner)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image loaded", color = MaterialTheme.colorScheme.error)
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        cropLeft = 0.05f; cropTop = 0.05f
                        cropRight = 0.95f; cropBottom = 0.95f
                    }
                ) {
                    Text("Reset Crop")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRecapture
                ) {
                    Text("Re-capture")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(cropLeft, cropTop, cropRight, cropBottom) },
                    enabled = bitmap != null
                ) {
                    Text("Analyze")
                }
            }
        }
    }
}

private fun loadCropPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }

        val maxDim = maxOf(opts.outWidth, opts.outHeight)
        val sampleSize = maxOf(1, maxDim / 2048)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}
