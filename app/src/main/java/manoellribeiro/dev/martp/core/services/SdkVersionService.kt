package manoellribeiro.dev.martp.core.services

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

class SdkVersionService {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun isAtLeastTiramisu(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

}
