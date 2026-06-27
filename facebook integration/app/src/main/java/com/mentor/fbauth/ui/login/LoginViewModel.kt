package com.mentor.fbauth.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.AccessToken
import com.mentor.fbauth.domain.ProfileRepository

/**
 * States reflecting the OAuth 2.0 auth lifecycle during Facebook Login.
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val declinedPermissions: Set<String>) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Handles coordination of Facebook login states, saving access tokens to secure storage,
 * and parsing permission configuration results.
 */
class LoginViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    /**
     * Called when the Facebook SDK signals that login was successful.
     * Initiates OAuth 2.0 Token persistence.
     *
     * OAuth 2.0 HANDSHAKE STEP 3 (Exchanging Authorization Code for Access Token):
     * The Facebook SDK completes this under the hood and delivers the resulting AccessToken.
     * We capture this short-lived session token and store it securely in EncryptedSharedPreferences.
     */
    fun onLoginSuccess(accessToken: AccessToken) {
        _loginState.value = LoginState.Loading
        try {
            // Save the raw token securely using EncryptedSharedPreferences (not logged/exposed)
            repository.saveSessionToken(accessToken.token)
            
            // Check if any scopes (e.g. email) were declined by the user
            val declined = accessToken.declinedPermissions ?: emptySet()
            _loginState.value = LoginState.Success(accessToken.token, declined)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("Session encryption failure: ${e.localizedMessage}")
        }
    }

    /**
     * Called when the OAuth login was canceled.
     */
    fun onLoginCanceled() {
        _loginState.value = LoginState.Error("Login canceled by user.")
    }

    /**
     * Called when the Facebook SDK encounters an exception.
     */
    fun onLoginFailed(error: Throwable) {
        _loginState.value = LoginState.Error(error.localizedMessage ?: "Facebook authentication error.")
    }

    /**
     * Resets the authentication UI states.
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
