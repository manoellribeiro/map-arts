package com.example.generatemaparts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.generatemaparts.core.extensions.executeIfNotNull
import processing.android.CompatUtils
import processing.android.PFragment

class MainActivity : AppCompatActivity() {

    private var sketch: Sketch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frameLayout = FrameLayout(this)
        frameLayout.id = CompatUtils.getUniqueViewId()
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(frameLayout, layoutParams)

        sketch = Sketch()

        val processingFragment = PFragment(sketch)
        processingFragment.setView(frameLayout, this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        sketch.executeIfNotNull {
            sketch?.onRequestPermissionsResult(
                requestCode, permissions, grantResults
            )
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sketch.executeIfNotNull {
            sketch?.onNewIntent(intent)
        }
    }

}