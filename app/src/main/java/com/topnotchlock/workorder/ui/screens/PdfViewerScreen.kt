@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * A built-in PDF reader so a work order can be viewed without depending on
 * whatever (if any) PDF app happens to be installed on the device. Renders
 * page 1 (every generated work order is exactly one page) with pinch-to-zoom,
 * plus Share and Save-to-Downloads actions right in the same screen.
 */
@Composable
fun PdfViewerScreen(viewModel: MainViewModel, filePath: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        bitmap = null
        loadError = null
        if (!file.exists()) {
            loadError = "This work order's PDF isn't on this device anymore (it may have been cleared, " +
                "e.g. by reinstalling the app). Go back and tap Generate PDF again to recreate it."
            return@LaunchedEffect
        }
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) error("The PDF has no pages")
                    val page = renderer.openPage(0)
                    // 2x render scale for a crisp zoomed-in view on high-density screens.
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                }
            }
        }.onSuccess { bitmap = it }
            .onFailure { e -> loadError = "Couldn't open this PDF (${e.javaClass.simpleName}): ${e.message ?: "unknown error"}" }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.saveToDownloads(file, file.nameWithoutExtension) { _, message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Storage permission is needed to save to Downloads on this version of Android.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Work Order PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!file.exists()) {
                            scope.launch { snackbarHostState.showSnackbar("This PDF no longer exists on this device.") }
                        } else {
                            sharePdf(context, viewModel.shareUriFor(file))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        when {
                            !file.exists() ->
                                scope.launch { snackbarHostState.showSnackbar("This PDF no longer exists on this device.") }
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ->
                                permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            else ->
                                viewModel.saveToDownloads(file, file.nameWithoutExtension) { _, message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                        }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Save to Downloads")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                loadError != null -> Text(
                    loadError.orEmpty(),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
                bitmap == null -> CircularProgressIndicator()
                else -> Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Work order PDF preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        }
                )
            }
        }
    }
}
