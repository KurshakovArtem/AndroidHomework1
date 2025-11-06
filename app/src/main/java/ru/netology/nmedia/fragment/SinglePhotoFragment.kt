package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentSinglePhotoBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.supportingFunctions.loadAttachmentImage
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class SinglePhotoFragment : Fragment() {
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentSinglePhotoBinding.inflate(
            inflater,
            container,
            false
        )
        val postId = arguments?.textArg?.toLong() ?: {
            findNavController().navigateUp()
        }

//        val post: Post = (viewModel.data.value?.posts?.find { it.id == postId }
//            ?: findNavController().navigateUp()) as Post
//
//        binding.singlePhoto.loadAttachmentImage("http://10.0.2.2:9999/media/${post.attachment?.url}")

        return binding.root
    }
}