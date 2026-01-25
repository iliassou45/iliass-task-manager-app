package com.iliass.iliass.util

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Singleton manager for Supabase client initialization and authentication
 */
object SupabaseManager {

    private const val SUPABASE_URL = "https://hbueamhydnpdeulaqlqy.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhidWVhbWh5ZG5wZGV1bGFxbHF5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYwMDE4NTQsImV4cCI6MjA2MTU3Nzg1NH0.Fal7Ek5LmGGdncbhDnQx-5yc9RVEgOoxJ_CAy78wwQo"
    const val BUCKET_NAME = "android_uploads"

    private const val PREFS_NAME = "supabase_prefs"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val KEY_USER_EMAIL = "user_email"

    @Volatile
    private var supabaseClient: SupabaseClient? = null

    /**
     * Get or create the Supabase client instance
     */
    fun getClient(): SupabaseClient {
        return supabaseClient ?: synchronized(this) {
            supabaseClient ?: createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_KEY
            ) {
                install(Auth)
                install(Storage)
            }.also { supabaseClient = it }
        }
    }

    /**
     * Sign up a new user
     */
    suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            getClient().auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success("Account created successfully. Please check your email for verification.")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    /**
     * Sign in an existing user
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            getClient().auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success("Signed in successfully")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): AuthResult {
        return try {
            getClient().auth.signOut()
            AuthResult.Success("Signed out successfully")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign out failed")
        }
    }

    /**
     * Get the current user info
     */
    fun getCurrentUser(): UserInfo? {
        return try {
            getClient().auth.currentUserOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Get the current user's ID
     */
    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id
    }

    /**
     * Get the current user's email
     */
    fun getCurrentUserEmail(): String? {
        return getCurrentUser()?.email
    }

    /**
     * Save last sync timestamp
     */
    fun saveLastSyncTimestamp(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }

    /**
     * Save user email for display purposes
     */
    fun saveUserEmail(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Get saved user email
     */
    fun getSavedUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Clear saved credentials
     */
    fun clearSavedData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Get storage bucket reference
     */
    fun getStorageBucket() = getClient().storage.from(BUCKET_NAME)

    sealed class AuthResult {
        data class Success(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}
