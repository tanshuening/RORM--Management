package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivitySignUpBinding
import com.examples.rormmanagement.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var ownerName: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Configure Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Set a click listener for the Google Sign-In button
        binding.googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        // Set a click listener for the email/password sign-up button
        binding.signUpButton.setOnClickListener {
            ownerName = binding.ownerName.text.toString().trim()
            email = binding.email.text.toString().trim()
            password = binding.password.text.toString().trim()

            if (ownerName.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            } else {
                createAccount(email, password)
            }
        }

        // Set a click listener for the login text view
        binding.login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Handle result from Google Sign-In
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account: GoogleSignInAccount? = task.result
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveUserData(account)
                        val intent = Intent(this, RegisterRestaurantActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                        Log.e("SignUpActivity", "Google sign-in failed", task.exception)
                    }
                }
            } else {
                Toast.makeText(this, "Google sign-in task failed", Toast.LENGTH_SHORT).show()
                Log.e("SignUpActivity", "Google sign-in task failed", task.exception)
            }
        } else {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                saveUserData(null)
                val intent = Intent(this, RegisterRestaurantActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val errorMessage = task.exception?.message ?: "Unknown error"
                Toast.makeText(this, "Failed to create account: $errorMessage", Toast.LENGTH_SHORT).show()
                Log.d("SignUpActivity", "createAccount: Failure", task.exception)
            }
        }
    }

    // Save data into Firebase database
    private fun saveUserData(account: GoogleSignInAccount?) {
        val user = if (account != null) {
            User(
                name = account.displayName ?: "",
                email = account.email ?: "",
                password = "" // No password for Google sign-in users
            )
        } else {
            User(
                name = binding.ownerName.text.toString().trim(),
                email = binding.email.text.toString().trim(),
                password = binding.password.text.toString().trim()
            )
        }

        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid
            database.child("users").child(userId).setValue(user).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignUpActivity", "User data saved successfully")
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e("SignUpActivity", "saveUserData: Failure: $errorMessage", task.exception)
                }
            }
        }
    }
}
