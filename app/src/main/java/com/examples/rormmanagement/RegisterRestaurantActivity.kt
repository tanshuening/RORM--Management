package com.examples.rormmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityRegisterRestaurantBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterRestaurantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterRestaurantBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupLocationSpinner()

        binding.nextButton.setOnClickListener {
            val location = binding.listOfLocation.text.toString()
            val phone = binding.phoneNumber.text.toString()
            val name = binding.restaurantName.text.toString()

            if (location.isNotEmpty() && phone.isNotEmpty() && name.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: run {
                    Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterRestaurant", "User not authenticated")
                    return@setOnClickListener
                }

                // Generate a unique restaurant ID
                val restaurantRef = database.reference.child("restaurants").push()
                val restaurantId = restaurantRef.key ?: run {
                    Toast.makeText(this, "Failed to generate restaurant ID.", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("RegisterRestaurant", "Failed to generate restaurant ID")
                    return@setOnClickListener
                }

                val restaurantUpdate = mapOf(
                    "location" to location,
                    "phone" to phone,
                    "name" to name,
                    "userId" to userId,
                    "restaurantId" to restaurantId
                )
                restaurantRef.setValue(restaurantUpdate)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, RegisterRestaurantProfileActivity::class.java).apply {
                                putExtra("restaurantId", restaurantId)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                            Log.e("RegisterRestaurant", "Failed to save data", task.exception)
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLocationSpinner() {
        val locationOptions = resources.getStringArray(R.array.malaysia_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locationOptions)
        binding.listOfLocation.setAdapter(adapter)
    }
}
