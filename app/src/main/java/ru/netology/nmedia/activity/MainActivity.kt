package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.supportingFunctions.converterNumToString
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()
        viewModel.data.observe(this) { posts ->
            binding.container.removeAllViews()
            posts.map { post ->
                CardPostBinding.inflate(layoutInflater, binding.container, true).apply {

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
                        viewModel.like(post.id)
                    }
                    shareButton.setOnClickListener {
                        viewModel.share(post.id)
                    }
                }.root
            }
        }
    }
}
