package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val postDao: PostDao,
    private val service: PostApiService,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val db: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> service.getLatest(state.config.initialLoadSize)

                LoadType.PREPEND -> {
                    val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    service.getAfter(id, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    service.getBefore(id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body()?.map {
                it.copy(syncServerState = true, isVisible = true)
            } ?: throw ApiError(
                response.code(),
                response.message()
            )

            if (body.isEmpty()) {
                return MediatorResult.Success(
                    endOfPaginationReached = true
                )
            }

            db.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        postRemoteKeyDao.removeAll()
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id
                                ),
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id
                                )
                            )
                        )
                        postDao.removeAll()
                    }

                    LoadType.PREPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.AFTER,
                                id = body.first().id
                            )
                        )
                    }

                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                id = body.last().id
                            )
                        )
                    }
                }
                postDao.insert(body.toEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            return MediatorResult.Error(e)
        }
    }
}

//    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
//        return null
//    }
//
//    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
//        try {
//            val result = when (params) {
//                is LoadParams.Append -> dao.getBefore(
//                    id = params.key,
//                    count = params.loadSize
//                ).toDto()
//
//                is LoadParams.Prepend -> return LoadResult.Page(
//                    data = emptyList(),
//                    prevKey = params.key,
//                    nextKey = null
//                )
//
//                is LoadParams.Refresh -> dao.getLatest(params.loadSize).toDto()
//            }
//
//            val nextKey = if (result.isEmpty()) null else result.last().id
//
//            return LoadResult.Page(data = result, prevKey = params.key, nextKey = nextKey)
//
//        } catch (e: Exception) {
//            if (e is CancellationException) {
//                throw e
//            }
//
//            return LoadResult.Error(e)
//        }
//    }