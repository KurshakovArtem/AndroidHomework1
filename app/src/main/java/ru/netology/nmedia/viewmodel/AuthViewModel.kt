package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.AuthState
import ru.netology.nmedia.model.AuthModelState

class AuthViewModel : ViewModel() {
    val data: LiveData<AuthState> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    private val _dataAuthState = MutableLiveData<AuthModelState>()

    val dataState: LiveData<AuthModelState>
        get() = _dataAuthState
    val isAuthorized: Boolean
        get() = AppAuth.getInstance().authStateFlow.value.id != 0L

    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            try {
                _dataAuthState.value = AuthModelState(loading = true)
                AppAuth.sendLoginPassword(username, password)
                _dataAuthState.value = AuthModelState(success = true)
                clearState()
            } catch (_: RuntimeException) {
                _dataAuthState.value = AuthModelState(error = true)
            }
        }
    }
    fun clearState(){
        _dataAuthState.value = AuthModelState()
    }

}