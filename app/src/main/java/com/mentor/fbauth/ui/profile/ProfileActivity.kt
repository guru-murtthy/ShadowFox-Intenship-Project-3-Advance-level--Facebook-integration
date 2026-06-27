package com.mentor.fbauth.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import com.mentor.fbauth.R
import com.mentor.fbauth.data.model.UserProfile
import com.mentor.fbauth.databinding.ActivityProfileBinding
import com.mentor.fbauth.ui.factory.ViewModelFactory
import com.mentor.fbauth.ui.login.LoginActivity
import com.mentor.fbauth.ui.share.ShareActivity

/**
 * Handles rich Profile Display of fetched Graph API information.
 * Supports token cleanup, logout, and handling email scope denials.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var callbackManager: CallbackManager

    private val viewModel: ProfileViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Local Callback Manager to handle in-app permission re-requests
        callbackManager = CallbackManager.Factory.create()

        setupListeners()
        observeViewModel()

        // Fetch User Profile details from the repository on view load
        viewModel.loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        // Reload profile data when returning to screen (e.g. after post shared increment)
        viewModel.loadUserProfile()
    }

    private fun setupListeners() {
        // Logout handler
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        // Routing to share screen
        binding.btnShareContent.setOnClickListener {
            val intent = Intent(this, ShareActivity::class.java)
            startActivity(intent)
        }

        // Email Rationale button - Re-prompts the user to authorize 'email' permission
        binding.btnRequestEmailPermission.setOnClickListener {
            reRequestEmailPermission()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProfileUiState.Loading -> {
                    binding.progressProfile.visibility = View.VISIBLE
                    binding.scrollProfileContent.visibility = View.GONE
                }
                is ProfileUiState.Success -> {
                    binding.progressProfile.visibility = View.GONE
                    binding.scrollProfileContent.visibility = View.VISIBLE
                    bindUserProfile(state.profile, state.emailDenied)
                }
                is ProfileUiState.Error -> {
                    binding.progressProfile.visibility = View.GONE
                    showError(state.message)
                }
                is ProfileUiState.LoggedOut -> {
                    // Redirect back to LoginActivity
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    /**
     * Map UserProfile data elements to Material UI layouts.
     */
    private fun bindUserProfile(profile: UserProfile, emailDenied: Boolean) {
        binding.tvProfileName.text = profile.name
        binding.tvMemberSince.text = profile.memberSince
        binding.tvPostsCount.text = profile.appPostsCount.toString()

        // Set Link or fall back
        binding.tvProfileLink.text = profile.profileLink ?: "No profile link available"

        // Handle Email permission denial gracefully
        if (emailDenied || profile.email.isNullOrEmpty()) {
            binding.tvProfileEmail.text = getString(R.string.profile_email_fallback)
            binding.tvProfileEmail.setTextColor(getColor(R.color.error_red))
            binding.cardEmailWarning.visibility = View.VISIBLE
        } else {
            binding.tvProfileEmail.text = profile.email
            binding.tvProfileEmail.setTextColor(getColor(R.color.text_secondary))
            binding.cardEmailWarning.visibility = View.GONE
        }

        // Render circular profile picture with Glide/Coil
        binding.imgProfilePicture.load(profile.profilePictureUrl) {
            crossfade(true)
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
        }
    }

    /**
     * Re-requests email permissions if previously denied.
     * Hooks into standard Facebook Login SDK authorization flow.
     */
    private fun reRequestEmailPermission() {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                if (result.accessToken.permissions.contains("email")) {
                    Toast.makeText(this@ProfileActivity, "Email permission granted!", Toast.LENGTH_SHORT).show()
                    viewModel.loadUserProfile()
                } else {
                    Toast.makeText(this@ProfileActivity, "Permission still denied. Falling back.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancel() {
                Toast.makeText(this@ProfileActivity, "Permission request cancelled.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                showError("Re-request failed: ${error.localizedMessage}")
            }
        })

        // Request read permission for email explicitly
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
    }

    /**
     * Propagate callback parameters back to the local Facebook SDK login manager
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_red))
            .setTextColor(getColor(R.color.white))
            .show()
    }
}
