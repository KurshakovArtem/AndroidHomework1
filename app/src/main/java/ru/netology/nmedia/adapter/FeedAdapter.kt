package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardDateBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Date
import ru.netology.nmedia.dto.FeedItem
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
    fun onSaveRefresh(post: Post) {}
    fun onMoveToSinglePhoto(post: Post) {}
    fun onAdClick(ad: Ad) {}
}

class FeedAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback) {
    private val typeAd = 0
    private val typePost = 1
    private val typeDate = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Ad -> typeAd
            is Post -> typePost
            is Date -> typeDate
            null -> throw IllegalArgumentException("unknow item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typePost -> {
                PostViewHolder(
                    CardPostBinding.inflate(layoutInflater, parent, false),
                    onInteractionListener
                )
            }

            typeAd -> {
                AdViewHolder(
                    CardAdBinding.inflate(layoutInflater, parent, false),
                    onInteractionListener
                )
            }

            typeDate -> {
                DateViewHolder(
                    CardDateBinding.inflate(layoutInflater, parent, false)
                )
            }

            else -> throw IllegalArgumentException("unknow view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            is Date -> (holder as? DateViewHolder)?.bind(item)
            null -> throw IllegalArgumentException("unknow item type")
        }
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) = with(binding) {
        author.text = post.author
        published.text = post.published.toString()
        content.text = post.content
        shareButton.text = converterNumToString(post.share)
        valuePostViews.text = converterNumToString(post.postViews)
        if (post.authorAvatar.isBlank()) {
            avatar.setImageResource(R.drawable.ic_empty_avatar_24)
        } else avatar.loadAvatar("http://10.0.2.2:9999/avatars/${post.authorAvatar}")

        if (post.syncServerState) {
            saveRefresh.isVisible = false
            likeButton.apply {
                isChecked = post.likedByMe
                text = converterNumToString(post.likes)
                isVisible = true
            }
            shareButton.isVisible = true
            likeButton.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            shareButton.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        } else {
            likeButton.isVisible = false
            shareButton.isVisible = false
            saveRefresh.isVisible = true

            saveRefresh.setOnClickListener {
                onInteractionListener.onSaveRefresh(post)
            }
        }
        cardPost.setOnClickListener {
            onInteractionListener.onMoveToSinglePost(post)
        }
        menu.isVisible = post.ownedByMe
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
            attachmentGroup.isVisible = false
        } else {
            attachmentGroup.isVisible = true
            when (post.attachment.type) {
                AttachmentType.IMAGE -> {
                    attachmentText.text = post.attachment.description
                    attachmentPost.loadAttachmentImage("http://10.0.2.2:9999/media/${post.attachment.url}")
                    attachmentPost.setOnClickListener {
                        onInteractionListener.onMoveToSinglePhoto(post)
                    }
                }

                AttachmentType.VIDEO -> {
                    attachmentText.text = post.attachment.description
                    attachmentPost.setImageResource(R.drawable.video_not_found)
                    attachmentGroup.setOnClickListener {
                        onInteractionListener.onVideo(post)
                    }
                }

                AttachmentType.EMPTY -> {
                    attachmentGroup.isVisible = false
                }
            }
        }
    }
}

class AdViewHolder(
    private val binding: CardAdBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(ad: Ad) {
        binding.apply {
            cardImage.loadAttachmentImage("http://10.0.2.2:9999/media/${ad.image}")
            cardImage.setOnClickListener {
                onInteractionListener.onAdClick(ad)
            }
        }
    }
}

class DateViewHolder(
    private val binding: CardDateBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(date: Date) {
        binding.dateText.text = date.title
    }
}

object FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}
