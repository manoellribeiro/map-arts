package manoellribeiro.dev.martp.core.extensions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import manoellribeiro.dev.martp.R
import java.io.File

fun Context.shareArtImage(imagePathLocation: String) {
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    sharingIntent.type = "image/*"
    val uri = FileProvider.getUriForFile(
        this,
        packageName + ".provider",
        File(imagePathLocation)
    )
    sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_art_message))
    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
    startActivities(arrayOf(Intent.createChooser(sharingIntent, "Share with")))
}
