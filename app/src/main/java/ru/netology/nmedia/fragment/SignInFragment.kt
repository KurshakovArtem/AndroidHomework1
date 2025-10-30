package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.ViewModelFactory

class SignInFragment : Fragment() {

    private val dependencyContainer = DependencyContainer.getInstance()
    private val viewModel: AuthViewModel by viewModels(
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

        viewModel.clearState()

        val binding = FragmentSignInBinding.inflate(
            inflater,
            container,
            false
        )

        binding.signInButton.setOnClickListener {
            val username = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            viewModel.signIn(username, password)
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressAuth.isVisible = state.loading
            binding.signInButton.isEnabled = !state.loading
            if (state.error) {
                Toast.makeText(requireContext(), R.string.error_auth, Toast.LENGTH_SHORT).show()
                viewModel.clearState()
            }
            if (state.success) {
                findNavController().navigateUp()
            }
        }


        return binding.root
    }
}