package com.example.scriptbreak.ui.screens

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.scriptbreak.logic.QrDecoder
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live QR scanner. The camera runs only while this screen is open; each frame is read in memory on the
 * phone (ZXing, no AI model) and nothing is saved or sent. [onScanned] returns true when the code was used
 * (the screen then closes); false keeps scanning, e.g. for a QR that is not a UPI payment code.
 */
@Composable
fun QrScannerScreen(
    title: String,
    onScanned: (String) -> Boolean,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted; asked = true
    }
    LaunchedEffect(Unit) { if (!hasCamera) askCamera.launch(Manifest.permission.CAMERA) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        AppTopBar(title = title, onBack = onBack, showScriptBreakBadge = true)
        Spacer(Modifier.height(12.dp))
        if (hasCamera) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(16.dp))
            ) {
                CameraQrPreview(onScanned)
                // where to hold the code
                Box(Modifier.size(240.dp).border(3.dp, Color.White, RoundedCornerShape(18.dp)))
            }
            Spacer(Modifier.height(12.dp))
            Text("Point the camera at a UPI QR code. It is read on this phone; nothing is saved.",
                fontSize = 15.sp, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Text("Camera permission is needed to scan a QR code", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = InkDark, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("The camera is used only on this screen, and nothing is saved. You can also pick a QR image.",
                    fontSize = 15.sp, color = MutedText, textAlign = TextAlign.Center)
                if (asked) {
                    Spacer(Modifier.height(16.dp))
                    DemoPrimaryButton("Allow camera", { askCamera.launch(Manifest.permission.CAMERA) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DemoSecondaryButton("Pick a QR image instead", onPickImage, Modifier.navigationBarsPadding())
    }
}

/** Camera preview + frame reader. Stops the camera when the screen closes. */
@Composable
private fun CameraQrPreview(onScanned: (String) -> Boolean) {
    val context = LocalContext.current
    val owner = remember(context) { context.lifecycleOwner() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        val main = Handler(Looper.getMainLooper())
        val busy = AtomicBoolean(false)   // one code at a time; a rejected code is retried after 2 s
        val disposed = AtomicBoolean(false)
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed.get() || owner == null) return@addListener
            val p = try { future.get() } catch (e: Exception) { null } ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { image ->
                val text = if (busy.get()) null else readQr(image)
                image.close()
                if (text != null && busy.compareAndSet(false, true)) {
                    main.post {
                        if (disposed.get()) return@post
                        if (!onScanned(text)) main.postDelayed({ busy.set(false) }, 2_000)
                    }
                }
            }
            try {
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                // no usable back camera: the "Pick a QR image" button still works
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed.set(true)
            provider?.unbindAll()
            executor.shutdown()
        }
    }
}

/** The camera frame's brightness plane (Y), copied row by row, then decoded. */
private fun readQr(image: ImageProxy): String? = try {
    val plane = image.planes[0]
    val w = image.width; val h = image.height
    val buffer = plane.buffer
    val data = ByteArray(w * h)
    for (row in 0 until h) {
        buffer.position(row * plane.rowStride)
        buffer.get(data, row * w, w)
    }
    QrDecoder.decodeLuminance(data, w, h)
} catch (e: Exception) {
    null
}

private tailrec fun Context.lifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.lifecycleOwner()
    else -> null
}
