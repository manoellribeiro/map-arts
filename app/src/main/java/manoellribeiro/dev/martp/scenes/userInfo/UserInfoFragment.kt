package manoellribeiro.dev.martp.scenes.userInfo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import manoellribeiro.dev.martp.core.components.MartpTextInput
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.databinding.FragmentUserInfoBinding
import manoellribeiro.dev.martp.scenes.main.MainViewModel


@AndroidEntryPoint
class UserInfoFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentUserInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentUserInfoBinding.inflate(layoutInflater)
        setupObservables()
        viewModel.tryToGetUserInfo()
        return binding.root
    }

    private fun setupObservables() {
        viewModel.userInfoState.observe(viewLifecycleOwner) { state ->
            when(state) {
                is UserInfoUiState.UserFound -> setupUserFoundState(state.user)
                UserInfoUiState.Loading -> setupLoadingState()
            }
        }
    }

    private fun setupUserFoundState(user: UserInfoEntity) = with(binding) {
        usernameMTI.visible()
        usernameDescriptionTV.visible()
        emailDescriptionTV.visible()
        emailMTI.visible()
        usernameMTI.currentText = user.username.orEmpty()
        emailMTI.currentText = user.email.orEmpty()
        setupChangedTextListenerToSaveUserData(
            textInput = binding.usernameMTI,
            actionToExecute = {
                viewModel.setUserName(usernameMTI.currentText, user.id)
            }
        )
        setupChangedTextListenerToSaveUserData(
            textInput = binding.emailMTI,
            actionToExecute = {
                viewModel.setUserEmail(emailMTI.currentText, user.id)
            }
        )
        loadingIndicatorPB.gone()
    }

    private fun setupLoadingState() = with(binding)  {
        usernameMTI.gone()
        usernameDescriptionTV.gone()
        emailDescriptionTV.gone()
        emailMTI.gone()
        loadingIndicatorPB.visible()
    }

    private fun setupChangedTextListenerToSaveUserData(
        textInput: MartpTextInput,
        actionToExecute: () -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        textInput.setOnChangedTextListener(
            textWatcher = object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed(userStoppedTyping, 2000)
                }
                var userStoppedTyping: Runnable = Runnable {
                    actionToExecute.invoke()
                }
                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int
                ) {}

                override fun onTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int
                ) {}
            }
        )
    }
}