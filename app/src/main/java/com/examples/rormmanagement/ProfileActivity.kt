package com.examples.rormmanagement

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityProfileBinding
import com.examples.rormmanagement.fragment.DashboardFragment
import com.examples.rormmanagement.model.Restaurant
import com.examples.rormmanagement.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        fetchUserData()
        fetchRestaurantData()

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                binding.ownerImage.setImageURI(uri)
                imageUri = uri
            }
        }

        binding.backButton.setOnClickListener {
            switchToFragment(DashboardFragment())
        }

        binding.cardViewImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            updateUserDetails()
            updateRestaurantDetails()
            if (imageUri != null) {
                uploadImage(imageUri!!)
            } else {
                Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.restaurantDetailsLayout.setOnClickListener {
            startActivity(Intent(this, RestaurantProfileActivity::class.java))
        }

        binding.restaurantDetails.setOnClickListener {
            startActivity(Intent(this, RestaurantProfileActivity::class.java))
        }
    }

    private fun uploadImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef: StorageReference = storage.reference.child("profile_images/$userId.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                saveProfileImageUrl(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show()
            Log.e("ProfileActivity", "Image upload failed", it)
        }
    }

    private fun saveProfileImageUrl(url: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("profileImageUrl" to url)
        database.child("users").child(userId).updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null) {
                    binding.ownerName.setText(user.name)
                    binding.ownerEmail.setText(user.email)
                    user.profileImageUrl?.let {
                        Glide.with(this@ProfileActivity).load(it).into(binding.ownerImage)
                    }
                } else {
                    Log.e("ProfileActivity", "User data is null")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                Log.e("ProfileActivity", "Failed to load user data", databaseError.toException())
            }
        })
    }

    private fun fetchRestaurantData() {
        val userId = auth.currentUser?.uid ?: return

        database.child("restaurants").orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val restaurant = dataSnapshot.children.firstOrNull()?.getValue(Restaurant::class.java)
                    if (restaurant != null) {
                        binding.ownerPhone.setText(restaurant.phone)
                    } else {
                        Log.e("ProfileActivity", "Restaurant data is null")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to load restaurant data", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileActivity", "Failed to load restaurant data", databaseError.toException())
                }
            })
    }

    private fun updateUserDetails() {
        val userId = auth.currentUser?.uid ?: return
        val userUpdates = mapOf(
            "name" to binding.ownerName.text.toString(),
            "email" to binding.ownerEmail.text.toString()
        )

        database.child("users").child(userId).updateChildren(userUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "User details updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update user details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRestaurantDetails() {
        val userId = auth.currentUser?.uid ?: return
        val restaurantUpdates = mapOf(
            "phone" to binding.ownerPhone.text.toString()
        )

        database.child("restaurants").orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val restaurantId = dataSnapshot.children.firstOrNull()?.key ?: return
                    database.child("restaurants").child(restaurantId).updateChildren(restaurantUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@ProfileActivity, "Restaurant details updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ProfileActivity, "Failed to update restaurant details", Toast.LENGTH_SHORT).show()
                            }
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to update restaurant details", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileActivity", "Failed to update restaurant details", databaseError.toException())
                }
            })
    }

    private fun switchToFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.dashboardFragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
