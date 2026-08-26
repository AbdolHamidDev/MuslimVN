package com.example.muslimvn.data.repository

import android.util.Log
import com.example.muslimvn.domain.models.UserProfile
import com.example.muslimvn.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")
    private val TAG = "UserRepository"

    override fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            Log.d(TAG, "getUserProfile: UID is empty")
            trySend(null)
            close()
            return@callbackFlow
        }
        
        Log.d(TAG, "getUserProfile: Subscribing to Firestore for UID: $uid")
        val subscription = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "getUserProfile: Firestore error", error)
                close(error)
                return@addSnapshotListener
            }
            val profile = snapshot?.toObject(UserProfile::class.java)
            Log.d(TAG, "getUserProfile: Received profile: $profile")
            trySend(profile)
        }
        awaitClose { 
            Log.d(TAG, "getUserProfile: Closing subscription for UID: $uid")
            subscription.remove() 
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        if (profile.uid.isEmpty()) {
            Log.e(TAG, "saveUserProfile: UID is empty")
            return@withContext Result.failure(Exception("UID is empty"))
        }
        
        return@withContext try {
            Log.d(TAG, "saveUserProfile: Initiating save for UID: ${profile.uid}")
            usersCollection.document(profile.uid).set(profile)
            Log.d(TAG, "saveUserProfile: Request sent to Firestore SDK cache")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveUserProfile: Save failed", e)
            Result.failure(e)
        }
    }
}
