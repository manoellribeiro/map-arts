package manoellribeiro.dev.martp.scenes.locationAcessDetails

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.databinding.ActivityLocationAccessDetailsBinding
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity

class LocationAccessDetailsActivity: AppCompatActivity() {

    private lateinit var binding: ActivityLocationAccessDetailsBinding
    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationAccessDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRequireLocationPermissionLauncher()
        setupAllowLocationAccessButton()
    }


    private fun setupAllowLocationAccessButton() {
        binding.allowLocationAccessMB.setOnClickListener {
            askForLocationPermission()
        }
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                val intent = Intent(this, CreateNewMapArtActivity::class.java)
                finish()
                startActivity(intent)
            }
        }
    }

    private fun askForLocationPermission() {
        requireLocationPermissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }



}