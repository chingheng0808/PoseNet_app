package com.example.posenet_henry

import ai.onnxruntime.*
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.*


internal data class Result(
    var position: FloatArray = floatArrayOf(),
    var orientation: FloatArray = floatArrayOf(),
    var processTimeMs: Long = 0
) {}

// omit constructor
internal class ORTAnalyzer(
    private val ortSession: OrtSession?,
    private val callBack: (Result) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        // Convert the input image to bitmap and resize to desired size for model input
        val imgBitmap = image.toBitmap()
        // Do rescaling. Filter is set to true: use bilinear interpolation, false: nearest-neighbor
        val rawBitmap = imgBitmap.let { Bitmap.createScaledBitmap(it, 384, 1920*384/1080, true) }
        /******** We don't use rotation info from the device sensor ********/
//        val bitmap = rawBitmap?.rotate(image.imageInfo.rotationDegrees.toFloat())
//        val bitmap = rawBitmap

        if (rawBitmap != null) {
            val result = Result()

            val imgData = preProcess(rawBitmap)
            val inputName = ortSession?.inputNames?.iterator()?.next()
            val shape = longArrayOf(1, 3, (1920*384/1080), 384)
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                val startTime = SystemClock.uptimeMillis()
                tensor.use {
                    val output = ortSession?.run(Collections.singletonMap(inputName, tensor))
                    output.use {
                        result.processTimeMs = SystemClock.uptimeMillis() - startTime
                        @Suppress("UNCHECKED_CAST")
                        val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]

                        result.position = floatArrayOf(rawOutput[2], rawOutput[0], rawOutput[1])
                        result.orientation = floatArrayOf(rawOutput[5], rawOutput[3], rawOutput[4])
                    }
                }
            }
            callBack(result)
        }

        image.close()
    }

    // We can switch analyzer in the app, need to make sure the native resources are freed
    protected fun finalize() {
        ortSession?.close()
    }
}