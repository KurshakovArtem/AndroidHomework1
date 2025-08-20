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
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.ErrorViewBinding
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

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

        binding.swiperefresh.setOnRefreshListener {
            viewModel.loadPosts()
            binding.swiperefresh.isRefreshing = false
        }


        val adapter = PostAdapter(
            object : OnInteractionListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
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
                        data = post.videoUrl?.toUri()
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
            }
        )

        binding.list.adapter = adapter
        val errorMergeBinding = ErrorViewBinding.bind(binding.root)

        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.progress.isVisible = state.loading
            binding.emptyText.isVisible = state.empty
            errorMergeBinding.errorGroup.isVisible = state.error
        }

        errorMergeBinding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }
        return binding.root
    }
}
