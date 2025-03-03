package manoellribeiro.dev.martp.scenes.martpProjectInfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import manoellribeiro.dev.martp.databinding.ActivityMartpProjectInfoBinding

class MartpProjectInfoActivity: AppCompatActivity() {

    companion object {
        private const val GITHUB_PROFILE_URL = "https://github.com/manoellribeiro"
        private const val MARTP_GITHUB_REPOSITORY_URL = "https://github.com/manoellribeiro/map-arts"
        private const val LINKEDIN_PROFILE_URL = "https://www.linkedin.com/in/manoellribeiro/"
    }

    private lateinit var binding: ActivityMartpProjectInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMartpProjectInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtonsActions()
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