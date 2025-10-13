package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ErrorReport
import ru.netology.nmedia.model.FeedErrorMassage
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.supportingFunctions.SingleLiveEvent


private val empty = Post(
    id = 0,
    author = "",
    published = "",
    content = "",
    likes = 0,
    likedByMe = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryNetworkImpl(
        AppDb.getInstance(application).postDao
    )


    val data: LiveData<FeedModel> = repository.data
        .map(::FeedModel)
        .catch { it.printStackTrace() }  // реализовать обработку ошибок(snackbar)
        .asLiveData(Dispatchers.Default)
    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.posts.firstOrNull { post -> post.id > 0 }?.id ?: 0L)
            .catch { e -> e.printStackTrace() } // Не сообщаем пользователю об ошибке в фоне
            .asLiveData(Dispatchers.Default)
    }
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _dataState.value = FeedModelState(loading = true)
            try {
                repository.getAllAsync()
                _dataState.value = FeedModelState()
            } catch (_: RuntimeException) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _dataState.value = FeedModelState(refreshing = true)
            try {
                repository.getAllAsync()
                _dataState.value = FeedModelState()
            } catch (_: RuntimeException) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun save(content: String) {
        edited.value?.let { editPost ->
            val text = content.trim()       //отсекает все пробелы в начале и конце
            _postCreated.value = Unit
            if (text != editPost.content) {
                viewModelScope.launch {
                    try {
                        val savedPost = repository.saveAsync(editPost.copy(content = content))
                        if (savedPost.syncServerState) {
                            _dataState.value = FeedModelState(errorReport = null)
                        } else {
                            _dataState.value = FeedModelState(
                                errorReport = ErrorReport(
                                    savedPost.id,
                                    FeedErrorMassage.SAVE_ERROR
                                )
                            )
                        }
                    } catch (_: RuntimeException) {
                        _dataState.value = FeedModelState(
                            errorReport = ErrorReport(
                                edited.value?.id ?: 0,
                                FeedErrorMassage.SAVE_ERROR
                            )
                        )
                    }
                }
            }
        }
        if (edited.value?.id == 0L) repository.setDraft("")  // очищаем черновик
        edited.value = empty
    }

    fun saveRefresh(post: Post) {
        viewModelScope.launch {
            try {
                val refreshingPost = repository.retrySaveAsync(post)
                if (refreshingPost.syncServerState) {
                    _dataState.value = FeedModelState(errorReport = null)
                } else {
                    _dataState.value = FeedModelState(
                        errorReport = ErrorReport(
                            refreshingPost.id,
                            FeedErrorMassage.SAVE_REFRESH_ERROR
                        )
                    )
                }
            } catch (_: RuntimeException) {
                _dataState.value = FeedModelState(
                    errorReport = ErrorReport(
                        post.id,
                        FeedErrorMassage.SAVE_REFRESH_ERROR
                    )
                )
            }
        }
    }

    fun likeById(id: Long) {
        val isLiked = data.value?.posts?.find { it.id == id }?.likedByMe ?: return
        viewModelScope.launch {
            try {
                repository.likeByIdAsync(id)
                _dataState.value = FeedModelState(errorReport = null)
            } catch (_: RuntimeException) {
                if (!isLiked) {
                    _dataState.value = FeedModelState(
                        errorReport = ErrorReport(
                            id,
                            FeedErrorMassage.LIKE_ERROR
                        )
                    )
                } else {
                    _dataState.value = FeedModelState(
                        errorReport = ErrorReport(
                            id,
                            FeedErrorMassage.DISLIKE_ERROR
                        )
                    )
                }
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeBiIdAsync(id)
                _dataState.value = FeedModelState(errorReport = null)
            } catch (_: RuntimeException) {
                _dataState.value = FeedModelState(
                    errorReport = ErrorReport(
                        id,
                        FeedErrorMassage.REMOVE_ERROR
                    )
                )
            }
        }
    }

    fun shareById(id: Long) = repository.shareById(id)

    fun edit(post: Post) {
        edited.value = post
    }

    fun cancelEdit() {
        edited.value = empty
    }

    fun createDraft(content: String) {
        if (edited.value?.id != 0L) {
            cancelEdit()
        } else {
            repository.setDraft(content)
        }
    }

    fun getDraft() = repository.getDraft()
}
