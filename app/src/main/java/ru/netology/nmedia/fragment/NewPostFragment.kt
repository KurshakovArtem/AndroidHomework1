package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.supportingFunctions.AndroidUtils
import ru.netology.nmedia.supportingFunctions.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel


class NewPostFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        arguments?.textArg?.let(binding.edit::setText) ?: binding.edit.setText(viewModel.getDraft())

        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            //viewModel.cancelEdit()
            viewModel.createDraft(binding.edit.text.toString())
            findNavController().navigateUp()
        }
        callback.isEnabled = true   // очистить пост при нажатии системной кнопки назад


        binding.edit.requestFocus()
        binding.ok.setOnClickListener {
            if (binding.edit.text.isNotBlank()) {
                val content = binding.edit.text.toString()
                viewModel.save(content)
                AndroidUtils.hideKeyboard(requireView())
            }
            findNavController().navigateUp()
        }
        return binding.root
    }

    companion object {
        var Bundle.textArg: String? by StringArg
    }
}