package com.mentor.fbauth.data.model

/**
 * Models the profile details fetched from Facebook Graph API.
 * Includes user metadata, fallback states for email denial,
 * and a counter for wall posts shared through our app.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String?,
    val profilePictureUrl: String?,
    val profileLink: String?,
    val memberSince: String = "June 2026",
    val appPostsCount: Int = 0
)
