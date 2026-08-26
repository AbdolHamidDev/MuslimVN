package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
}
