package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentSinglePhotoBinding
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.supportingFunctions.loadAttachmentImage

@AndroidEntryPoint
class SinglePhotoFragment : Fragment() {

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
        val url = arguments?.textArg ?: {
            findNavController().navigateUp()
        }

        binding.singlePhoto.loadAttachmentImage("http://10.0.2.2:9999/media/${url}")

        return binding.root
    }
}