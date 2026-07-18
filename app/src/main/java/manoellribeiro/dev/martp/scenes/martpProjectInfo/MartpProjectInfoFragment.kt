package manoellribeiro.dev.martp.scenes.martpProjectInfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import manoellribeiro.dev.martp.databinding.FragmentMartpProjectInfoBinding

class MartpProjectInfoFragment: Fragment() {

    companion object {
        private const val GITHUB_PROFILE_URL = "https://github.com/manoellribeiro"
        private const val MARTP_GITHUB_REPOSITORY_URL = "https://github.com/manoellribeiro/map-arts"
        private const val LINKEDIN_PROFILE_URL = "https://www.linkedin.com/in/manoellribeiro/"
    }

    private lateinit var binding: FragmentMartpProjectInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMartpProjectInfoBinding.inflate(layoutInflater)
        setupButtonsActions()
        return binding.root
    }

    private fun setupButtonsActions() = with(binding) {
        githubIV.setOnClickListener {
            openUrl(GITHUB_PROFILE_URL)
        }
        linkedinIV.setOnClickListener {
            openUrl(LINKEDIN_PROFILE_URL)
        }
        codeBaseIV.setOnClickListener {
            openUrl(MARTP_GITHUB_REPOSITORY_URL)
        }
    }

    private fun openUrl(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

}