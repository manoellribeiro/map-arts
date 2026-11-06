package manoellribeiro.dev.martp.scenes.success

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import manoellribeiro.dev.martp.core.extensions.shareArtImage
import manoellribeiro.dev.martp.scenes.main.MainActivity

class SuccessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        val imagePathLocation = intent.getStringExtra(IMAGE_PATH_LOCATION_EXTRA)
        if (imagePathLocation == null) {
            finish()
            return
        }
        setContent {
            SuccessScreen(
                imagePathLocation = imagePathLocation,
                onShareClick = { shareArtImage(imagePathLocation) },
                onCreateAnotherArtClick = ::openEditTab
            )
        }
    }

    private fun openEditTab() {
        startActivity(MainActivity.newIntentOpeningEditTab(this))
        finish()
    }

    companion object {
        private const val IMAGE_PATH_LOCATION_EXTRA = "image_path_location"

        fun newIntent(context: Context, imagePathLocation: String): Intent =
            Intent(context, SuccessActivity::class.java)
                .putExtra(IMAGE_PATH_LOCATION_EXTRA, imagePathLocation)
    }
}
