package com.example.posenet_henry

import android.content.Context
import android.graphics.*
import android.view.View

class CanvasCl(context: Context, sl: Int, st: Int, w: Int, h: Int) : View(context) {
    private var extraCanvas: Canvas //Canvas定義了可以在屏幕上繪製的形狀。
    private var extraBitmap: Bitmap //使用畫布繪圖 API繪製緩存位圖
    private var mapStartLeft = sl.toFloat()
    private var mapStartTop = st.toFloat()

    private var pointsRecord: FloatArray = floatArrayOf()

    //paint 定義您繪製的每個形狀的顏色、樣式、字體等。
    private val linePaint = Paint().apply {
        color = Color.YELLOW // 畫筆顏色
        isAntiAlias = true // 定義是否應用邊緣平滑。
        isDither = true // 影響精度高於設備的顏色下採
        // 樣的方式。
        style = Paint.Style.STROKE // 指定要繪製的圖元是填充、描邊還是兩者（以相同顏色）。default: FILL
        strokeJoin = Paint.Join.ROUND // 指定線條和曲線段如何在描邊路徑上連接 , default: MITER
        strokeCap = Paint.Cap.ROUND // 指定描邊線和路徑的開始和結束方式。 default: BUTT
        strokeWidth = 5f // 以像素為單位指定筆劃的寬度。 default: Hairline-width (really thin)
    }
    private val pointPaint = Paint().apply {
        color = Color.RED // 畫筆顏色
        isAntiAlias = true // 定義是否應用邊緣平滑。
        isDither = true // 影響精度高於設備的顏色下採樣的方式。
        strokeJoin = Paint.Join.ROUND // 指定線條和曲線段如何在描邊路徑上連接 , default: MITER
        strokeCap = Paint.Cap.ROUND // 指定描邊線和路徑的開始和結束方式。 default: BUTT
        strokeWidth = 15f // 以像素為單位指定筆劃的寬度。 default: Hairline-width (really thin)
    }

    private var currentX = 0f
    private var currentY = 0f

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    init {
        extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
//        extraBitmap = resizeImg(extraBitmap)
        extraCanvas = Canvas(extraBitmap)
//        extraCanvas.drawColor(Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(extraBitmap, mapStartLeft, mapStartTop, null)
    }

//    //觸控手勢處理
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        motionTouchEventX = event.x - mapStartLeft
//        motionTouchEventY = event.y - mapStartTop
//
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> touchStart()
//            MotionEvent.ACTION_MOVE -> touchMove()
//        }
//        return true
//    }
//
//    private fun touchStart() {
//        currentX = motionTouchEventX
//        currentY = motionTouchEventY
//    }
//
//    private fun touchMove() {
//        val stopX = motionTouchEventX
//        val stopY = motionTouchEventY
//        extraCanvas.drawLine(currentX, currentY, stopX, stopY, linePaint)
//        currentX = motionTouchEventX
//        currentY = motionTouchEventY
//
//        invalidate()
//    }

    private fun resizeImg(bitmap: Bitmap): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val aspRat: Float = w / h
        val h_ = 600
        val w_ = (h_ * aspRat).toInt()
        return Bitmap.createScaledBitmap(bitmap, w_, h_, false)
    }

    private fun addElement(arr: FloatArray, element: Float): FloatArray {
        val mutableArray = arr.toMutableList()
        mutableArray.add(element)
        return mutableArray.toFloatArray()
    }

    private fun removeFirstPoint(arr: FloatArray): FloatArray{
        val mutableArray = arr.toMutableList()
        mutableArray.removeFirst()  //remove x
        mutableArray.removeFirst()  //remove y
        return mutableArray.toFloatArray()
    }


    private fun drawTrajectory(x: Float, y: Float) {
        if (pointsRecord.count() > 4)
            for (i in 0..pointsRecord.count() - 3 step 2)
                extraCanvas.drawLine(pointsRecord[i], pointsRecord[i + 1], pointsRecord[i + 2], pointsRecord[i + 3], linePaint)
    }

    fun drawPoint_(x: Float, y: Float, isTrajectory: Boolean) {
        extraCanvas.drawPoint(x, y, pointPaint)
        if(pointsRecord.count() > 100)
            pointsRecord = removeFirstPoint(pointsRecord)

        pointsRecord = addElement(pointsRecord, x)
        pointsRecord = addElement(pointsRecord, y)

        if(isTrajectory)
            drawTrajectory(x, y)
    }

    fun clearPoint() {
        extraCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
}