package com.mentor.fbauth.ui.share

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mentor.fbauth.domain.ProfileRepository

/**
 * Screen states indicating the current state of a wall share posting interaction.
 */
sealed class ShareState {
    object Idle : ShareState()
    object Sharing : ShareState()
    object Success : ShareState()
    data class ValidationError(val errorMsg: String) : ShareState()
    data class Error(val errorMsg: String) : ShareState()
    object Canceled : ShareState()
}

/**
 * Handles validation of post content and updates statistics when the Facebook SDK callbacks
 * trigger success events.
 */
class ShareViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _shareState = MutableLiveData<ShareState>(ShareState.Idle)
    val shareState: LiveData<ShareState> = _shareState

    /**
     * Called when the user initiates sharing. Validates input bounds.
     */
    fun onShareInitiated(content: String) {
        if (content.trim().isEmpty()) {
            _shareState.value = ShareState.ValidationError("Post content cannot be empty.")
            return
        }
        _shareState.value = ShareState.Sharing
    }

    /**
     * Called when the Facebook SDK signals that content was successfully shared.
     * We increment the posts counts securely in our local caches.
     */
    fun onShareSuccess() {
        repository.incrementLocalPostCount()
        _shareState.value = ShareState.Success
    }

    /**
     * Called when the user cancels the share composer overlay.
     */
    fun onShareCanceled() {
        _shareState.value = ShareState.Canceled
    }

    /**
     * Called when the Facebook SDK fails to publish.
     */
    fun onShareError(error: Throwable) {
        _shareState.value = ShareState.Error(error.localizedMessage ?: "Unknown sharing failure.")
    }

    /**
     * Resets the composer ui states.
     */
    fun resetState() {
        _shareState.value = ShareState.Idle
    }
}
