package com.perso.antivol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Note importante : depuis Android 12, un indicateur visuel (point vert) apparaît
 * à l'écran chaque fois que la caméra est utilisée. C'est une protection système
 * qu'aucune app tierce ne peut désactiver sans root. Pour un usage anti-vol,
 * c'est acceptable — l'objectif est d'identifier qui détient le téléphone, pas
 * une capture totalement invisible.
 */
object CameraHelper {

    suspend fun takePhoto(context: Context, useFrontCamera: Boolean = true): File? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val facing = cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            if (useFrontCamera) facing == CameraCharacteristics.LENS_FACING_FRONT
            else facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.firstOrNull() ?: return null

        val handlerThread = HandlerThread("CameraThread").apply { start() }
        val handler = Handler(handlerThread.looper)

        return try {
            suspendCancellableCoroutine { cont ->
                val imageReader = ImageReader.newInstance(1280, 960, ImageFormat.JPEG, 1)

                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    val outFile = File(context.filesDir, "capture_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outFile).use { it.write(bytes) }

                    if (cont.isActive) cont.resume(outFile)
                    handlerThread.quitSafely()
                }, handler)

                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureRequestBuilder.addTarget(imageReader.surface)

                        camera.createCaptureSession(
                            listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    session.capture(captureRequestBuilder.build(), null, handler)
                                }
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    if (cont.isActive) cont.resume(null)
                                    camera.close()
                                }
                            },
                            handler
                        )
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        if (cont.isActive) cont.resume(null)
                        camera.close()
                    }
                }, handler)

                cont.invokeOnCancellation { handlerThread.quitSafely() }
            }
        } catch (e: SecurityException) {
            null
        }
    }
}
