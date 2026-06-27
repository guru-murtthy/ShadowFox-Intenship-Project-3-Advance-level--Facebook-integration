package com.mentor.fbauth.domain

import com.mentor.fbauth.data.model.UserProfile

/**
 * Domain layer repository interface outlining Facebook authentication and session data operations.
 */
interface ProfileRepository {
    fun getSessionToken(): String?
    fun saveSessionToken(token: String)
    fun fetchUserProfile(token: String, onResult: (Result<UserProfile>) -> Unit)
    fun getLocalProfile(): UserProfile?
    fun saveLocalProfile(profile: UserProfile)
    fun clearSession()
    fun incrementLocalPostCount()
}
