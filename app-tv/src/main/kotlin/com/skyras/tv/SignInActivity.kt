package com.skyras.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skyras.tv.core.prefs.MockCredentialAuth
import com.skyras.tv.core.prefs.SignInOutcome
import com.skyras.tv.core.prefs.UserPreferencesRecord
import com.skyras.tv.playlist.PlaylistLoader
import com.skyras.tv.prefs.PreferenceServices
import com.skyras.tv.prefs.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionStore(this)
        if (session.isSignedIn && session.username.isNotBlank()) {
            openLiveTv()
            return
        }

        setContentView(R.layout.activity_sign_in)

        val username = findViewById<EditText>(R.id.input_username)
        val password = findViewById<EditText>(R.id.input_password)
        val m3u = findViewById<EditText>(R.id.input_m3u)
        val error = findViewById<TextView>(R.id.sign_in_error)
        val signIn = findViewById<Button>(R.id.button_sign_in)
        val demo = findViewById<Button>(R.id.button_demo)

        username.requestFocus()

        signIn.setOnClickListener {
            submit(
                username = username.text?.toString().orEmpty(),
                password = password.text?.toString().orEmpty(),
                playlistUrl = m3u.text?.toString().orEmpty(),
                errorView = error,
                signInButton = signIn,
                demoButton = demo
            )
        }
        demo.setOnClickListener {
            completeSession("demo", m3uUrl = null, useDemoCatalog = true)
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }

    private fun submit(
        username: String,
        password: String,
        playlistUrl: String,
        errorView: TextView,
        signInButton: Button,
        demoButton: Button
    ) {
        when (val outcome = MockCredentialAuth.authenticate(username, password, playlistUrl)) {
            is SignInOutcome.Failure -> showError(errorView, outcome.message)
            is SignInOutcome.Success -> {
                if (outcome.useDemoCatalog) {
                    completeSession(outcome.username, outcome.m3uUrl, useDemoCatalog = true)
                    return
                }
                signInButton.isEnabled = false
                demoButton.isEnabled = false
                showError(errorView, getString(R.string.sign_in_loading))
                uiScope.launch {
                    val loaded = runCatching {
                        PlaylistLoader().fetchChannels(outcome.m3uUrl.orEmpty())
                    }
                    if (loaded.isFailure) {
                        signInButton.isEnabled = true
                        demoButton.isEnabled = true
                        showError(
                            errorView,
                            loaded.exceptionOrNull()?.message ?: getString(R.string.sign_in_playlist_failed)
                        )
                        return@launch
                    }
                    completeSession(outcome.username, outcome.m3uUrl, useDemoCatalog = false)
                }
            }
        }
    }

    private fun completeSession(username: String, m3uUrl: String?, useDemoCatalog: Boolean) {
        session.signIn(username, m3uUrl, useDemoCatalog)
        uiScope.launch {
            val repo = PreferenceServices.repository(this@SignInActivity)
            val existing = repo.load(username)
            val record = (existing ?: UserPreferencesRecord(username = username)).copy(
                username = username,
                m3uUrl = m3uUrl ?: existing?.m3uUrl,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            repo.save(record)
            openLiveTv()
        }
    }

    private fun showError(view: TextView, message: String) {
        view.visibility = View.VISIBLE
        view.text = message
    }

    private fun openLiveTv() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
