package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.UserProfile
import com.example.muslimvn.domain.repository.AuthRepository
import com.example.muslimvn.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                userRepository.getUserProfile(user.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        checkAndCreateDefaultProfile()
    }

    private fun checkAndCreateDefaultProfile() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value ?: return@launch
            val existingProfile = userRepository.getUserProfile(user.uid).first()
            
            if (existingProfile == null) {
                val newProfile = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "Người dùng",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl ?: ""
                )
                userRepository.saveUserProfile(newProfile)
            }
        }
    }

    fun onDisplayNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(displayName = newName)
        )
    }

    fun onBioChange(newBio: String) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(bio = newBio)
        )
    }

    fun onMadhabChange(newMadhab: String) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(madhab = newMadhab)
        )
    }

    fun onLocationChange(newLocation: String) {
        _uiState.value = _uiState.value.copy(
            userProfile = _uiState.value.userProfile.copy(location = newLocation)
        )
    }

    fun setInitialProfile(profile: UserProfile) {
        if (_uiState.value.userProfile.uid.isEmpty()) {
            _uiState.value = _uiState.value.copy(userProfile = profile)
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val currentProfile = _uiState.value.userProfile

            // Save profile data
            val result = userRepository.saveUserProfile(currentProfile)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Lỗi khi lưu hồ sơ"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetSavedState() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}
