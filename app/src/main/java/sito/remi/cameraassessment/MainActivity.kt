package sito.remi.cameraassessment

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import sito.remi.cameraassessment.ui.theme.CameraAssessmentTheme
import java.io.File
import java.text.SimpleDateFormat

import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraAssessmentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasCameraPermission()) {
                        CameraApp()
                    } else {
                        requestCameraPermission()
                    }
                }
            }
        }
    }
    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        isFrontCamera: Boolean,
        lifecycleOwner: LifecycleOwner,
        onCameraSwitch: () -> Unit,
        imageCapture: MutableState<ImageCapture?>
    ) {
        val context = LocalContext.current
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context)

        AndroidView(modifier = modifier, factory = { context ->
            PreviewView(context).apply {
                val executor: Executor = ContextCompat.getMainExecutor(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val imageCaptureUseCase = ImageCapture.Builder().build()

                    val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                    preview.setSurfaceProvider(surfaceProvider)

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCaptureUseCase)
                        imageCapture.value = imageCaptureUseCase
                    } catch (exception: Exception) {
                        Log.e("error","Unbinding failed.")
                        Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }, executor)

                // Toggle camera when tapped
                setOnClickListener {
                    onCameraSwitch()
                }
            }
        })
    }


    @Composable
    fun CameraApp() {
        val ctx = LocalContext.current
        var currentTime by remember { mutableStateOf(getCurrentTime()) }
        var activeCamera by remember { mutableIntStateOf(1) } // 1 for front camera, 2 for back camera
        var cameraName by remember { mutableStateOf("cam") }

        // Update the clock display every second
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                currentTime = getCurrentTime()
            }
        }

        val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }

        Surface(color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Moleculight Dual Camera Control", color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp)) // Add space after the header
                Row {
                    Box(
                        modifier = Modifier
                            .size(200.dp, 300.dp) // Set fixed width and height
                            .clickable { activeCamera = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        // Start the front camera preview if activeCamera is 1
                        if (activeCamera == 1) {
                            cameraName = "frontCamera"
                            CameraPreview(
                                isFrontCamera = true,
                                lifecycleOwner = LocalLifecycleOwner.current,
                                onCameraSwitch = { activeCamera = 2 },
                                imageCapture = imageCapture
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(200.dp, 300.dp)
                            .clickable { activeCamera = 2 },
                        contentAlignment = Alignment.Center
                    ) {

                        // Start the back camera preview if activeCamera is 2
                        if (activeCamera == 2) {
                            cameraName="backCamera"
                            CameraPreview(
                                isFrontCamera = false,
                                lifecycleOwner = LocalLifecycleOwner.current,
                                onCameraSwitch = { activeCamera = 1 },
                                imageCapture = imageCapture
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { captureImage(imageCapture.value,ctx,cameraName) }) {
                    Text(text = "Save Image on $cameraName", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Clock Display: $currentTime", color = Color.Black)
            }
        }
    }

    private fun captureImage(imageCapture: ImageCapture?, context: Context, cameraName: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Storage permission is required to save images.", Toast.LENGTH_SHORT).show()
        } else {
            imageCapture?.let { capture ->
                val filename = "${cameraName}_${getCurrentTime()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                }

                val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                // I Set up image capture listener, which is triggered after photo has been taken
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val msg = "Photo capture succeeded: $imageUri"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            val msg = "Photo capture failed: ${exception.message}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }


    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }


    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setContent {
                    CameraAssessmentTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                            content = { CameraApp() }

                        )

                    }
                }
            } else {
                setContent {
                    CameraAssessmentTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Camera",
                                    modifier = Modifier.padding(16.dp)
                                )

                                Text("Permission Denied for Camera")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }



}

