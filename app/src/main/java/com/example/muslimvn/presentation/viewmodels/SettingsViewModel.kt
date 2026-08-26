package com.example.muslimvn.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.UserData
import com.example.muslimvn.domain.repository.AuthRepository
import com.example.muslimvn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = repository.getAppTheme()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.FOLLOW_SYSTEM
        )

    val currentUser: StateFlow<UserData?> = authRepository.currentUser

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState = _authUiState.asStateFlow()

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateAppTheme(theme)
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = authRepository.signInWithGoogle(context)
            result.fold(
                onSuccess = {
                    _authUiState.value = AuthUiState(isLoading = false)
                },
                onFailure = { error ->
                    _authUiState.value = AuthUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Đăng nhập thất bại"
                    )
                }
            )
        }
    }

    fun clearError() {
        _authUiState.value = _authUiState.value.copy(errorMessage = null)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
