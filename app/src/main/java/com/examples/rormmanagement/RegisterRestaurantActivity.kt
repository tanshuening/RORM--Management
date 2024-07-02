package com.examples.rormmanagement

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityRegisterRestaurantBinding
import com.examples.rormmanagement.model.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class RegisterRestaurantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterRestaurantBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var restaurantImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        setupLocationSpinner()

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                binding.selectedImage.setImageURI(uri)
                restaurantImage = uri
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

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
                    Toast.makeText(this, "Failed to generate restaurant ID.", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterRestaurant", "Failed to generate restaurant ID")
                    return@setOnClickListener
                }

                if (restaurantImage != null) {
                    uploadImageToStorage(restaurantImage!!) { imageUrl ->
                        val restaurant = Restaurant(
                            restaurantId = restaurantId,
                            location = location,
                            phone = phone,
                            name = name,
                            userId = userId,
                            images = listOf(imageUrl)
                        )

                        restaurantRef.setValue(restaurant)
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
                    }
                } else {
                    Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show()
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

    private fun uploadImageToStorage(imageUri: Uri, onSuccess: (String) -> Unit) {
        val storageRef: StorageReference = storage.reference.child("restaurant_images/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show()
            Log.e("RegisterRestaurant", "Image upload failed", it)
        }
    }
}
