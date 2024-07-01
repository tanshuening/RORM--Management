package com.examples.rormmanagement

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityAddMenuItemBinding
import com.examples.rormmanagement.model.Menu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AddMenuItemActivity : AppCompatActivity() {

    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredients: String
    private var foodImage: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var binding: ActivityAddMenuItemBinding

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImage = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMenuItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.addButton.setOnClickListener {
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.foodDescription.text.toString().trim()
            foodIngredients = binding.foodIngredients.text.toString().trim()

            if (!(foodName.isBlank() || foodPrice.isBlank() || foodDescription.isBlank() || foodIngredients.isBlank())) {
                uploadData()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun uploadData() {
        val userId = auth.currentUser?.uid ?: return
        val restaurantsRef = database.getReference("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Assuming the user has only one restaurant
                    val restaurantSnapshot = snapshot.children.first()
                    val restaurantId = restaurantSnapshot.key ?: return

                    val menuRef = database.getReference("restaurants").child(restaurantId).child("menu")
                    val newItemKey = menuRef.push().key

                    if (foodImage != null) {
                        val storageRef = FirebaseStorage.getInstance().reference
                        val imageRef = storageRef.child("menu_images/$newItemKey.jpg")
                        val uploadTask = imageRef.putFile(foodImage!!)

                        uploadTask.addOnSuccessListener {
                            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                val foodPriceInt = foodPrice.toIntOrNull() ?: 0  // Convert foodPrice to int

                                val newItem = Menu(
                                    foodId = newItemKey ?: "",
                                    foodName = foodName,
                                    foodPrice = foodPrice,  // Use the converted int value
                                    foodDescription = foodDescription,
                                    foodIngredients = foodIngredients,
                                    foodImage = downloadUri.toString(),
                                    restaurantId = restaurantId // Set the restaurantId
                                )
                                newItemKey?.let { key ->
                                    menuRef.child(key).setValue(newItem).addOnSuccessListener {
                                        Toast.makeText(
                                            this@AddMenuItemActivity,
                                            "Item added successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }.addOnFailureListener {
                                        Toast.makeText(
                                            this@AddMenuItemActivity,
                                            "Item upload failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@AddMenuItemActivity,
                                    "Failed to get image URL",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(
                                this@AddMenuItemActivity,
                                "Image upload failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@AddMenuItemActivity,
                            "Please select an image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Failed to retrieve restaurant: ${error.message}")
            }
        })
    }
}
