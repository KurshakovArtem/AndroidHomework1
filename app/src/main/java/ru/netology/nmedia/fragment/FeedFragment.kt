package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.FeedAdapter
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PagingLoadStateAdapter
import ru.netology.nmedia.adapter.StateOnInteractionListener
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.model.FeedErrorMassage
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

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


        val adapter = FeedAdapter(
            object : OnInteractionListener {
                override fun onLike(post: Post) {
                    if (appAuth.authStateFlow.value.id != 0L) {
                        viewModel.likeById(post)
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
                            textArg = post.attachment?.url
                        }
                    )
                }
            }
        )

        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PagingLoadStateAdapter(
                object : StateOnInteractionListener {
                    override fun onRetry() {
                        adapter.retry()
                    }
                }),
            footer = PagingLoadStateAdapter(
                object : StateOnInteractionListener {
                    override fun onRetry() {
                        adapter.retry()
                    }
                })
        )

        val layoutManager = binding.list.layoutManager as LinearLayoutManager

        binding.list.addOnScrollListener( object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int){
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                val firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition()

                val shouldShowHeader = firstCompletelyVisible == 0 || firstVisiblePosition == 0
                binding.todayText.isVisible = shouldShowHeader
            }
        })


        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { state ->
                binding.swiperefresh.isRefreshing =
                    state.refresh is LoadState.Loading
//                            state.prepend is LoadState.Loading ||
//                            state.append is LoadState.Loading
            }
        }

        authViewModel.dataState.observe(viewLifecycleOwner) { dataState ->
            if (dataState.needRefresh) {
                adapter.refresh()
                authViewModel.clearState()
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            //viewModel.refresh()
            adapter.refresh()
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) {
                        //viewModel.loadPosts()
                        adapter.refresh()
                    }
                    .show()
            }

            when (state.errorReport?.feedErrorMassage) {
                FeedErrorMassage.LIKE_ERROR -> {
                    Snackbar.make(binding.root, R.string.like_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val postId = state.errorReport.postIdError
                            val post = adapter.snapshot().items.find { it.id == postId }
                                ?: return@setAction
                            if (post is Post) {
                                viewModel.likeById(post)
                            } else {
                                return@setAction
                            }
                        }
                        .show()
                }

                FeedErrorMassage.DISLIKE_ERROR -> {
                    Snackbar.make(binding.root, R.string.dislike_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val postId = state.errorReport.postIdError
                            val post = adapter.snapshot().items.find { it.id == postId }
                                ?: return@setAction
                            if (post is Post) {
                                viewModel.likeById(post)
                            } else {
                                return@setAction
                            }
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
                                adapter.snapshot().items.find {
                                    it.id == state.errorReport.postIdError
                                }
                                    ?: throw RuntimeException("Post error")
                            if (post is Post) {
                                viewModel.saveRefresh(post)
                            } else {
                                return@setAction
                            }
                        }
                        .show()
                }

                FeedErrorMassage.SAVE_REFRESH_ERROR -> {
                    Snackbar.make(binding.root, R.string.save_error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) {
                            val post =
                                adapter.snapshot().items.find {
                                    it.id == state.errorReport.postIdError
                                }
                                    ?: throw RuntimeException("Post error")
                            if (post is Post) {
                                viewModel.saveRefresh(post)
                            } else {
                                return@setAction
                            }
                        }
                        .show()
                }

                null -> {} //нет смысла уведомлять об успешной операции
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest(adapter::submitData)
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
            if (appAuth.authStateFlow.value.id != 0L) {
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
