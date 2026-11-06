package manoellribeiro.dev.martp.scenes.userInfoBottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.databinding.FragmentSetUserInfoBottomSheetBinding

class SetUserInfoBottomSheetFragment(
    private val userInfo: UserInfoEntity?,
    private val onSaveWithMyName: (UserInfoEntity) -> Unit,
    private val onSaveAnonymously: () -> Unit
): BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSetUserInfoBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetUserInfoBottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() = with(binding) {
        if(userInfo?.username.isNullOrEmpty()) saveAnonymouslyMB.visible()
        usernameMTI.currentText = userInfo?.username.orEmpty()
        emailMTI.currentText = userInfo?.email.orEmpty()
        saveAnonymouslyMB.setOnClickListener {
            onSaveAnonymously.invoke()
            this@SetUserInfoBottomSheetFragment.dismiss()
        }
        saveWithNameMB.setOnClickListener {
            onSaveWithMyName(
                UserInfoEntity(
                    id = userInfo?.id.orEmpty(),
                    username = usernameMTI.currentText,
                    email = emailMTI.currentText
                )
            )
            this@SetUserInfoBottomSheetFragment.dismiss()
        }
    }

}
