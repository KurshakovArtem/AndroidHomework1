package ru.netology.nmedia.auth

import android.content.Context
import kotlinx.coroutines.flow.*
import ru.netology.nmedia.dto.AuthState
import androidx.core.content.edit

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
    fun setAuth(id: Long, token: String) {
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