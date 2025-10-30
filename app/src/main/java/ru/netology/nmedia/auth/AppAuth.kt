package ru.netology.nmedia.auth

import android.content.Context
import kotlinx.coroutines.flow.*
import ru.netology.nmedia.dto.AuthState
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.dto.PushToken
import java.io.File

class AppAuth (context: Context) {
    companion object {
        private const val ID_KEY = "id"
        private const val TOKEN_KEY = "token"

//        @Volatile
//        private var instance: AppAuth? = null
//
//        fun getInstance(): AppAuth = synchronized(this) {
//            instance ?: throw IllegalStateException(
//                "AppAuth is not initialized, you must call AppAuth.initializeApp(Context context) first."
//            )
//        }
//
//        fun initApp(context: Context): AppAuth = synchronized(this) {
//            instance ?: buildAuth(context).also { instance = it }
//        }
//
//        private fun buildAuth(context: Context): AppAuth = AppAuth(context)

    }

    suspend fun sendLoginPassword(username: String, password: String) {
            try {
                val authState =
                    DependencyContainer.getInstance().apiService.updateUser(username, password)
                setAuth(authState.id, authState.token)
            } catch (_: Exception) {
                setAuth(0L, null)
                throw RuntimeException("Ошибка авторизации")
            }
        }

    suspend fun sendRegistration(nickname: String, login: String, password: String) {
            try {
                val authState = DependencyContainer.getInstance().apiService.registerUser(
                    login,
                    password,
                    nickname
                )
                setAuth(authState.id, authState.token)
            } catch (_: Exception) {
                setAuth(0L, null)
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
                val authState = DependencyContainer.getInstance().apiService.registerWithPhoto(
                    login = login.toRequestBody("text/plain".toMediaType()),
                    pass = password.toRequestBody("text/plain".toMediaType()),
                    name = nickname.toRequestBody("text/plain".toMediaType()),
                    media = MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody()
                    )
                )
                setAuth(authState.id, authState.token)
            } catch (_: Exception) {
                setAuth(0L, null)
                throw RuntimeException("Ошибка регистрации")
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
        sendPushToken()
    }

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String?) {
        _authStateFlow.value = AuthState(id, token)
        prefs.edit {
            putLong(ID_KEY, id)
            putString(TOKEN_KEY, token)
        }
        sendPushToken()
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        prefs.edit { clear() }
        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pushToken = PushToken(token ?: Firebase.messaging.token.await())
                DependencyContainer.getInstance().apiService.sendPushToken(pushToken)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}