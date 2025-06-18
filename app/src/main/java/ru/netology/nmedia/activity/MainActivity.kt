package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener

import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.supportingFunctions.AndroidUtils
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()

        val adapter = PostAdapter(
            object : OnInteractionListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
                }

                override fun onShare(post: Post) {
                    viewModel.shareById(post.id)
                    val intent = Intent().apply{
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.content)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                    startActivity(shareIntent)
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                }
            }
        )

        binding.list.adapter = adapter

        viewModel.data.observe(this) { posts ->
            //val isNew = posts.size != adapter.itemCount
            adapter.submitList(posts)
//            {                                    //для скрола экрана вверх при создании поста
//                if (isNew) {                     //скролит экран при повороте экрана
//                    binding.list.smoothScrollToPosition(0)
//                }
//            }
        }

        viewModel.edited.observe(this) { post ->
            if (post.id != 0L) {
                binding.editGroup.visibility = View.VISIBLE
                binding.oldEdit.text = post.content
                with(binding.contentText) {
                    setText(post.content)
                    AndroidUtils.showKeyboard(this)
                }
            } else {
                binding.oldEdit.text = ""
                binding.editGroup.visibility = View.GONE
            }
        }

        binding.save.setOnClickListener {
            with(binding.contentText) {
                if (text.isNullOrBlank()) {
                    Toast.makeText(
                        this@MainActivity,
                        context.getString(R.string.error_empty_content),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                viewModel.save(text.toString())

                setText("")
                clearFocus()
                AndroidUtils.hideKeyboard(it)
            }
        }

        binding.closeButton.setOnClickListener {
            with(binding.contentText) {
                viewModel.cancelEdit()
                setText("")
                clearFocus()
                AndroidUtils.hideKeyboard(it)
            }
        }
    }
}
