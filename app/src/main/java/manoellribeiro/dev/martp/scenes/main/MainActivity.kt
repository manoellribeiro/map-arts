package manoellribeiro.dev.martp.scenes.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import manoellribeiro.dev.martp.databinding.ActivityMainBinding
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity
import manoellribeiro.dev.martp.scenes.locationAcessDetails.LocationAccessDetailsActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRequireLocationPermissionLauncher()
        setupBottomNavigationBar()
        setupViews()
    }

    private fun setupBottomNavigationBar() {
        val navHostFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerFCV.id) as NavHostFragment
        val navController = navHostFragment.findNavController()
        NavigationUI.setupWithNavController(binding.bottomNavigationBarBNV, navController)
    }

    private fun setupViews() = with(binding) {
        createNewArtFAB.setOnClickListener {
            verifyPermissionToAccessLocation()
        }
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                openCreateNewMapArtScene()
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    openLocationAccessDetailScene()
                }
            }
        }
    }

    private fun openCreateNewMapArtScene() {
        val intent = Intent(this, CreateNewMapArtActivity::class.java)
        startActivity(intent)
    }

    private fun openLocationAccessDetailScene() {
        val intent = Intent(this, LocationAccessDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun verifyPermissionToAccessLocation() {
        val didUserAlreadyGivePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when{
            didUserAlreadyGivePermission -> {
                openCreateNewMapArtScene()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                openLocationAccessDetailScene()
            }
            else -> {
                requireLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }
}