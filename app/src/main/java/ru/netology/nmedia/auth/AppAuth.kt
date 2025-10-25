package ru.netology.nmedia.auth

import android.content.Context
import kotlinx.coroutines.flow.*
import ru.netology.nmedia.dto.AuthState
import androidx.core.content.edit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.PostApi
import java.io.File

class AppAuth private constructor(context: Context) {
    companion object {
        private const val ID_KEY = "id"
        private const val TOKEN_KEY = "token"

        @Volatile
        private var instance: AppAuth? = null

        fun getInstance(): AppAuth = synchronized(this) {
            instance ?: throw IllegalStateException(
                "AppAuth is not initialized, you must call AppAuth.initializeApp(Context context) first."
            )
        }

        fun initApp(context: Context): AppAuth = synchronized(this) {
            instance ?: buildAuth(context).also { instance = it }
        }

        private fun buildAuth(context: Context): AppAuth = AppAuth(context)

        suspend fun sendLoginPassword(username: String, password: String) {
            try {
                val authState = PostApi.service.updateUser(username, password)
                instance?.setAuth(authState.id, authState.token)
            } catch (_: Exception) {
                instance?.setAuth(0L, null)
                throw RuntimeException("Ошибка авторизации")
            }
        }

        suspend fun sendRegistration(nickname: String, login: String, password: String) {
            try {
                val authState = PostApi.service.registerUser(login, password, nickname)
                instance?.setAuth(authState.id, authState.token)
            } catch (_: Exception) {
                instance?.setAuth(0L, null)
                throw RuntimeException("Ошибка регистрации")
            }
        }

        suspend fun sendRegistrationWithPhoto(
            nickname: String,
            login: String,
            password: String,
            file: File
        ) {
            try {
                val authState = PostApi.service.registerWithPhoto(
                    login = login.toRequestBody("text/plain".toMediaType()),
                    pass = password.toRequestBody("text/plain".toMediaType()),
                    name = nickname.toRequestBody("text/plain".toMediaType()),
                    media = MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody()
                    )
                )
                instance?.setAuth(authState.id, authState.token)
            }catch (_: Exception) {
                instance?.setAuth(0L, null)
                throw RuntimeException("Ошибка регистрации")
            }
        }
    }

    private val prefs =
        context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow: MutableStateFlow<AuthState>

    init {
        val id = prefs.getLong(ID_KEY, 0)
        val token = prefs.getString(TOKEN_KEY, null)
        if (id == 0L || token == null) {
            _authStateFlow = MutableStateFlow(AuthState())
            prefs.edit { clear() }
        } else {
            _authStateFlow = MutableStateFlow(AuthState(id, token))
        }
    }

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String?) {
        _authStateFlow.value = AuthState(id, token)
        prefs.edit {
            putLong(ID_KEY, id)
            putString(TOKEN_KEY, token)
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        prefs.edit { clear() }
    }


}