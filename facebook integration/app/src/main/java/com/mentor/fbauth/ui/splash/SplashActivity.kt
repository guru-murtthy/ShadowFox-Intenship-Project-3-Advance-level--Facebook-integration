package com.mentor.fbauth.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.mentor.fbauth.data.local.SessionManager
import com.mentor.fbauth.databinding.ActivitySplashBinding
import com.mentor.fbauth.ui.login.LoginActivity
import com.mentor.fbauth.ui.profile.ProfileActivity

/**
 * Entrance activity of the application.
 * Verifies OAuth 2.0 Access Token status to determine routing.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState: Bundle?)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Add a deliberate micro-delay of 1.5s to display the premium theme assets
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 1500)
    }

    /**
     * Session Restoration Logic:
     * We inspect the Facebook SDK native token state AND our secure EncryptedSharedPreferences cache.
     * Both must exist and be valid to bypass re-authentication.
     */
    private fun checkSessionAndNavigate() {
        val currentFbToken = AccessToken.getCurrentAccessToken()
        val storedSessionToken = sessionManager.getAccessToken()

        val hasValidFbToken = currentFbToken != null && !currentFbToken.isExpired
        val hasStoredToken = !storedSessionToken.isNullOrEmpty()

        if (hasValidFbToken && hasStoredToken) {
            // Restore session: Session persists across app restarts without re-login
            navigateToProfile()
        } else {
            // Session expired or incomplete: Clear remnants and route to authentication panel
            sessionManager.clearSession()
            navigateToLogin()
        }
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
