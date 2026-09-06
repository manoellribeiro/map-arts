package manoellribeiro.dev.martp.scenes.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import manoellribeiro.dev.martp.databinding.ActivityMainBinding
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity
import manoellribeiro.dev.martp.scenes.locationAcessDetails.LocationAccessDetailsActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationBarView
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.extensions.dp
import manoellribeiro.dev.martp.core.extensions.isPositive
import kotlin.getValue


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
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
        setStatusBarColor()
        setupObservables()
        viewModel.handleBadgesVisibilities()
    }

    private fun setupObservables() {
        viewModel.mainState.observe(this) { state ->
            when(state) {
                is MainUiState.SetUserInfoBadgeVisibility -> setupUserInfoBadgeVisibility(state.visible)
            }
        }
    }

    private fun setupUserInfoBadgeVisibility(visible: Boolean) {
        binding.bottomNavigationBarBNV.getOrCreateBadge(R.id.userInfoFragment).isVisible = visible
    }

    private fun setupBottomNavigationBar() {
        val navHostFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerFCV.id) as NavHostFragment
        val navController = navHostFragment.findNavController()
        NavigationUI.setupWithNavController(binding.bottomNavigationBarBNV, navController)
    }

    fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                view.setBackgroundColor(getColor(R.color.blue))
                insets
            }
        } else {
            // For Android 14 and below
            window.statusBarColor = getColor(R.color.blue)
        }
    }

    private fun setupViews() = with(binding) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationBarBNV) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = height + insets.bottom
                if(insets.bottom.isPositive()) {
                    bottomNavigationBarBNV.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
                    createNewArtFAB.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = 8.dp(resources)
                    }
                    createNewArtFAB.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    }
                }
                ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationBarBNV, null)
                //bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

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