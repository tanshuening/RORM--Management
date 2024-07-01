package com.examples.rormmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.examples.rormmanagement.databinding.ActivityProfileBinding
import com.examples.rormmanagement.fragment.DashboardFragment
import com.examples.rormmanagement.model.Restaurant
import com.examples.rormmanagement.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupLocationSpinner()
        fetchUserData()
        fetchRestaurantData()

        binding.backButton.setOnClickListener {
            switchToFragment(DashboardFragment())
        }

        binding.restaurantDetailsLayout.setOnClickListener {
            val intent = Intent(this, RestaurantProfileActivity::class.java)
            startActivity(intent)
        }

        binding.saveButton.setOnClickListener {
            updateUserDetails()
            updateRestaurantDetails()
        }
    }

    private fun setupLocationSpinner() {
        val locationOptions = resources.getStringArray(R.array.malaysia_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.listOfLocation.setAdapter(adapter)
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null) {
                    binding.ownerName.text = user.name
                    binding.ownerEmail.setText(user.email)
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
                        val locationArray = resources.getStringArray(R.array.malaysia_states)
                        val locationPosition = locationArray.indexOf(restaurant.location)

                        // Ensure locationPosition is within bounds
                        if (locationPosition >= 0 && locationPosition < locationArray.size) {
                            binding.listOfLocation.setText(locationArray[locationPosition], false)
                        } else {
                            Log.e("ProfileActivity", "Invalid location position: $locationPosition")
                        }

                        binding.restaurantName.setText(restaurant.name)
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
            "ownerName" to binding.ownerName.text.toString(),
            "email" to binding.ownerEmail.text.toString()
        )

        database.child("users").child(userId).updateChildren(userUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "User details updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update user details", Toast.LENGTH_SHORT).show()
                task.exception?.let { Log.e("ProfileActivity", "Failed to update user details", it) }
            }
        }
    }

    private fun updateRestaurantDetails() {
        val userId = auth.currentUser?.uid ?: return
        database.child("restaurants").orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val restaurantId = dataSnapshot.children.firstOrNull()?.key ?: return
                    val restaurantUpdates = mapOf(
                        "location" to binding.listOfLocation.text.toString(),
                        "name" to binding.restaurantName.text.toString(),
                        "phone" to binding.ownerPhone.text.toString()
                    )

                    database.child("restaurants").child(restaurantId).updateChildren(restaurantUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@ProfileActivity, "Restaurant details updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProfileActivity, "Failed to update restaurant details", Toast.LENGTH_SHORT).show()
                            task.exception?.let { Log.e("ProfileActivity", "Failed to update restaurant details", it) }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to load restaurant data", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileActivity", "Failed to load restaurant data", databaseError.toException())
                }
            })
    }

    private fun switchToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardFragment, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(null)
            .commit()
    }
}
