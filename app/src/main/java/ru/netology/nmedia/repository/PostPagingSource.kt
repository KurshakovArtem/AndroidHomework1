package ru.netology.nmedia.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.toDto

class PostPagingSource(
    private val dao: PostDao
) : PagingSource<Long, Post>() {
    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return null
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            val result = when (params) {
                is LoadParams.Append -> dao.getBefore(
                    id = params.key,
                    count = params.loadSize
                ).toDto()

                is LoadParams.Prepend -> return LoadResult.Page(
                    data = emptyList(),
                    prevKey = params.key,
                    nextKey = null
                )

                is LoadParams.Refresh -> dao.getLatest(params.loadSize).toDto()
            }

            val nextKey = if (result.isEmpty()) null else result.last().id

            return LoadResult.Page(data = result, prevKey = params.key, nextKey = nextKey)

        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            return LoadResult.Error(e)
        }
    }

}