package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityEditMenuItemInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class EditMenuItemInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditMenuItemInfoBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItemRef: DatabaseReference
    private var menuItemId: String? = null
    private var restaurantId: String? = null
    private var foodImage: Uri? = null
    private lateinit var initialImageUri: String

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImage = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditMenuItemInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        menuItemId = intent.getStringExtra("menuItemId")
        restaurantId = intent.getStringExtra("restaurantId")

        if (menuItemId != null && restaurantId != null) {
            menuItemRef = database.getReference("restaurants").child(restaurantId!!).child("menu").child(menuItemId!!)
        } else {
            Toast.makeText(this, "MenuItemId or restaurantId is null", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val menuItemName = intent.getStringExtra("foodName")
        val menuItemPrice = intent.getStringExtra("foodPrice")
        val menuItemDescription = intent.getStringExtra("foodDescription")
        val menuItemIngredients = intent.getStringExtra("foodIngredients")
        initialImageUri = intent.getStringExtra("foodImage") ?: ""

        binding.foodName.setText(menuItemName)
        binding.foodPrice.setText(menuItemPrice)
        binding.foodDescription.setText(menuItemDescription)
        binding.foodIngredients.setText(menuItemIngredients)
        Glide.with(this).load(Uri.parse(initialImageUri)).into(binding.selectedImage)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val updatedData = mutableMapOf(
            "foodName" to binding.foodName.text.toString(),
            "foodPrice" to binding.foodPrice.text.toString(),
            "foodDescription" to binding.foodDescription.text.toString(),
            "foodIngredients" to binding.foodIngredients.text.toString(),
            "foodImage" to initialImageUri
        )

        if (foodImage != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/$menuItemId.jpg")
            val uploadTask = imageRef.putFile(foodImage!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updatedData["foodImage"] = downloadUri.toString()
                    updateDatabase(updatedData)
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateDatabase(updatedData)
        }
    }

    private fun updateDatabase(updatedData: Map<String, Any>) {
        menuItemRef.updateChildren(updatedData)
            .addOnSuccessListener {
                val updatedIntent = Intent().apply {
                    putExtra("menuItemId", menuItemId)
                    putExtra("foodName", updatedData["foodName"].toString())
                    putExtra("foodPrice", updatedData["foodPrice"].toString())
                    putExtra("foodDescription", updatedData["foodDescription"].toString())
                    putExtra("foodIngredients", updatedData["foodIngredients"].toString())
                    putExtra("foodImage", updatedData["foodImage"].toString())
                }
                setResult(Activity.RESULT_OK, updatedIntent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
