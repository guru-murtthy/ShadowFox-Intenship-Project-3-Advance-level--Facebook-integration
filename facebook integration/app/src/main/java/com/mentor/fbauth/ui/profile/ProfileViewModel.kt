package com.mentor.fbauth.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.AccessToken
import com.mentor.fbauth.data.model.UserProfile
import com.mentor.fbauth.domain.ProfileRepository

/**
 * Screen state models for the Graph API Profile UI.
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile, val emailDenied: Boolean) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object LoggedOut : ProfileUiState()
}

/**
 * Coordinates fetching, displaying, and checking permission scopes of the Facebook User Profile.
 */
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableLiveData<ProfileUiState>(ProfileUiState.Loading)
    val uiState: LiveData<ProfileUiState> = _uiState

    /**
     * Attempts to fetch fresh data from Graph API. If it fails, falls back to the encrypted cache.
     */
    fun loadUserProfile() {
        _uiState.value = ProfileUiState.Loading
        
        val cached = repository.getLocalProfile()
        val token = repository.getSessionToken()
        
        if (token == null) {
            _uiState.value = ProfileUiState.Error("Active session token not found. Please re-authenticate.")
            return
        }

        // Call repository to issue the async Graph API request
        repository.fetchUserProfile(token) { result ->
            result.fold(
                onSuccess = { freshProfile ->
                    val emailDenied = isEmailPermissionDenied()
                    _uiState.postValue(ProfileUiState.Success(freshProfile, emailDenied))
                },
                onFailure = { error ->
                    // Network failure fallback: return cached data from EncryptedSharedPreferences if available
                    if (cached != null) {
                        val emailDenied = isEmailPermissionDenied()
                        _uiState.postValue(ProfileUiState.Success(cached, emailDenied))
                    } else {
                        _uiState.postValue(ProfileUiState.Error(error.localizedMessage ?: "Failed to retrieve user profile."))
                    }
                }
            )
        }
    }

    /**
     * Checks the granted permissions inside the SDK's active AccessToken.
     * Return true if 'email' scope was explicitly denied/revoked by the user.
     */
    private fun isEmailPermissionDenied(): Boolean {
        val currentToken = AccessToken.getCurrentAccessToken()
        return currentToken != null && !currentToken.permissions.contains("email")
    }

    /**
     * Handles User Logout.
     * Revokes local secure preferences and signs out of the Facebook SDK session.
     */
    fun logout() {
        // Facebook SDK logout clears the current AccessToken internally
        com.facebook.login.LoginManager.getInstance().logOut()
        
        // Revoke our custom secure token storage
        repository.clearSession()
        
        _uiState.value = ProfileUiState.LoggedOut
    }
}
