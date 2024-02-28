package sito.remi.cameraassessment

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Point
import android.net.Uri
import android.os.Build
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
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Canvas
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
import sito.remi.cameraassessment.ui.theme.CameraAssessmentTheme
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
                    CameraApp()

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

        if (hasPermissions()) {
        }else{

            requestPermissions()

        }

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
                Text(
                    text = "Moleculight Camera Control",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.animateContentSize()) {
                    Box(
                        modifier = Modifier
                            .size(200.dp, 300.dp)
                            .clickable { activeCamera = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeCamera == 1) {

                            CameraPreview(
                                isFrontCamera = true,
                                lifecycleOwner = LocalLifecycleOwner.current,
                                onCameraSwitch = { activeCamera = 2 },
                                imageCapture = imageCapture

                            )
                        } else {
                            Text(
                                text = "Tap to switch!",
                                color = Color.Black,
                                modifier = Modifier.alpha(0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Box(
                        modifier = Modifier
                            .size(200.dp, 300.dp)
                            .clickable { activeCamera = 2 },
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeCamera == 2) {
                            cameraName = "Back Camera"
                            CameraPreview(
                                isFrontCamera = false,
                                lifecycleOwner = LocalLifecycleOwner.current,
                                onCameraSwitch = { activeCamera = 1 },
                                imageCapture = imageCapture

                            )
                        } else {
                            Text(
                                text = "Tap to switch!",
                                color = Color.Black,
                                modifier = Modifier.alpha(0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { saveImageToGallery(imageCapture.value, ctx, cameraName) }) {
                    Text(text = "Save Image on $cameraName", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                DigitalClock()
            }
        }
    }


    @SuppressLint("NewApi")
    @Composable
    fun DigitalClock() {
        val currentDateTime = remember { mutableStateOf(LocalDateTime.now()) }

        LaunchedEffect(key1 = currentDateTime) {
            while (true) {
                currentDateTime.value = LocalDateTime.now()
                delay(1000)
            }
        }

        Text(
            text = "Time: ${currentDateTime.value.format(DateTimeFormatter.ofPattern("HH:mm:ss a"))}",
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium
        )
    }


    private fun saveImageToGallery(imageCapture: ImageCapture?, context: Context, cameraName: String) {
        if (imageCapture == null) {
            Log.e("CameraApp", "ImageCapture is null, cannot save image")
            Toast.makeText(context, "ImageCapture is null, cannot save image", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageName = "$cameraName-$timeStamp.jpg"

        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "CameraAssessment")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val outputDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraAssessment")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val file = File(outputDirectory, imageName)

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    val errorMessage = "Failed to save image: ${exception.message}"
                    Log.e("CameraApp", errorMessage, exception)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val successMessage = "Image saved successfully: ${outputFileResults.savedUri}"
                    Log.i("CameraApp", successMessage)
                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(imageUri!!, contentValues, null, null)
                    }

                    // Now, let us Notify the system about the new file
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                }
            }
        )
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }


     private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Camera permission is not granted, request for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Storage permission is not granted, request for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE || requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            var cameraPermissionGranted = false
            var storagePermissionGranted = false

            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.CAMERA && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionGranted = true
                }
                if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    storagePermissionGranted = true
                }
            }

            if (cameraPermissionGranted && storagePermissionGranted) {
                // Permissions granted, show the CameraApp
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
                //Permissions denied, show a message or handle it appropriately
                Toast.makeText(this, "Permissions not granted yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val STORAGE_PERMISSION_REQUEST_CODE = 101
    }



}



