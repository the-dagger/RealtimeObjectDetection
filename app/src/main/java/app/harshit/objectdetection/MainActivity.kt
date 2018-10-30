package app.harshit.objectdetection

import ai.fritz.core.Fritz
import ai.fritz.fritzvisionobjectmodel.FritzVisionObjectPredictor
import ai.fritz.vision.inputs.FritzVisionImage
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var renderScript: RenderScript
    private lateinit var yuvToRGB: ScriptIntrinsicYuvToRGB
    private var yuvDataLength: Int = 0
    private lateinit var allocationIn: Allocation
    private lateinit var allocationOut: Allocation
    private lateinit var bitmapOut: Bitmap

    private val itemMap by lazy {
        hashMapOf<String, Int>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fritz.configure(this)

        val objectPredictor = FritzVisionObjectPredictor.getInstance(this)
        var fritzVisionImage: FritzVisionImage

        cameraView.addFrameProcessor {

            if (yuvDataLength == 0) {
                //Run this only once
                initializeData()
            }

            //Camera Preview returns NV21, so convert it to Bitmap :
            //https://stackoverflow.com/a/43551798/5471095
            allocationIn.copyFrom(it.data)
            yuvToRGB.forEach(allocationOut)
            allocationOut.copyTo(bitmapOut)
            fritzVisionImage = FritzVisionImage.fromBitmap(bitmapOut, it.rotation)
            val visionObjects = objectPredictor.predict(fritzVisionImage)

            itemMap.clear()

            visionObjects.forEach { visionObject ->
                if (itemMap.containsKey(visionObject.visionLabel.text))
                    itemMap[visionObject.visionLabel.text] = itemMap[visionObject.visionLabel.text]!! + 1
                itemMap[visionObject.visionLabel.text] = 1
            }

            runOnUiThread {
                tvDetectedItem.text = ""
                itemMap.forEach { map ->
                    tvDetectedItem.append("Detected ${map.value} ${map.key}\n")
                }
            }
        }
    }

    private fun initializeData() {
        yuvDataLength = cameraView.previewSize?.height!! * cameraView.previewSize?.width!! * 3 / 2
        renderScript = RenderScript.create(baseContext)
        yuvToRGB = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
        allocationIn = Allocation.createSized(renderScript, Element.U8(renderScript), yuvDataLength)
        bitmapOut = Bitmap.createBitmap(cameraView.previewSize?.width!!, cameraView.previewSize?.height!!, Bitmap.Config.ARGB_8888)
        allocationOut = Allocation.createFromBitmap(renderScript, bitmapOut)
        yuvToRGB.setInput(allocationIn)
    }

    override fun onStart() {
        super.onStart()
        cameraView.start()
    }

    override fun onStop() {
        super.onStop()
        cameraView.stop()
    }
}