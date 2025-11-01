package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.AuthViewModel

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel.clearState()

        val binding = FragmentSignUpBinding.inflate(
            inflater,
            container,
            false
        )

        val photoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(requireContext(), R.string.image_error, Toast.LENGTH_SHORT)
                        .show()
                    return@registerForActivityResult
                }
                val uri = result.data?.data ?: return@registerForActivityResult
                viewModel.updatePhoto(uri, uri.toFile())
            }

        binding.registrationButton.setOnClickListener {
            val nickname = binding.nicknameEditText.text.toString()
            val login = binding.signUpLoginEditText.text.toString()
            val password = binding.signUpPasswordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            viewModel.signUp(nickname, login, password, confirmPassword)
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressSignUp.isVisible = state.loading
            binding.registrationButton.isEnabled = !state.loading

            if (state.error) {
                Toast.makeText(requireContext(), R.string.error_registration, Toast.LENGTH_SHORT)
                    .show()
                viewModel.clearState()
            }

            if (state.success) {
                findNavController().navigateUp()
            }

            viewModel.photo.observe(viewLifecycleOwner) { photo ->
                if (photo == null) {
                    binding.avatarImage.setImageResource(R.drawable.ic_person_24)
                    return@observe
                }
                binding.avatarImage.setImageURI(photo.uri)
            }

            binding.removeAvatarButton.setOnClickListener {
                viewModel.removePhoto()
            }


            binding.takePhotoSignUp.setOnClickListener {
                ImagePicker.with(this)
                    .cameraOnly()
                    .crop()
                    .createIntent(photoLauncher::launch)
            }

            binding.pickPhotoSignUp.setOnClickListener {
                ImagePicker.with(this)
                    .galleryOnly()
                    .crop()
                    .createIntent(photoLauncher::launch)
            }

        }
        return binding.root
    }
}