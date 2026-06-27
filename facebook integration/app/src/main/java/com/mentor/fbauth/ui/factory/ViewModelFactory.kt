package com.mentor.fbauth.ui.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mentor.fbauth.data.local.SessionManager
import com.mentor.fbauth.data.repository.ProfileRepositoryImpl
import com.mentor.fbauth.ui.login.LoginViewModel
import com.mentor.fbauth.ui.profile.ProfileViewModel
import com.mentor.fbauth.ui.share.ShareViewModel

/**
 * Standard Android ViewModelProvider.Factory to inject dependencies without complex frameworks,
 * keeping the codebase lightweight and focused on core principles.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sessionManager = SessionManager(context.applicationContext)
        val repository = ProfileRepositoryImpl(sessionManager)
        
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ShareViewModel::class.java) -> {
                ShareViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
