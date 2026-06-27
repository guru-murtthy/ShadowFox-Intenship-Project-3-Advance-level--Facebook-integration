package com.mentor.fbauth.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mentor.fbauth.data.model.UserProfile

/**
 * SessionManager handles the persistence of the Facebook OAuth tokens and profile info.
 * It encapsulates EncryptedSharedPreferences to store all fields securely on disk,
 * preventing extraction via simple backup or rooted-device inspection.
 */
class SessionManager(context: Context) {

    private val sharedPrefs: SharedPreferences

    init {
        // Build the MasterKey using standard AES256 GCM key scheme.
        // On Android 6.0+, this is backed by Android Keystore System.
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences to encrypt keys & values on disk.
        sharedPrefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Store the active short-lived or long-lived Facebook OAuth access token.
     */
    fun saveAccessToken(token: String) {
        sharedPrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    /**
     * Retrieve the stored access token, returning null if unauthenticated.
     */
    fun getAccessToken(): String? {
        return sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Persist the user profile details fetched via Facebook Graph API.
     */
    fun saveUserProfile(profile: UserProfile) {
        sharedPrefs.edit().apply {
            putString(KEY_USER_ID, profile.id)
            putString(KEY_USER_NAME, profile.name)
            putString(KEY_USER_EMAIL, profile.email)
            putString(KEY_USER_PIC, profile.profilePictureUrl)
            putString(KEY_USER_LINK, profile.profileLink)
            putString(KEY_MEMBER_SINCE, profile.memberSince)
            putInt(KEY_POSTS_COUNT, profile.appPostsCount)
            apply()
        }
    }

    /**
     * Load the user profile details from encrypted preferences.
     */
    fun getUserProfile(): UserProfile? {
        val id = sharedPrefs.getString(KEY_USER_ID, null) ?: return null
        val name = sharedPrefs.getString(KEY_USER_NAME, "") ?: ""
        val email = sharedPrefs.getString(KEY_USER_EMAIL, null)
        val pic = sharedPrefs.getString(KEY_USER_PIC, null)
        val link = sharedPrefs.getString(KEY_USER_LINK, null)
        val memberSince = sharedPrefs.getString(KEY_MEMBER_SINCE, "June 2026") ?: "June 2026"
        val postsCount = sharedPrefs.getInt(KEY_POSTS_COUNT, 0)
        return UserProfile(id, name, email, pic, link, memberSince, postsCount)
    }

    /**
     * Increment internal wall posts count metric safely.
     */
    fun incrementPostCount() {
        val currentCount = sharedPrefs.getInt(KEY_POSTS_COUNT, 0)
        sharedPrefs.edit().putInt(KEY_POSTS_COUNT, currentCount + 1).apply()
    }

    /**
     * Completely clear all user credentials and profile state.
     */
    fun clearSession() {
        sharedPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILE_NAME = "secure_fb_session"
        private const val KEY_ACCESS_TOKEN = "fb_access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PIC = "user_pic"
        private const val KEY_USER_LINK = "user_link"
        private const val KEY_MEMBER_SINCE = "member_since"
        private const val KEY_POSTS_COUNT = "posts_count"
    }
}
