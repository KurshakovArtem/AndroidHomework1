package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.LoadStateBinding

interface StateOnInteractionListener {
    fun onRetry() {}
}

class PagingLoadStateAdapter(
    private val stateOnInteractionListener: StateOnInteractionListener,
) : LoadStateAdapter<LoadStateViewHolder>() {
    override fun onBindViewHolder(
        holder: LoadStateViewHolder,
        loadState: LoadState
    ) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): LoadStateViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return LoadStateViewHolder(
            LoadStateBinding.inflate(layoutInflater, parent, false),
            stateOnInteractionListener
        )
    }
}

class LoadStateViewHolder(
    private val binding: LoadStateBinding,
    private val stateOnInteractionListener: StateOnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(loadState: LoadState) {
        binding.apply {
            progressState.isVisible = loadState is LoadState.Loading
            retryState.isVisible = loadState is LoadState.Error
            retryState.setOnClickListener {
                stateOnInteractionListener.onRetry()
            }
        }
    }
}