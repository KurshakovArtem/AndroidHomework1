package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.supportingFunctions.converterNumToString


interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onVideo(post: Post) {}
    fun onMoveToSinglePost(post: Post) {}
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) = with(binding) {
        author.text = post.author
        published.text = post.published
        content.text = post.content
        shareButton.text = converterNumToString(post.share)
        valuePostViews.text = converterNumToString(post.postViews)

        likeButton.apply {
            isChecked = post.likedByMe
            text = converterNumToString(post.likes)
        }

        likeButton.setOnClickListener {
            onInteractionListener.onLike(post)
        }
        shareButton.setOnClickListener {
            onInteractionListener.onShare(post)
        }
        cardPost.setOnClickListener {
            onInteractionListener.onMoveToSinglePost(post)
        }
        menu.setOnClickListener {
            PopupMenu(it.context, it).apply {
                inflate(R.menu.option_post)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.remove -> {
                            onInteractionListener.onRemove(post)
                            true
                        }

                        R.id.edit -> {
                            onInteractionListener.onEdit(post)
                            true
                        }

                        else -> false
                    }
                }
            }.show()
        }

        if (post.videoUrl.isNullOrBlank()) {
            videoGroup.visibility = View.GONE
        } else {
            videoGroup.visibility = View.VISIBLE
            urlText.text = post.videoUrl
            videoGroup.setOnClickListener {
                onInteractionListener.onVideo(post)
            }
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
