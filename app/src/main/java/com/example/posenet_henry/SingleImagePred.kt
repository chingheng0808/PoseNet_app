package com.example.posenet_henry

import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import java.io.InputStream
import java.util.*

internal class SuperResPerformer(
) {

    fun upscale(inputStream: InputStream, ortEnv: OrtEnvironment, ortSession: OrtSession): Result {
        var result = Result()

        val imgBitmap = BitmapFactory.decodeStream(inputStream)
        val rawBitmap = imgBitmap.let { Bitmap.createScaledBitmap(it, 256, 256, true) }
        val imgData = preProcess(rawBitmap)

        val shape = longArrayOf(1, 3, 256, 256)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            imgData,
            shape,
        )
        val inputName = ortSession.inputNames?.iterator()?.next()
        val startTime = SystemClock.uptimeMillis()
        inputTensor.use {
            val output = ortSession.run(Collections.singletonMap(inputName, inputTensor))

            output.use {
                result.processTimeMs = SystemClock.uptimeMillis() - startTime
                @Suppress("UNCHECKED_CAST")
                val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]

                result.position = floatArrayOf(rawOutput[0], rawOutput[1], rawOutput[2])
                result.orientation = floatArrayOf(rawOutput[3], rawOutput[4], rawOutput[5])
            }
        }
        return result
    }
}