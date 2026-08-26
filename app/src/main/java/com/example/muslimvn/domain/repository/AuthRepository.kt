package com.example.muslimvn.domain.repository

import android.content.Context
import com.example.muslimvn.domain.models.UserData
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserData?>
    
    suspend fun signInWithGoogle(context: Context): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
