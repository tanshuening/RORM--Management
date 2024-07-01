package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivitySignBinding
import com.examples.rormmanagement.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class SignActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding: ActivitySignBinding by lazy {
        ActivitySignBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Configure Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Build a GoogleSignInClient with the options specified by googleSignInOptions
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Set a click listener for the Google Sign-In button
        binding.googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // Activity result launcher to handle Google Sign-In result
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account: GoogleSignInAccount? = task.result
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkUserExistence(account)
                    } else {
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                        Log.e("SignActivity", "Google sign-in failed", task.exception)
                    }
                }
            } else {
                Toast.makeText(this, "Google sign-in task failed", Toast.LENGTH_SHORT).show()
                Log.e("SignActivity", "Google sign-in task failed", task.exception)
            }
        } else {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if user exists in the database
    private fun checkUserExistence(account: GoogleSignInAccount?) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User already exists
                    Log.d("SignActivity", "User already exists")
                    startActivity(Intent(this@SignActivity, MainActivity::class.java))
                } else {
                    // New user, save user data
                    Log.d("SignActivity", "New user, saving user data")
                    saveUserData(account)
                }
                // Finish activity here to prevent back navigation
                finish()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@SignActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                Log.e("SignActivity", "Database error: ${databaseError.message}")
            }
        })
    }

    // Save new user data to the database
    private fun saveUserData(account: GoogleSignInAccount?) {
        val user = User(
            name = account?.displayName ?: "",
            email = account?.email ?: "",
            password = "" // Assuming password is not required for Google sign-in users
        )
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        database.child("users").child(userId).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SignActivity", "User data saved successfully, starting RegisterRestaurantActivity")
                startActivity(Intent(this@SignActivity, RegisterRestaurantActivity::class.java))
                // Note: Do not finish here, let the RegisterRestaurantActivity handle its logic
            } else {
                Toast.makeText(this@SignActivity, "Failed to save user data", Toast.LENGTH_SHORT).show()
                Log.e("SignActivity", "Failed to save user data", task.exception)
            }
        }
    }
}