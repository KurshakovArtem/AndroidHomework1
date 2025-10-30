package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.model.FeedErrorMassage
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.viewmodel.ViewModelFactory

class FeedFragment : Fragment() {

    private val dependencyContainer = DependencyContainer.getInstance()
    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
        factoryProducer = {
            ViewModelFactory(
                dependencyContainer.repository,
                dependencyContainer.appAuth
            )
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentFeedBinding.inflate(
            inflater,
            container,
            false
        )


        val adapter = PostAdapter(
            object : OnInteractionListener {
                override fun onLike(post: Post) {
                    if (dependencyContainer.appAuth.authStateFlow.value.id != 0L) {
                        viewModel.likeById(post.id)
                    } else {
                        showLoginDialog()
                    }
                }

                override fun onSaveRefresh(post: Post) {
                    viewModel.saveRefresh(post)
                }

                override fun onShare(post: Post) {
                    viewModel.shareById(post.id)
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.content)
                        type = "text/plain"
                    }
                    val shareIntent =
                        Intent.createChooser(intent, getString(R.string.chooser_share_post))
                    startActivity(shareIntent)
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                    findNavController().navigate(
                        R.id.action_feedFragment_to_newPostFragment,
                        Bundle().apply {
                            textArg = post.content
                        }
                    )
                }

                override fun onVideo(post: Post) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = post.attachment?.url?.toUri()
                    }
                    val videoIntent =
                        Intent.createChooser(intent, getString(R.string.chooser_open_video))
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {    // врзможно стоит реализовать через getActivity() вместо requireActivity()
                        startActivity(videoIntent)
                    }
                }

                override fun onMoveToSinglePost(post: Post) {
                    findNavController().navigate(
                        R.id.action_feedFragment_to_singlePostFragment,
                        Bundle().apply {
                            textArg = post.id.toString()
                        }
                    )
                }

                override fun onMoveToSinglePhoto(post: Post) {
                    findNavController().navigate(
                        R.id.action_feedFragment_to_singlePhotoFragment,
                        Bundle().apply {
                            textArg = post.id.toString()
                        }
                    )
                }
            }
        )

        binding.list.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.newerBanner.visibility = View.GONE
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            //errorMergeBinding.errorGroup.isVisible = state.error
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) {
                        viewModel.loadPosts()
                    }
                    .show()
            }

            when (state.errorReport?.feedErrorMassage) {
                FeedErrorMassage.LIKE_ERROR -> {
                    Snackbar.make(binding.root, R.string.like_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val postId = state.errorReport.postIdError
                            viewModel.likeById(postId)
                        }
                        .show()
                }

                FeedErrorMassage.DISLIKE_ERROR -> {
                    Snackbar.make(binding.root, R.string.dislike_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val postId = state.errorReport.postIdError
                            viewModel.likeById(postId)
                        }
                        .show()
                }

                FeedErrorMassage.REMOVE_ERROR -> {
                    Snackbar.make(binding.root, R.string.remove_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val postId = state.errorReport.postIdError
                            viewModel.removeById(postId)
                        }
                        .show()
                }

                FeedErrorMassage.SAVE_ERROR -> {
                    Snackbar.make(binding.root, R.string.save_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val post =
                                viewModel.data.value?.posts?.find {
                                    it.id == state.errorReport.postIdError
                                }
                                    ?: throw RuntimeException("Post error")
                            viewModel.saveRefresh(post)
                        }
                        .show()
                }

                FeedErrorMassage.SAVE_REFRESH_ERROR -> {
                    Snackbar.make(binding.root, R.string.save_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val post =
                                viewModel.data.value?.posts?.find {
                                    it.id == state.errorReport.postIdError
                                }
                                    ?: throw RuntimeException("Post error")
                            viewModel.saveRefresh(post)
                        }
                        .show()
                }

                null -> {} //нет смысла уведомлять об успешной операции
            }
        }




        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.empty
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
            println(count)
            if (count == 0) {
                binding.newerBanner.visibility = View.GONE
            } else {
                binding.newerBanner.visibility = View.VISIBLE
                binding.newerCount.text = count.toString()
            }
        }

        binding.newerBanner.setOnClickListener {
            viewModel.updateNewerToOld()
            binding.newerBanner.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.newerBanner.visibility = View.GONE
                    binding.newerBanner.alpha = 1f
                }
                .start()

            // задержка скрола
            binding.list.post {
                binding.list.smoothScrollToPosition(0)
            }
        }

        binding.fab.setOnClickListener {
            if (dependencyContainer.appAuth.authStateFlow.value.id != 0L) {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            } else {
                showLoginDialog()
            }
        }


        return binding.root
    }

    private fun showLoginDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.need_auth)
            .setMessage(R.string.must_login)
            .setPositiveButton(R.string.sign_in) { _, _ ->
                findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(true)
            .show()
    }
}
