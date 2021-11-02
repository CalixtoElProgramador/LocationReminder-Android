package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

private const val TAG = "AuthenticationActivity"

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var registerForActivityResult: ActivityResultLauncher<Intent>
    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityAuthenticationBinding>(this, R.layout.activity_authentication)

        registerForSignInResult()

        binding.loginButton.setOnClickListener { launchSignInFlow() }

        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            when(authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED ->
                {
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                    Log.i(
                        TAG,
                        "User ${FirebaseAuth.getInstance().currentUser?.displayName} signed in,Should go to RemindersActivity")
                }
                else -> Log.i(TAG, "Error or not authenticated")
            }

        })
    }

    /**
     * Registers a launcher used to start the process of executing an ActivityResultContract
     * so that we can listen to the result of the sign - in process
     * This must be done in onCreate() or on Attach, ie before the fragment is created
     */
    private fun registerForSignInResult() {
        registerForActivityResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
            val response = IdpResponse.fromResultIntent(result.data)
            // Listen to the result of the sign - in process
            if(result.resultCode == Activity.RESULT_OK){
                Log.i(TAG, "User ${FirebaseAuth.getInstance().currentUser?.displayName} has signed in")
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun launchSignInFlow() {
        // Give users the option to either sign in / register with their email or Google account
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())

        registerForActivityResult.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build())

    }
}
