package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ErrorReport
import ru.netology.nmedia.model.FeedErrorMassage
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.supportingFunctions.SingleLiveEvent
import java.io.File
import javax.inject.Inject


private val empty = Post(
    id = 0,
    authorId = 0,
    author = "",
    published = "",
    content = "",
    likes = 0,
    likedByMe = false
)


@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    appAuth: AppAuth,
) : ViewModel() {

    private val cached = repository.data.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Post>> = appAuth
        .authStateFlow
        .flatMapLatest { (myId, _) ->
            cached.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }


//        val newerCount: LiveData<Int> = data.switchMap {
//        repository.getNewerCount()
//            .catch { e -> e.printStackTrace() } // Не сообщаем пользователю об ошибке в фоне
//            .asLiveData(Dispatchers.Default)
//    }
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    private val _photo = MutableLiveData<PhotoModel?>()
    val photo: LiveData<PhotoModel?>
        get() = _photo

    init {
        loadPosts()
    }

    fun updatePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
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

    fun updateNewerToOld() {
        viewModelScope.launch {
            try {
                repository.updateNewerToOld()
            } catch (_: Exception) {
                println("ошибка БД")
            }
        }
    }

    suspend fun getPostById(id: Long): Post? {
        return repository.getPostById(id)
    }

    fun save(content: String) {
        edited.value?.let { editPost ->
            val text = content.trim()       //отсекает все пробелы в начале и конце
            _postCreated.value = Unit
            if (text != editPost.content) {
                viewModelScope.launch {
                    try {

                        val savedPost = repository.saveAsync(
                            editPost.copy(content = content),
                            _photo.value?.file
                        )
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
        _photo.value = null
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

    fun likeById(post: Post) {
        val isLiked = post.likedByMe
        viewModelScope.launch {
            try {
                repository.likeByIdAsync(post.id)
                _dataState.value = FeedModelState(errorReport = null)
            } catch (_: RuntimeException) {
                if (!isLiked) {
                    _dataState.value = FeedModelState(
                        errorReport = ErrorReport(
                            post.id,
                            FeedErrorMassage.LIKE_ERROR
                        )
                    )
                } else {
                    _dataState.value = FeedModelState(
                        errorReport = ErrorReport(
                            post.id,
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

    fun removePhoto() {
        _photo.value = null
    }
}
