package com.mentor.fbauth.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.Sharer
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.google.android.material.snackbar.Snackbar
import com.mentor.fbauth.R
import com.mentor.fbauth.databinding.ActivityShareBinding
import com.mentor.fbauth.ui.factory.ViewModelFactory

/**
 * Handles the Facebook Composer Sharing flow.
 * Instantiates the ShareDialog to post link quotes on the user's wall.
 */
class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private lateinit var callbackManager: CallbackManager
    private lateinit var shareDialog: ShareDialog

    private val viewModel: ShareViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState: Bundle?)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callbackManager = CallbackManager.Factory.create()
        setupShareDialog()
        setupListeners()
        observeViewModel()
    }

    /**
     * Initializes the Facebook SDK Share Dialog and registers execution callbacks.
     */
    private fun setupShareDialog() {
        shareDialog = ShareDialog(this)
        
        // Register callbacks on the dialog container to forward sharer responses to the ViewModel
        shareDialog.registerCallback(callbackManager, object : FacebookCallback<Sharer.Result> {
            override fun onSuccess(result: Sharer.Result) {
                viewModel.onShareSuccess()
            }

            override fun onCancel() {
                viewModel.onShareCanceled()
            }

            override fun onError(error: FacebookException) {
                viewModel.onShareError(error)
            }
        })
    }

    private fun setupListeners() {
        binding.btnSubmitShare.setOnClickListener {
            val contentText = binding.etShareInput.text.toString().trim()
            viewModel.onShareInitiated(contentText)
        }

        binding.btnCancelShare.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.shareState.observe(this) { state ->
            when (state) {
                is ShareState.Idle -> {
                    binding.tilShareInput.error = null
                }
                is ShareState.Sharing -> {
                    binding.tilShareInput.error = null
                    triggerFacebookSharingIntent()
                }
                is ShareState.Success -> {
                    Toast.makeText(this, getString(R.string.share_success), Toast.LENGTH_LONG).show()
                    finish() // Close composer and return to Profile Screen
                }
                is ShareState.ValidationError -> {
                    binding.tilShareInput.error = state.errorMsg
                    viewModel.resetState()
                }
                is ShareState.Error -> {
                    showError(state.errorMsg)
                    viewModel.resetState()
                }
                is ShareState.Canceled -> {
                    Toast.makeText(this, getString(R.string.share_canceled), Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
            }
        }
    }

    /**
     * Constructs the ShareLinkContent payload and triggers Facebook's sharing overlay.
     */
    private fun triggerFacebookSharingIntent() {
        val userQuote = binding.etShareInput.text.toString()

        // Build sharing content wrapper
        val content = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("https://developers.facebook.com")) // Mandatory target link URL
            .setQuote(userQuote) // User-authored text overlay on post
            .build()

        if (ShareDialog.canShow(ShareLinkContent::class.java)) {
            shareDialog.show(content)
        } else {
            showError("Sharing dialog not supported on this device. Install Facebook App or check configurations.")
            viewModel.resetState()
        }
    }

    /**
     * Route SDK Activity Results back to callbackManager
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
