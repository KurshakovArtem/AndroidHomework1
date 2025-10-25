package ru.netology.nmedia.viewmodel

import android.net.Uri
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
import ru.netology.nmedia.model.PhotoModel
import java.io.File

class AuthViewModel : ViewModel() {
    val data: LiveData<AuthState> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    private val _dataAuthState = MutableLiveData<AuthModelState>()

    val dataState: LiveData<AuthModelState>
        get() = _dataAuthState
    val isAuthorized: Boolean
        get() = AppAuth.getInstance().authStateFlow.value.id != 0L

    private val _photo = MutableLiveData<PhotoModel?>()
    val photo: LiveData<PhotoModel?>
        get() = _photo

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

    fun signUp(nickname: String, login: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _dataAuthState.value = AuthModelState(error = true)
        } else {
            viewModelScope.launch {
                try {
                    _dataAuthState.value = AuthModelState(loading = true)
                    if (_photo.value == null) {
                        AppAuth.sendRegistration(nickname, login, password)
                        _dataAuthState.value = AuthModelState(success = true)
                    } else {
                        AppAuth.sendRegistrationWithPhoto(
                            nickname,
                            login,
                            password,
                            _photo.value?.file ?: return@launch
                        )
                        removePhoto()
                        _dataAuthState.value = AuthModelState(success = true)
                    }
                } catch (_: RuntimeException) {
                    _dataAuthState.value = AuthModelState(error = true)
                }
            }
        }
    }

    fun clearState() {
        _dataAuthState.value = AuthModelState()
    }

    fun updatePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

}