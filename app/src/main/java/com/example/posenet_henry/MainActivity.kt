package com.example.posenet_henry

import ai.onnxruntime.*
import com.example.posenet_henry.databinding.ActivityMainBinding
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RadioGroup
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity() {
    private val screenLayout by lazy{
        findViewById<ConstraintLayout>(R.id.screenLayout)
    }
    private val myLinear by lazy{
        findViewById<LinearLayout>(R.id.mylinear)
    }
    private val mapView by lazy{
        findViewById<ImageView>(R.id.mapView)
    }
    private val radioGroup by lazy{
        findViewById<RadioGroup>(R.id.radioGroup)
    }
    lateinit var mainHandler: Handler
    lateinit var myDraw: CanvasCl
    // locX, locY is at the local coordinate
    private var locX = 20f
    private var locY = 20f

    private lateinit var binding: ActivityMainBinding

    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var mCameraProvider: ProcessCameraProvider? = null
    private var mPreview: Preview?= null
    private val mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var ortEnv: OrtEnvironment? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    //    private var enableQuantizedModel: Boolean = false
    private var modelSelectID: Int = R.id.radioButton1

    private val translation = floatArrayOf(3.5f, 2.0f)
    private val modelDict = mapOf(R.id.radioButton1 to R.raw.magiclab_1115_infer, R.id.radioButton2 to R.raw.magiclab_1115_int8, R.id.radioButton3 to R.raw.magiclab_1115_pruned_basic_sp75_ft
        , R.id.radioButton4 to R.raw.magiclab_1115_2_infer, R.id.radioButton5 to R.raw.magiclab_1115_2_int8, R.id.radioButton6 to R.raw.magiclab_1115_2_pruned_basic_sp60_ft)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar)
        ortEnv = OrtEnvironment.getEnvironment()
        // Request Camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            modelSelectID = id
            setORTAnalyzer()
        }

        // Drawing part
        screenLayout.post{
            val sysLayout = listOf(screenLayout.width, screenLayout.height) //Int, 無法直接使用Resources.getSystem().displayMetrics來獲取長寬，因為每支手機尺寸可能不同
            val mapLayout = listOf(mapView.layoutParams.width, mapView.layoutParams.height) //Int
            myLinear.post{

                val mapLocate: MutableList<Int> = sysLayout.toMutableList()

                mapLocate[0] -= (myLinear.width) // 地圖都在螢幕最左邊
                mapLocate[1] -= (myLinear.height + mapLayout[1])
                // Initialize Canvas
                myDraw = CanvasCl(this, mapLocate[0], mapLocate[1], mapLayout[0], mapLayout[1])
                addContentView(myDraw, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                // Test for update points on canvas
//                mainHandler = Handler(Looper.getMainLooper())
//                mainHandler.post(object : Runnable {
//                    override fun run() {
//                        updatePoint()
//                        mainHandler.postDelayed(this, 1000)
//                    }
//                })
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            mCameraProvider = cameraProvider

            // Preview
            mPreview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .build()

//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            try {
                mCameraProvider!!.unbindAll()

                mCameraProvider!!.bindToLifecycle(
                    this, mCameraSelector, mPreview, imageCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            setORTAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        ortEnv?.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

        }
    }

    private fun updateUI(result: Result) {
//        if (result.position.isEmpty() || result.orientation.isEmpty())
//            return

        runOnUiThread {
            binding.estimatedPositionValue.text = "%.3f, %.3f, %.3f".format(result.position[0],result.position[1],result.position[2])
            binding.estimatedOrientationValue.text = "%.3f, %.3f, %.3f".format(result.orientation[0],result.orientation[1],result.orientation[2])

            binding.inferenceTimeValue.text = result.processTimeMs.toString() + "ms"
            binding.percentMeter.progress = (100* result.processTimeMs / 500).toInt()

            if(binding.checkBoxMap.isChecked)
                updatePoint(result.position, true)
            else
                updatePoint(result.position, false)
        }
    }

    // Read ort model into a ByteArray, run in background
    private suspend fun readModel(): ByteArray = withContext(Dispatchers.IO) {
        var modelID = modelDict[modelSelectID]
        if(modelID == null){
            modelID = R.raw.magiclab_1115
        }

        resources.openRawResource(modelID).readBytes()
    }

    // Create a new ORT session in background
    private suspend fun createOrtSession(): OrtSession? = withContext(Dispatchers.Default) {
        ortEnv?.createSession(readModel())
    }

    // Create a new ORT session and then change the ImageAnalysis.Analyzer
    // This part is done in background to avoid blocking the UI
    private fun setORTAnalyzer(){
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            imageAnalysis?.setAnalyzer(
                backgroundExecutor,
                ORTAnalyzer(createOrtSession(), ::updateUI)
            )
        }
    }

    companion object {
        const val TAG = "ORTImageClassifier"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun updatePoint(positionRes: FloatArray, isTrajectory: Boolean){
        //Test
        // Scalar = pictureSize/realWoldSize
        val scalarX = 910.0f/7.0f
        val scalarY = 567.0f/4.0f
        locX = -(positionRes[0]-translation[0])*scalarX
        locY = -(positionRes[2]-translation[1])*scalarY
//        System.out.println(positionRes[0])
        // Update Canvas View
        myDraw.clearPoint()
        myDraw.drawPoint_(locX, locY, isTrajectory)

        val parent = myDraw.parent as ViewGroup
        if(parent != null){
            parent.removeView(myDraw)
        }

        addContentView(myDraw, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    // Menu override functions
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.unBindCamX -> {
                mCameraProvider?.unbindAll()
                return true
            }
            R.id.bindCamX ->{
                mCameraProvider?.bindToLifecycle(this, mCameraSelector, mPreview, imageCapture, imageAnalysis)
                return true
            }
            R.id.photoMode ->{
                findViewById<PreviewView>(R.id.viewFinder).visibility = GONE
                findViewById<View>(R.id.imageView).visibility = VISIBLE

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
