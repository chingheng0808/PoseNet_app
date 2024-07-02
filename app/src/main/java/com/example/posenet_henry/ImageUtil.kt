package com.example.posenet_henry

import android.graphics.*
import java.nio.FloatBuffer

const val DIM_BATCH_SIZE = 1;
const val DIM_PIXEL_SIZE = 3;
const val IMAGE_SIZE_X = 384;
const val IMAGE_SIZE_Y = 1920*384/1080;
val MEAN = floatArrayOf(0.5128f, 0.4938f, 0.4747f)
val STD = floatArrayOf(0.2537f, 0.2484f, 0.2360f)


fun preProcess(bitmap: Bitmap): FloatBuffer {
    val imgData = FloatBuffer.allocate(
        DIM_BATCH_SIZE
                * DIM_PIXEL_SIZE
                * IMAGE_SIZE_X
                * IMAGE_SIZE_Y
    )
    imgData.rewind()
    val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
    val bmpData = IntArray(stride)
    bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (i in 0 until IMAGE_SIZE_X) {
        for (j in 0 until IMAGE_SIZE_Y) {
            val idx = IMAGE_SIZE_Y * i + j
            val pixelValue = bmpData[idx]
            imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - MEAN[0]) / STD[0]))
            imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - MEAN[1]) / STD[1]))
            imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - MEAN[2]) / STD[2]))
        }
    }

    imgData.rewind()
    return imgData
}
