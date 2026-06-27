package com.mentor.fbauth.data.repository

import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.mentor.fbauth.data.local.SessionManager
import com.mentor.fbauth.data.model.UserProfile
import com.mentor.fbauth.domain.ProfileRepository

/**
 * Concrete implementation of ProfileRepository coordinating data between EncryptedSharedPreferences
 * and raw Graph API calls.
 */
class ProfileRepositoryImpl(
    private val sessionManager: SessionManager
) : ProfileRepository {

    override fun getSessionToken(): String? {
        return sessionManager.getAccessToken()
    }

    override fun saveSessionToken(token: String) {
        sessionManager.saveAccessToken(token)
    }

    override fun fetchUserProfile(token: String, onResult: (Result<UserProfile>) -> Unit) {
        val currentFbToken = AccessToken.getCurrentAccessToken()
        if (currentFbToken == null || currentFbToken.isExpired) {
            onResult(Result.failure(Exception("Active or non-expired Facebook AccessToken is missing.")))
            return
        }

        // Create the me request targeting the current user node
        val request = GraphRequest.newMeRequest(currentFbToken) { jsonObject, response ->
            if (response?.error != null) {
                val errorMsg = response.error?.errorMessage ?: "Unknown Graph API Error"
                onResult(Result.failure(Exception(errorMsg)))
                return@newMeRequest
            }

            if (jsonObject == null) {
                onResult(Result.failure(Exception("Facebook Graph API returned empty/null payload.")))
                return@newMeRequest
            }

            try {
                // Parsing response parameters
                val id = jsonObject.getString("id")
                val name = jsonObject.getString("name")
                
                // Email permission can be denied, handle missing key gracefully
                val email = if (jsonObject.has("email")) jsonObject.getString("email") else null
                
                // Profile picture structure in Graph API: picture -> data -> url
                var profilePictureUrl: String? = null
                if (jsonObject.has("picture")) {
                    val pictureObj = jsonObject.getJSONObject("picture")
                    if (pictureObj.has("data")) {
                        val dataObj = pictureObj.getJSONObject("data")
                        profilePictureUrl = dataObj.getString("url")
                    }
                }

                // Public profile link (might require additional review depending on API version, handle gracefully)
                val link = if (jsonObject.has("link")) jsonObject.getString("link") else null

                val profile = UserProfile(
                    id = id,
                    name = name,
                    email = email,
                    profilePictureUrl = profilePictureUrl,
                    profileLink = link,
                    memberSince = "June 2026", // Default date representation for new log-ins
                    appPostsCount = getLocalProfile()?.appPostsCount ?: 0
                )
                
                // Cache user details locally
                saveLocalProfile(profile)
                onResult(Result.success(profile))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }

        // Configure standard parameters requesting specific user fields
        val parameters = Bundle().apply {
            putString("fields", "id,name,email,picture.type(large),link")
        }
        request.parameters = parameters
        request.executeAsync()
    }

    override fun getLocalProfile(): UserProfile? {
        return sessionManager.getUserProfile()
    }

    override fun saveLocalProfile(profile: UserProfile) {
        sessionManager.saveUserProfile(profile)
    }

    override fun clearSession() {
        sessionManager.clearSession()
    }

    override fun incrementLocalPostCount() {
        sessionManager.incrementPostCount()
    }
}
