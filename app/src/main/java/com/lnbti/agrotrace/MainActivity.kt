package com.lnbti.agrotrace

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.lnbti.agrotrace.databinding.ActivityMainBinding
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private var systemBarInsets: Insets = Insets.NONE
    private var imeInsets: Insets = Insets.NONE
    private var bottomNavigationVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        updateSystemBarIconAppearance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationVisible = destination.id !in setOf(
                R.id.extractionFragment,
                R.id.dataViewFragment,
                R.id.documentDetailFragment
            )
            binding.bottomNavigation.visibility =
                if (bottomNavigationVisible) View.VISIBLE else View.GONE
            applyWindowInsets()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            systemBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            applyWindowInsets()
            insets
        }
        ViewCompat.requestApplyInsets(binding.drawerLayout)
    }

    private fun applyWindowInsets() {
        if (!::binding.isInitialized) return

        val contentBottomInset = if (bottomNavigationVisible) {
            0
        } else {
            max(systemBarInsets.bottom, imeInsets.bottom)
        }

        binding.navHostFragment.updateLayoutParams<ConstraintLayout.LayoutParams> {
            leftMargin = systemBarInsets.left
            topMargin = systemBarInsets.top
            rightMargin = systemBarInsets.right
            bottomMargin = contentBottomInset
        }

    }

    private fun updateSystemBarIconAppearance() {
        val isDarkTheme =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
