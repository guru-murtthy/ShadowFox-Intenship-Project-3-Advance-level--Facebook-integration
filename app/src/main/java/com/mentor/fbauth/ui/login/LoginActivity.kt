package com.mentor.fbauth.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import com.mentor.fbauth.R
import com.mentor.fbauth.databinding.ActivityLoginBinding
import com.mentor.fbauth.ui.factory.ViewModelFactory
import com.mentor.fbauth.ui.profile.ProfileActivity

/**
 * Handles User authentication via OAuth 2.0 and Facebook Login SDK.
 * Registers SDK lifecycle hooks and delegates token callbacks to LoginViewModel.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var callbackManager: CallbackManager

    // Lazy initialization of ViewModel using factory provider
    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Facebook Callback Manager to catch Activity Results
        callbackManager = CallbackManager.Factory.create()

        setupFacebookLogin()
        observeLoginState()
    }

    /**
     * Integrates standard Facebook Login Button and specifies required scopes.
     *
     * OAuth 2.0 HANDSHAKE STEP 1 & 2:
     * Requesting 'public_profile' and 'email' scopes triggers the Facebook consent sheet,
     * prompting the user to grant access to these fields.
     */
    private fun setupFacebookLogin() {
        // Define scopes to request
        binding.btnFacebookLogin.setPermissions("public_profile", "email")

        // Register authentication result handler callbacks
        binding.btnFacebookLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // Pass Facebook's AccessToken object containing the encrypted session key to the ViewModel
                viewModel.onLoginSuccess(result.accessToken)
            }

            override fun onCancel() {
                viewModel.onLoginCanceled()
            }

            override fun onError(error: FacebookException) {
                viewModel.onLoginFailed(error)
            }
        })
    }

    /**
     * Observe MVVM states to update the UI and perform redirection.
     */
    private fun observeLoginState() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> {
                    binding.btnFacebookLogin.isEnabled = true
                }
                is LoginState.Loading -> {
                    binding.btnFacebookLogin.isEnabled = false
                }
                is LoginState.Success -> {
                    binding.btnFacebookLogin.isEnabled = true
                    Toast.makeText(this, "Success: OAuth handshake completed!", Toast.LENGTH_SHORT).show()
                    
                    // Route user to Profile Activity, resetting stack
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                is LoginState.Error -> {
                    binding.btnFacebookLogin.isEnabled = true
                    showError(state.message)
                    viewModel.resetState()
                }
            }
        }
    }

    /**
     * Show validation or SDK runtime errors using Material Snackbar.
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_red))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    /**
     * SDK Lifecycle Hook:
     * Activity Result returns execution focus here when returning from Chrome Custom Tabs/Facebook App.
     * The callbackManager intercepts this payload and triggers the onSuccess/onCancel/onError callbacks.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Propagate result payload to Facebook SDK callbackManager
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
