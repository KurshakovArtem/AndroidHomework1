package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.supportingFunctions.converterNumToString

typealias OnActionListener = (post: Post) -> Unit

class PostAdapter(
    private val onLikeListener: OnActionListener,
    private val onShareListener: OnActionListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onLikeListener, onShareListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onLikeListener: OnActionListener,
    private val onShareListener: OnActionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) = with(binding) {
        author.text = post.author
        published.text = post.published
        content.text = post.content
        valueLike.text = converterNumToString(post.likes)
        valueShare.text = converterNumToString(post.share)
        valuePostViews.text = converterNumToString(post.postViews)
        likeButton.setImageResource(
            if (post.likedByMe) {
                valueLike.text = converterNumToString(post.likes)
                R.drawable.ic_liked_24
            } else {
                valueLike.text = converterNumToString(post.likes)
                R.drawable.ic_like_24
            }
        )
        likeButton.setOnClickListener {
            onLikeListener(post)
        }
        shareButton.setOnClickListener {
            onShareListener(post)
        }
    }
}

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

}
