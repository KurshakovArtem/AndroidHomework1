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
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.supportingFunctions.converterNumToString
import ru.netology.nmedia.supportingFunctions.loadAttachmentImage
import ru.netology.nmedia.supportingFunctions.loadAvatar


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
        if (post.authorAvatar.isBlank()) {
            avatar.setImageResource(R.drawable.ic_empty_avatar_24)
        } else avatar.loadAvatar("http://10.0.2.2:9999/avatars/${post.authorAvatar}")

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
        if (post.attachment == null) {
            attachmentGroup.visibility = View.GONE
        } else {
            attachmentGroup.visibility = View.VISIBLE
            when (post.attachment.type) {
                AttachmentType.IMAGE -> {
                    attachmentText.text = post.attachment.description
                    attachmentPost.loadAttachmentImage("http://10.0.2.2:9999/images/${post.attachment.url}")
                }

                AttachmentType.VIDEO -> {
                    attachmentText.text = post.attachment.description
                    attachmentPost.setImageResource(R.drawable.video_not_found)
                    attachmentGroup.setOnClickListener {
                        onInteractionListener.onVideo(post)
                    }
                }

                AttachmentType.EMPTY -> {
                    attachmentGroup.visibility = View.GONE
                }
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
